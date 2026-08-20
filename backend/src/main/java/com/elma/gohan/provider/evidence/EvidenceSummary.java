package com.elma.gohan.provider.evidence;

/** 对客户端安全的证据摘要，不包含第三方 POI ID、原始响应或匹配特征。 */
public record EvidenceSummary(
        EntityMatchStatus matchStatus,
        Double matchConfidence,
        ConsistencyLevel consistency,
        Double ratingDifference,
        String reason,
        SourceSummary amap,
        SourceSummary baidu
) {
    public record SourceSummary(
            EvidenceStatus status,
            Double rating,
            Double tasteRating,
            Double serviceRating,
            Double environmentRating,
            Integer averagePrice,
            Integer commentCount
    ) {
        public static SourceSummary from(PlatformEvidence evidence) {
            if (evidence == null) {
                return new SourceSummary(EvidenceStatus.NO_DATA, null, null, null,
                        null, null, null);
            }
            return new SourceSummary(evidence.status(), evidence.overallRating(),
                    evidence.tasteRating(), evidence.serviceRating(),
                    evidence.environmentRating(), evidence.averagePrice(),
                    evidence.commentCount());
        }
    }
}
