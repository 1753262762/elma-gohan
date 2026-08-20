package com.elma.gohan.provider.evidence;

import com.elma.gohan.config.EntityResolutionProperties;
import com.elma.gohan.domain.restaurant.Restaurant;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

/** 透明、确定性的高德餐厅与百度 POI 一对一匹配。 */
@Component
public class EntityResolver {

    private static final Pattern NON_TEXT = Pattern.compile("[^\\p{IsHan}a-z0-9]");
    private static final Pattern STORE_SUFFIX = Pattern.compile("(旗舰店|门店|分店|店)$");
    private final EntityResolutionProperties properties;

    public EntityResolver(EntityResolutionProperties properties) {
        this.properties = properties;
    }

    public Map<String, EntityMatchResult> resolve(List<Restaurant> restaurants,
                                                   List<PlatformEvidence> evidence,
                                                   Set<String> reservedProviderIds) {
        Map<String, List<ScoredCandidate>> candidates = new HashMap<>();
        for (Restaurant restaurant : restaurants) {
            List<ScoredCandidate> scores = new ArrayList<>();
            for (PlatformEvidence item : evidence) {
                if (reservedProviderIds.contains(item.providerPoiId())) continue;
                ScoredCandidate candidate = score(restaurant, item);
                if (candidate != null) scores.add(candidate);
            }
            scores.sort(Comparator.comparingDouble(ScoredCandidate::score).reversed()
                    .thenComparing(c -> c.evidence().providerPoiId()));
            candidates.put(restaurant.sourcePoiId(), scores);
        }

        List<PrimaryCandidate> accepted = new ArrayList<>();
        Map<String, EntityMatchResult> result = new LinkedHashMap<>();
        for (Restaurant restaurant : restaurants) {
            List<ScoredCandidate> scores = candidates.get(restaurant.sourcePoiId());
            if (scores == null || scores.isEmpty()) {
                result.put(restaurant.sourcePoiId(), EntityMatchResult.noMatch());
                continue;
            }
            if (scores.get(0).score() < properties.getAcceptThreshold()) {
                result.put(restaurant.sourcePoiId(),
                        EntityMatchResult.noMatch(scores.get(0).features()));
                continue;
            }
            ScoredCandidate best = scores.get(0);
            if (scores.size() > 1
                    && best.score() - scores.get(1).score() < properties.getAmbiguityMargin()) {
                result.put(restaurant.sourcePoiId(), new EntityMatchResult(
                        EntityMatchStatus.AMBIGUOUS, round(best.score()), null, best.features()));
                continue;
            }
            accepted.add(new PrimaryCandidate(restaurant.sourcePoiId(), best));
        }

        accepted.sort(Comparator.comparingDouble(
                (PrimaryCandidate value) -> value.candidate().score()).reversed()
                .thenComparing(PrimaryCandidate::primaryPoiId));
        Set<String> used = new HashSet<>(reservedProviderIds);
        for (PrimaryCandidate primary : accepted) {
            ScoredCandidate candidate = primary.candidate();
            if (!used.add(candidate.evidence().providerPoiId())) {
                result.put(primary.primaryPoiId(),
                        EntityMatchResult.noMatch(candidate.features()));
                continue;
            }
            result.put(primary.primaryPoiId(), new EntityMatchResult(EntityMatchStatus.MATCHED,
                    round(candidate.score()), candidate.evidence(), candidate.features()));
        }
        return result;
    }

