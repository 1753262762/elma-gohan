package com.elma.gohan.provider.deep;

import com.elma.gohan.config.DeepEvidenceProperties;
import com.elma.gohan.domain.restaurant.Restaurant;
import com.elma.gohan.provider.evidence.EvidenceStatus;
import com.fasterxml.jackson.databind.JsonNode;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

/** 只消费 Brave Search 正式 API 的索引结果，不访问结果网页。 */
@Component
public class BraveWebEvidenceProvider implements DeepEvidenceProvider {

    private static final Logger log = LoggerFactory.getLogger(BraveWebEvidenceProvider.class);

    private final DeepEvidenceProperties properties;
    private final WebEvidenceMatcher matcher;
    private final RestClient restClient;

    public BraveWebEvidenceProvider(DeepEvidenceProperties properties,
                                    WebEvidenceMatcher matcher) {
        this.properties = properties;
        this.matcher = matcher;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofMillis(properties.getConnectTimeoutMs()));
        factory.setReadTimeout(Duration.ofMillis(properties.getReadTimeoutMs()));
        restClient = RestClient.builder().baseUrl(properties.getBaseUrl())
                .requestFactory(factory).build();
        if (properties.isEnabled() && properties.getApiKey().isBlank()) {
            log.warn("BRAVE_SEARCH_API_KEY 未配置，按需深挖将降级为 UNAVAILABLE");
        }
    }

    @Override
    public DeepEvidenceBatch fetch(DeepEvidenceSource source, Restaurant restaurant) {
        Instant now = Instant.now();
        if (!properties.isEnabled() || properties.getApiKey().isBlank()) {
            return DeepEvidenceBatch.unavailable(source, now);
        }
        long started = System.nanoTime();
        try {
            SearchAttempt recent = search(source, restaurant, now, true, "RECENT_31_DAYS");
            List<WebEvidenceItem> items = recent.items();
            if (items.isEmpty()) {
                try {
                    SearchAttempt allTime = search(source, restaurant, now, false, "ALL_TIME_FALLBACK");
                    items = allTime.items();
                } catch (RestClientException fallbackException) {
                    log.warn("Brave 深挖回退失败 source={} durationMs={} errorType={}", source,
                            elapsedMillis(started), fallbackException.getClass().getSimpleName());
                }
            }
            EvidenceStatus status = items.isEmpty()
                    ? EvidenceStatus.NO_DATA : EvidenceStatus.AVAILABLE;
            log.info("Brave 深挖完成 source={} status={} matchedResultCount={} durationMs={}",
                    source, status, items.size(), elapsedMillis(started));
            return new DeepEvidenceBatch(source, status, items, now);
        } catch (RestClientResponseException exception) {
            log.warn("Brave 深挖失败 source={} httpStatus={} validationField={} durationMs={}",
                    source, exception.getStatusCode().value(),
                    validationField(exception.getResponseBodyAsString()), elapsedMillis(started));
            return DeepEvidenceBatch.unavailable(source, now);
        } catch (RestClientException exception) {
            log.warn("Brave 深挖失败 source={} durationMs={} errorType={}", source,
                    elapsedMillis(started), exception.getClass().getSimpleName());
            return DeepEvidenceBatch.unavailable(source, now);
        }
    }

    private SearchAttempt search(DeepEvidenceSource source, Restaurant restaurant,
                                 Instant fetchedAt, boolean recentOnly, String phase) {
        long started = System.nanoTime();
        JsonNode body = restClient.get().uri(uriBuilder -> {
                    var builder = uriBuilder.path("/res/v1/web/search")
                            .queryParam("q", buildQuery(source, restaurant))
                            .queryParam("count", Math.min(20, Math.max(1, properties.getResultCount())))
                            .queryParam("offset", 0)
                            .queryParam("country", "CN")
                            .queryParam("search_lang", "zh-hans")
                            .queryParam("ui_lang", "zh-CN")
                            .queryParam("safesearch", "moderate")
                            .queryParam("spellcheck", false)
                            .queryParam("extra_snippets", false);
                    if (recentOnly) builder.queryParam("freshness", "pm");
                    return builder.build();
                })
                .accept(MediaType.APPLICATION_JSON)
                .header("X-Subscription-Token", properties.getApiKey())
                .retrieve().body(JsonNode.class);
        JsonNode results = body == null ? null : body.path("web").path("results");
        int rawResultCount = results != null && results.isArray() ? results.size() : 0;
        List<WebEvidenceItem> items = map(source, restaurant, results, fetchedAt);
        log.info("Brave 深挖检索 source={} phase={} rawResultCount={} matchedResultCount={} durationMs={}",
                source, phase, rawResultCount, items.size(), elapsedMillis(started));
        return new SearchAttempt(items);
    }

    private String validationField(String responseBody) {
        if (responseBody == null || responseBody.isBlank()) return "none";
        try {
            JsonNode location = new com.fasterxml.jackson.databind.ObjectMapper()
                    .readTree(responseBody).path("error").path("meta")
                    .path("errors").path(0).path("loc");
            return location.isArray() && !location.isEmpty()
                    ? location.path(location.size() - 1).asText("unknown") : "unknown";
        } catch (Exception ignored) {
            return "unparseable";
        }
    }

    private String buildQuery(DeepEvidenceSource source, Restaurant restaurant) {
        String safeName = restaurant.name().replace('"', ' ').trim();
        String location = matcher.searchLocationKeyword(restaurant.name(), restaurant.address());
        return (source.siteQuery() + " \"" + safeName + "\" " + location).trim();
    }

    private record SearchAttempt(List<WebEvidenceItem> items) { }

    private List<WebEvidenceItem> map(DeepEvidenceSource source, Restaurant restaurant,
                                      JsonNode results, Instant fetchedAt) {
        if (results == null || !results.isArray()) return List.of();
        Map<String, WebEvidenceItem> deduplicated = new LinkedHashMap<>();
        for (JsonNode result : results) {
            String title = truncate(text(result, "title"), 200);
            String rawUrl = text(result, "url");
            String snippet = truncate(text(result, "description"), 500);
            String url = canonicalUrl(source, rawUrl);
            if (title == null || url == null) continue;
            double matchConfidence = matcher.match(restaurant, title, snippet);
            if (matchConfidence <= 0.0) continue;
            WebEvidenceItem item = new WebEvidenceItem(source, title, url, snippet,
                    parsePublishedAt(result), fetchedAt, matchConfidence, List.of());
            deduplicated.putIfAbsent(url, item);
        }
        return List.copyOf(deduplicated.values());
    }

    private String canonicalUrl(DeepEvidenceSource source, String value) {
        if (value == null || value.isBlank()) return null;
        try {
            URI uri = URI.create(value.trim());
            if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.getHost() == null) return null;
            String host = uri.getHost().toLowerCase(Locale.ROOT);
            if (!allowedHost(source, host)) return null;
            String path = uri.getRawPath() == null || uri.getRawPath().isBlank()
                    ? "/" : uri.getRawPath();
            String canonical = URI.create("https://" + host + path).toASCIIString();
            return canonical.length() <= 1000 ? canonical : null;
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    private boolean allowedHost(DeepEvidenceSource source, String host) {
        String domain = switch (source) {
            case BILIBILI -> "bilibili.com";
            case XIAOHONGSHU -> "xiaohongshu.com";
            case DIANPING -> "dianping.com";
        };
        return host.equals(domain) || host.endsWith("." + domain);
    }

    private Instant parsePublishedAt(JsonNode result) {
        for (String field : List.of("page_age", "published_at", "date")) {
            String value = text(result, field);
            if (value == null) continue;
            try {
                return Instant.parse(value);
            } catch (DateTimeParseException ignored) {
                try {
                    return OffsetDateTime.parse(value).toInstant();
                } catch (DateTimeParseException ignoredOffset) {
                    try {
                        return LocalDateTime.parse(value).toInstant(ZoneOffset.UTC);
                    } catch (DateTimeParseException ignoredDateTime) {
                        try {
                            return LocalDate.parse(value).atStartOfDay().toInstant(ZoneOffset.UTC);
                        } catch (DateTimeParseException ignoredDate) {
                            // 相对时间或非标准日期不猜测。
                        }
                    }
                }
            }
        }
        return null;
    }

    private static String text(JsonNode node, String field) {
        if (node == null || node.isMissingNode() || node.isNull()) return null;
        String value = node.path(field).asText("").trim();
        return value.isBlank() ? null : value;
    }

    private static String truncate(String value, int max) {
        if (value == null) return null;
        return value.length() <= max ? value : value.substring(0, max);
    }

    private static long elapsedMillis(long started) {
        return (System.nanoTime() - started) / 1_000_000;
    }
}
