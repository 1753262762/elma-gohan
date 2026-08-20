package com.elma.gohan.provider.evidence;

import com.elma.gohan.config.BaiduProperties;
import com.elma.gohan.domain.restaurant.Location;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/** 百度 Place V3/V2 正规 API；完整 URL 含 AK，禁止写入日志。 */
@Component
public class BaiduPlaceApiProvider implements PlatformEvidenceProvider {

    private static final Logger log = LoggerFactory.getLogger(BaiduPlaceApiProvider.class);
    private static final String SOURCE = "BAIDU";

    private final BaiduProperties properties;
    private final RestClient restClient;

    public BaiduPlaceApiProvider(BaiduProperties properties) {
        this.properties = properties;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofMillis(properties.getConnectTimeoutMs()));
        factory.setReadTimeout(Duration.ofMillis(properties.getReadTimeoutMs()));
        this.restClient = RestClient.builder().baseUrl(properties.getBaseUrl())
                .requestFactory(factory).build();
        if (properties.isEnabled() && properties.getAk().isBlank()) {
            log.warn("BAIDU_MAP_AK 未配置，百度 Evidence 将降级为 UNAVAILABLE");
        }
    }

    @Override
    public PlatformSearchResult searchV3(Location center, int radiusMeters) {
        return search("V3", "/place/v3/around", center, radiusMeters);
    }

    @Override
    public PlatformSearchResult searchV2(Location center, int radiusMeters) {
        return search("V2", "/place/v2/search", center, radiusMeters);
    }

    private PlatformSearchResult search(String apiVersion, String path, Location center,
                                        int radiusMeters) {
        if (!properties.isEnabled() || properties.getAk().isBlank()) {
            return PlatformSearchResult.unavailable();
        }
        long started = System.nanoTime();
        try {
            JsonNode body = restClient.get().uri(uriBuilder -> uriBuilder.path(path)
                    .queryParam("query", properties.getQuery())
                    .queryParam("location", center.latitude() + "," + center.longitude())
                    .queryParam("radius", radiusMeters)
                    .queryParam("radius_limit", true)
                    .queryParam("scope", 2)
                    .queryParam("coord_type", 2)
                    .queryParam("ret_coordtype", "gcj02ll")
                    .queryParam("page_size", Math.min(20, Math.max(10, properties.getPageSize())))
                    .queryParam("page_num", 0)
                    .queryParam("output", "json")
                    .queryParam("ak", properties.getAk())
                    .build()).retrieve().body(JsonNode.class);
            if (body == null || body.path("status").asInt(-1) != 0) {
                int status = body == null ? -1 : body.path("status").asInt(-1);
                log.warn("百度 Place {} 返回失败状态 status={} durationMs={}", apiVersion,
                        status, elapsedMillis(started));
                return PlatformSearchResult.unavailable();
            }
            List<PlatformEvidence> evidence = mapResults(body.path("results"), Instant.now());
            log.info("百度 Place {} 完成 resultCount={} durationMs={}", apiVersion,
                    evidence.size(), elapsedMillis(started));
            return new PlatformSearchResult(evidence.isEmpty()
                    ? EvidenceStatus.NO_DATA : EvidenceStatus.AVAILABLE, evidence);
        } catch (RestClientException exception) {
            log.warn("百度 Place {} 请求失败 durationMs={} errorType={}", apiVersion,
                    elapsedMillis(started), exception.getClass().getSimpleName());
            return PlatformSearchResult.unavailable();
        }
    }

    private List<PlatformEvidence> mapResults(JsonNode results, Instant observedAt) {
        if (!results.isArray()) return List.of();
        List<PlatformEvidence> mapped = new ArrayList<>();
        for (JsonNode result : results) {
            String uid = text(result, "uid");
            String name = text(result, "name");
            if (uid == null || name == null) continue;
            JsonNode location = result.path("location");
            JsonNode detail = result.path("detail_info");
            mapped.add(new PlatformEvidence(SOURCE, uid, EvidenceStatus.AVAILABLE, observedAt,
                    name, text(result, "address"), decimal(location, "lat"),
                    decimal(location, "lng"), decimal(detail, "overall_rating"),
                    decimal(detail, "taste_rating"), decimal(detail, "service_rating"),
                    decimal(detail, "environment_rating"), integer(detail, "comment_num"),
                    integer(detail, "price"), text(detail, "shop_hours"),
                    text(detail, "brand"), text(result, "telephone")));
        }
        return List.copyOf(mapped);
    }

    private static String text(JsonNode node, String field) {
        if (node == null || node.isMissingNode() || node.isNull()) return null;
        String value = node.path(field).asText("").trim();
        return value.isBlank() ? null : value;
    }

    private static Double decimal(JsonNode node, String field) {
        String value = text(node, field);
        if (value == null) return null;
        try {
            double parsed = Double.parseDouble(value.replaceAll("[^0-9.-]", ""));
            return Double.isFinite(parsed) ? parsed : null;
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private static Integer integer(JsonNode node, String field) {
        Double value = decimal(node, field);
        return value == null ? null : (int) Math.round(value);
    }

    private static long elapsedMillis(long started) {
        return (System.nanoTime() - started) / 1_000_000;
    }
}
