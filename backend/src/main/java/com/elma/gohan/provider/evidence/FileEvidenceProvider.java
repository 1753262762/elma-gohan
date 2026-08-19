package com.elma.gohan.provider.evidence;

import com.elma.gohan.config.EvidenceProperties;
import com.elma.gohan.domain.restaurant.Restaurant;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.InputStream;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

/** 文件 DTO 只有经本类映射成 RestaurantEvidence 后才能进入领域层。 */
@Component
@ConditionalOnProperty(prefix = "elma.evidence", name = "provider",
        havingValue = "file", matchIfMissing = true)
public class FileEvidenceProvider implements EvidenceProvider {

    private static final Logger log = LoggerFactory.getLogger(FileEvidenceProvider.class);

    private final EvidenceProperties properties;
    private final ObjectMapper objectMapper;
    private final ResourceLoader resourceLoader;
    private volatile Map<String, RestaurantEvidence> index;
    private volatile boolean loadFailed;

    public FileEvidenceProvider(EvidenceProperties properties, ObjectMapper objectMapper,
                                ResourceLoader resourceLoader) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.resourceLoader = resourceLoader;
    }

    @Override
    public RestaurantEvidence getEvidence(Restaurant restaurant) {
        ensureLoaded();
        if (loadFailed) {
            return RestaurantEvidence.unavailable("FILE");
        }
        return index.getOrDefault(key(restaurant.source(), restaurant.sourcePoiId()),
                RestaurantEvidence.noData("FILE"));
    }

    private void ensureLoaded() {
        if (index != null || loadFailed) {
            return;
        }
        synchronized (this) {
            if (index != null || loadFailed) {
                return;
            }
            try (InputStream input = resourceLoader.getResource(properties.getLocation())
                    .getInputStream()) {
                FileDocument document = objectMapper.readValue(input, FileDocument.class);
                Map<String, RestaurantEvidence> loaded = new HashMap<>();
                if (document.restaurants() != null) {
                    for (FileRestaurant raw : document.restaurants()) {
                        if (raw.source() == null || raw.sourcePoiId() == null) {
                            continue;
                        }
                        List<ReviewEvidence> reviews = raw.reviews() == null ? List.of()
                                : raw.reviews().stream()
                                .limit(Math.max(0, properties.getMaxReviewsPerRestaurant()))
                                .map(review -> new ReviewEvidence(review.reviewId(), review.text(),
                                        validRating(review.rating()), review.createdAt()))
                                .toList();
                        loaded.put(key(raw.source(), raw.sourcePoiId()),
                                RestaurantEvidence.available(
                                        raw.evidenceSource() == null ? "FILE" : raw.evidenceSource(),
                                        reviews, raw.fetchedAt()));
                    }
                }
                index = Map.copyOf(loaded);
            } catch (Exception exception) {
                loadFailed = true;
                log.warn("Evidence 文件不可用，已降级为空证据: {}", properties.getLocation(),
                        exception);
            }
        }
    }

    private Double validRating(Double rating) {
        return rating != null && rating >= 1.0 && rating <= 5.0 ? rating : null;
    }

    private String key(String source, String sourcePoiId) {
        return (source == null ? "" : source.toUpperCase(Locale.ROOT)) + ":" + sourcePoiId;
    }

    private record FileDocument(List<FileRestaurant> restaurants) { }
    private record FileRestaurant(String source, String sourcePoiId, String evidenceSource,
                                  Instant fetchedAt, List<FileReview> reviews) { }
    private record FileReview(String reviewId, String text, Double rating, Instant createdAt) { }
}
