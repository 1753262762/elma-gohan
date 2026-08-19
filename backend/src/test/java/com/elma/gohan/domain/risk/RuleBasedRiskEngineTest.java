package com.elma.gohan.domain.risk;

import static org.assertj.core.api.Assertions.assertThat;

import com.elma.gohan.TestRestaurants;
import com.elma.gohan.config.RiskProperties;
import com.elma.gohan.provider.evidence.RestaurantEvidence;
import com.elma.gohan.provider.evidence.ReviewEvidence;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class RuleBasedRiskEngineTest {

    private final RiskProperties properties = new RiskProperties();
    private final RuleBasedRiskEngine engine = new RuleBasedRiskEngine(properties);

    @Test
    void noEvidenceLowersConfidenceAndAddsInsufficientRisk() {
        RiskResult result = engine.evaluate(TestRestaurants.full("p1", 4.6, 300),
                RestaurantEvidence.empty());

        assertThat(result.riskScore()).isBetween(0, 100);
        assertThat(result.riskLevel()).isEqualTo(RiskLevel.MEDIUM_LOW);
        assertThat(result.confidence()).isEqualTo(0.25);
        assertThat(result.factors().dataInsufficientRisk()).isEqualTo(85);
        assertThat(result.reasons()).anyMatch(reason -> reason.contains("暂无外部评论证据"));
        assertThat(result.algorithmVersion()).isEqualTo("risk-v0.2");
    }

    @Test
    void externalEvidenceChangesRiskScoreAndConfidence() {
        var restaurant = TestRestaurants.full("p1", 4.6, 300);
        RiskResult empty = engine.evaluate(restaurant, RestaurantEvidence.empty());
        List<ReviewEvidence> reviews = new ArrayList<>();
        Instant now = Instant.now();
        for (int i = 0; i < 30; i++) {
            reviews.add(new ReviewEvidence("r" + i, "真实到店体验不同菜品记录" + i,
                    4.6, now.minus(35L + i, ChronoUnit.DAYS)));
        }
        RiskResult withEvidence = engine.evaluate(restaurant,
                RestaurantEvidence.available("TEST", reviews, now));

        assertThat(withEvidence.riskScore()).isLessThan(empty.riskScore());
        assertThat(withEvidence.confidence()).isGreaterThan(empty.confidence());
        assertThat(withEvidence.factors().ratingRisk()).isZero();
    }

    @Test
    void scoreAndConfidenceAreAlwaysClamped() {
        properties.getWeights().setRating(5.0);
        properties.getWeights().setDataInsufficient(5.0);
        RiskResult result = engine.evaluate(TestRestaurants.full("p1", 1.0, 300)
                        .withRating(null), RestaurantEvidence.unavailable("TEST"));
        assertThat(result.riskScore()).isEqualTo(100);
        assertThat(result.confidence()).isBetween(0.0, 1.0);
        assertThat(result.factors().ratingRisk()).isEqualTo(100);
    }
}
