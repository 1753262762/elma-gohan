package com.elma.gohan.domain.risk;

import com.elma.gohan.config.RiskProperties;
import com.elma.gohan.provider.evidence.ReviewEvidence;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.stereotype.Component;

/** 中文友好的字符 n-gram Jaccard 近似重复检测，无外部模型依赖。 */
@Component
public class JaccardTemplateCommentDetector implements TemplateCommentDetector {

    private final RiskProperties.Template properties;

    public JaccardTemplateCommentDetector(RiskProperties riskProperties) {
        this.properties = riskProperties.getTemplate();
    }

    @Override
    public TemplateDetectionResult detect(List<ReviewEvidence> reviews) {
        List<Set<String>> grams = new ArrayList<>();
        for (ReviewEvidence review : reviews == null ? List.<ReviewEvidence>of() : reviews) {
            String normalized = normalize(review.text());
            if (normalized.length() >= properties.getMinTextLength()) {
                grams.add(ngrams(normalized, properties.getNgramSize()));
            }
        }
        if (grams.size() < properties.getMinReviews()) {
            return TemplateDetectionResult.insufficient(grams.size());
        }

        UnionFind clusters = new UnionFind(grams.size());
        for (int i = 0; i < grams.size(); i++) {
            for (int j = i + 1; j < grams.size(); j++) {
                if (jaccard(grams.get(i), grams.get(j)) >= properties.getSimilarityThreshold()) {
                    clusters.union(i, j);
                }
            }
        }
        int[] sizes = new int[grams.size()];
        for (int i = 0; i < grams.size(); i++) {
            sizes[clusters.find(i)]++;
        }
        int templateReviews = 0;
        for (int i = 0; i < grams.size(); i++) {
            if (sizes[clusters.find(i)] >= 2) {
                templateReviews++;
            }
        }
        return new TemplateDetectionResult((double) templateReviews / grams.size(),
                grams.size(), templateReviews);
    }

    private String normalize(String text) {
        if (text == null) {
            return "";
        }
        return text.toLowerCase(Locale.ROOT).replaceAll("[^\\p{L}\\p{N}]", "");
    }

    private Set<String> ngrams(String text, int configuredSize) {
        int size = Math.max(1, configuredSize);
        Set<String> result = new HashSet<>();
        if (text.length() <= size) {
            result.add(text);
            return result;
        }
        for (int i = 0; i <= text.length() - size; i++) {
            result.add(text.substring(i, i + size));
        }
        return result;
    }

    private double jaccard(Set<String> left, Set<String> right) {
        if (left.isEmpty() && right.isEmpty()) {
            return 1.0;
        }
        int intersection = 0;
        for (String value : left) {
            if (right.contains(value)) {
                intersection++;
            }
        }
        int union = left.size() + right.size() - intersection;
        return union == 0 ? 0.0 : (double) intersection / union;
    }

    private static final class UnionFind {
        private final int[] parent;
        UnionFind(int size) {
            parent = new int[size];
            for (int i = 0; i < size; i++) parent[i] = i;
        }
        int find(int value) {
            if (parent[value] != value) parent[value] = find(parent[value]);
            return parent[value];
        }
        void union(int left, int right) {
            int a = find(left);
            int b = find(right);
            if (a != b) parent[b] = a;
        }
    }
}
