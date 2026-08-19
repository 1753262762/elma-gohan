package com.elma.gohan.domain.risk;

public record TemplateDetectionResult(double templateRatio, int eligibleReviews,
                                      int templateReviews) {
    public static TemplateDetectionResult insufficient(int eligibleReviews) {
        return new TemplateDetectionResult(0.0, eligibleReviews, 0);
    }
}
