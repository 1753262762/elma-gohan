package com.elma.gohan.domain.recommendation;

import com.elma.gohan.config.TasteProperties;
import com.elma.gohan.domain.restaurant.Restaurant;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/** 可解释的轻量画像：品类、价格档和距离档三个独立累计权重。 */
public record TasteProfile(
        int schemaVersion,
        Map<String, Double> categoryWeights,
        Map<String, Double> priceBandWeights,
        Map<String, Double> distanceBandWeights,
        int feedbackCount,
        LocalDateTime updatedAt
) {
    public static final int SCHEMA_VERSION = 2;

    public TasteProfile {
        categoryWeights = immutable(categoryWeights);
        priceBandWeights = immutable(priceBandWeights);
        distanceBandWeights = immutable(distanceBandWeights);
    }

    public static TasteProfile empty() {
        return new TasteProfile(SCHEMA_VERSION, Map.of(), Map.of(), Map.of(), 0, null);
    }

    public TasteProfile apply(Restaurant restaurant, int distanceMeters, String result,
                              LocalDateTime occurredAt, TasteProperties properties) {
        Map<String, Double> categories = decayed(categoryWeights, properties.getDecay());
        Map<String, Double> prices = decayed(priceBandWeights, properties.getDecay());
        Map<String, Double> distances = decayed(distanceBandWeights, properties.getDecay());
        double delta = feedbackDelta(result, properties);
        add(categories, categoryKey(restaurant), delta, properties.getMaxAbsoluteWeight());
        if (restaurant.averagePrice() != null) {
            add(prices, bandKey("P", restaurant.averagePrice(),
                    properties.getPriceBandUpperBounds()), delta,
                    properties.getMaxAbsoluteWeight());
        }
        add(distances, bandKey("D", Math.max(0, distanceMeters),
                properties.getDistanceBandUpperBounds()), delta,
                properties.getMaxAbsoluteWeight());
        return new TasteProfile(SCHEMA_VERSION, categories, prices, distances,
                feedbackCount + 1, occurredAt);
    }

    public double categoryWeight(Restaurant restaurant) {
        return categoryWeights.getOrDefault(categoryKey(restaurant), 0.0);
    }

    public double priceWeight(Restaurant restaurant, TasteProperties properties) {
        if (restaurant.averagePrice() == null) return 0.0;
        return priceBandWeights.getOrDefault(bandKey("P", restaurant.averagePrice(),
                properties.getPriceBandUpperBounds()), 0.0);
    }

    public double distanceWeight(Restaurant restaurant, TasteProperties properties) {
        return distanceBandWeights.getOrDefault(bandKey("D", restaurant.distanceMeters(),
                properties.getDistanceBandUpperBounds()), 0.0);
    }

    private static String categoryKey(Restaurant restaurant) {
        return restaurant.categoryCode() == null ? "UNKNOWN"
                : restaurant.categoryCode().toUpperCase(Locale.ROOT);
    }

    private static String bandKey(String prefix, int value, java.util.List<Integer> bounds) {
        int index = 0;
        while (index < bounds.size() && value > bounds.get(index)) index++;
        return prefix + index;
    }

    private static double feedbackDelta(String result, TasteProperties properties) {
        return switch (result) {
            case "LIKE" -> properties.getFeedback().getLike();
            case "NORMAL" -> properties.getFeedback().getNormal();
            case "DISLIKE" -> properties.getFeedback().getDislike();
            default -> throw new IllegalArgumentException("未知反馈值: " + result);
        };
    }

    private static void add(Map<String, Double> values, String key, double delta, double max) {
        double updated = values.getOrDefault(key, 0.0) + delta;
        values.put(key, Math.max(-max, Math.min(max, updated)));
    }

    private static Map<String, Double> decayed(Map<String, Double> source, double decay) {
        Map<String, Double> result = new LinkedHashMap<>();
        source.forEach((key, value) -> {
            double decayed = value * decay;
            if (Math.abs(decayed) >= 0.0001) result.put(key, decayed);
        });
        return result;
    }

    private static Map<String, Double> immutable(Map<String, Double> source) {
        return source == null ? Map.of() : Map.copyOf(source);
    }
}
