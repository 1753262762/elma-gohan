package com.elma.gohan.domain.risk;

import static org.assertj.core.api.Assertions.assertThat;

import com.elma.gohan.config.RiskProperties;
import com.elma.gohan.provider.evidence.ReviewEvidence;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class JaccardTemplateCommentDetectorTest {

    @Test
    void calculatesTemplateMemberRatio() {
        RiskProperties properties = new RiskProperties();
        properties.getTemplate().setSimilarityThreshold(0.75);
        var detector = new JaccardTemplateCommentDetector(properties);
        List<ReviewEvidence> reviews = new ArrayList<>();
        for (int i = 0; i < 28; i++) {
            reviews.add(new ReviewEvidence("t" + i, "环境很好服务很好味道很好值得推荐", 5.0, null));
        }
        for (int i = 28; i < 100; i++) {
            reviews.add(new ReviewEvidence("u" + i, "第" + i + "次到店品尝菜品编号" + (i * 17)
                    + "口感记录各不相同", 4.0, null));
        }

        TemplateDetectionResult result = detector.detect(reviews);
        assertThat(result.eligibleReviews()).isEqualTo(100);
        assertThat(result.templateReviews()).isGreaterThanOrEqualTo(28);
        assertThat(result.templateRatio()).isGreaterThanOrEqualTo(0.28);
    }

    @Test
    void insufficientTextSamplesReturnZero() {
        var detector = new JaccardTemplateCommentDetector(new RiskProperties());
        assertThat(detector.detect(List.of(
                new ReviewEvidence("1", "太短", 5.0, null))).templateRatio()).isZero();
    }
}
