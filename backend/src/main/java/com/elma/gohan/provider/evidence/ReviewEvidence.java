package com.elma.gohan.provider.evidence;

import java.time.Instant;

/** 进入领域层前完成标准化的单条评论证据。 */
public record ReviewEvidence(
        String externalReviewId,
        String text,
        Double rating,
        Instant createdAt
) {
}