    private ScoredCandidate score(Restaurant restaurant, PlatformEvidence evidence) {
        String restaurantName = normalizeName(restaurant.name());
        String evidenceName = normalizeName(evidence.name());
        String restaurantAddress = normalizeText(restaurant.address());
        String evidenceAddress = normalizeText(evidence.address());
        double name = jaccardBigrams(restaurantName, evidenceName);
        double address = jaccardBigrams(restaurantAddress, evidenceAddress);
        double distance = haversineMeters(restaurant.latitude(), restaurant.longitude(),
                evidence.latitude(), evidence.longitude());
        Set<String> restaurantTelephones = telephones(restaurant.telephone());
        Set<String> evidenceTelephones = telephones(evidence.telephone());
        boolean telephoneComparable = !restaurantTelephones.isEmpty()
                && !evidenceTelephones.isEmpty();
        boolean telephoneExact = telephoneComparable
                && restaurantTelephones.stream().anyMatch(evidenceTelephones::contains);
        if (distance > properties.getMaximumDistanceMeters() && !telephoneExact) return null;
        if (name < properties.getMinimumNameSimilarity() && !telephoneExact) return null;

        boolean addressComparable = !restaurantAddress.isBlank() && !evidenceAddress.isBlank();
        boolean coordinateComparable = evidence.latitude() != null && evidence.longitude() != null;
        boolean sparseEvidence = !addressComparable || !telephoneComparable;
        if (sparseEvidence && !telephoneExact
                && (name < properties.getSparseMatchMinimumNameSimilarity()
                || distance > properties.getSparseMatchMaximumDistanceMeters())) {
            return null;
        }

        double coordinate = coordinateSimilarity(distance);
        double telephone = telephoneExact ? 1.0 : 0.0;
        double weightedScore = name * properties.getNameWeight();
        double availableWeight = properties.getNameWeight();
        if (coordinateComparable) {
            weightedScore += coordinate * properties.getCoordinateWeight();
            availableWeight += properties.getCoordinateWeight();
        }
        if (addressComparable) {
            weightedScore += address * properties.getAddressWeight();
            availableWeight += properties.getAddressWeight();
        }
        if (telephoneComparable) {
            weightedScore += telephone * properties.getTelephoneWeight();
            availableWeight += properties.getTelephoneWeight();
        }
        double score = availableWeight == 0.0 ? 0.0 : weightedScore / availableWeight;
        if (telephoneExact && name >= 0.7) score = Math.max(score, 0.85);
        Map<String, Double> features = new LinkedHashMap<>();
        features.put("name", round(name));
        features.put("coordinate", round(coordinate));
        features.put("address", round(address));
        features.put("telephone", telephone);
        features.put("distanceMeters", round(distance));
        features.put("availableWeight", round(availableWeight));
        features.put("weightedScore", round(weightedScore));
        return new ScoredCandidate(evidence, Math.min(1.0, score), features);
    }

    private double coordinateSimilarity(double meters) {
        if (meters <= 30) return 1.0;
        if (meters <= 100) return 1.0 - (meters - 30) * 0.4 / 70.0;
        if (meters <= 300) return 0.6 - (meters - 100) * 0.6 / 200.0;
        return 0.0;
    }

    static String normalizeName(String value) {
        return STORE_SUFFIX.matcher(normalizeText(value)).replaceFirst("");
    }

    static String normalizeText(String value) {
        if (value == null) return "";
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFKC)
                .toLowerCase(Locale.ROOT);
        return NON_TEXT.matcher(normalized).replaceAll("");
    }

    private static Set<String> telephones(String value) {
        if (value == null || value.isBlank()) return Set.of();
        Set<String> result = new LinkedHashSet<>();
        // 空格和连字符常是同一个号码的排版字符；只用多号码分隔符拆分。
        for (String part : value.split("[,;/]+")) {
            String digits = part.replaceAll("[^0-9]", "");
            if (digits.length() >= 7) result.add(digits);
        }
        return result;
    }

    private static double jaccardBigrams(String first, String second) {
        if (first.isBlank() || second.isBlank()) return 0.0;
        if (first.equals(second)) return 1.0;
        Set<String> left = bigrams(first);
        Set<String> right = bigrams(second);
        Set<String> intersection = new HashSet<>(left);
        intersection.retainAll(right);
        Set<String> union = new HashSet<>(left);
        union.addAll(right);
        return union.isEmpty() ? 0.0 : (double) intersection.size() / union.size();
    }

    private static Set<String> bigrams(String value) {
        if (value.length() < 2) return Set.of(value);
        Set<String> result = new LinkedHashSet<>();
        for (int i = 0; i < value.length() - 1; i++) result.add(value.substring(i, i + 2));
        return result;
    }

    private static double haversineMeters(double lat1, double lng1, Double lat2, Double lng2) {
        if (lat2 == null || lng2 == null) return Double.MAX_VALUE;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        return 6_371_000 * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    private static double round(double value) {
        return Math.round(value * 1000.0) / 1000.0;
    }

    private record ScoredCandidate(PlatformEvidence evidence, double score,
                                   Map<String, Double> features) { }
    private record PrimaryCandidate(String primaryPoiId, ScoredCandidate candidate) { }
}
