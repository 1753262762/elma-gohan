package com.elma.gohan.domain.risk;

import static org.assertj.core.api.Assertions.assertThat;

import com.elma.gohan.TestRestaurants;
import com.elma.gohan.config.RiskProperties;
import com.elma.gohan.provider.evidence.RestaurantEvidence;
import com.elma.gohan.provider.evidence.ReviewEvidence;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
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
        assertThat(result.algorithmVersion()).isEqualTo("risk-v0.3");
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

    @Test
    void staleEvidenceLowersConfidence() {
        Instant now = Instant.parse("2026-08-19T00:00:00Z");
        Clock clock = Clock.fixed(now, ZoneOffset.UTC);
        RuleBasedRiskEngine fixedEngine = new RuleBasedRiskEngine(properties,
                new JaccardTemplateCommentDetector(properties),
                new SlidingWindowBurstDetector(properties),
                new RuleBasedRecentTrendDetector(properties, clock), clock);
        var restaurant = TestRestaurants.full("p1", 4.6, 300);

        List<ReviewEvidence> fresh = new ArrayList<>();
        List<ReviewEvidence> stale = new ArrayList<>();
        for (int i = 0; i < 30; i++) {
            fresh.add(new ReviewEvidence("f" + i, "近期真实体验记录各有不同" + i,
                    4.6, now.minus(30L + i, ChronoUnit.DAYS)));
            stale.add(new ReviewEvidence("s" + i, "多年前的真实体验记录各有不同" + i,
                    4.6, now.minus(400L + i, ChronoUnit.DAYS)));
        }

        RiskResult freshResult = fixedEngine.evaluate(restaurant,
                RestaurantEvidence.available("TEST", fresh, now));
        RiskResult staleResult = fixedEngine.evaluate(restaurant,
                RestaurantEvidence.available("TEST", stale, now));
        // 全部评论超出新鲜度窗口 -> confidence 收缩,不再与近期证据同权
        assertThat(staleResult.confidence()).isLessThan(freshResult.confidence());
    }

    @Test
    void futureReviewsAndStaleFetchDoNotIncreaseConfidence() {
        Instant now = Instant.parse("2026-08-19T00:00:00Z");
        Clock clock = Clock.fixed(now, ZoneOffset.UTC);
        RuleBasedRiskEngine fixedEngine = new RuleBasedRiskEngine(properties,
                new JaccardTemplateCommentDetector(properties),
                new SlidingWindowBurstDetector(properties),
                new RuleBasedRecentTrendDetector(properties, clock), clock);
        var restaurant = TestRestaurants.full("p1", 4.6, 300);
        List<ReviewEvidence> future = java.util.stream.IntStream.range(0, 30)
                .mapToObj(i -> new ReviewEvidence("f" + i, "未来评论" + i, 4.6,
                        now.plus(i + 1L, ChronoUnit.DAYS)))
                .toList();
        List<ReviewEvidence> recent = java.util.stream.IntStream.range(0, 30)
                .mapToObj(i -> new ReviewEvidence("r" + i, "近期评论" + i, 4.6,
                        now.minus(i + 1L, ChronoUnit.DAYS)))
                .toList();

        RiskResult futureResult = fixedEngine.evaluate(restaurant,
                RestaurantEvidence.available("TEST", future, now));
        RiskResult staleFetchResult = fixedEngine.evaluate(restaurant,
                RestaurantEvidence.available("TEST", recent,
                        now.minus(121, ChronoUnit.DAYS)));
        RiskResult freshResult = fixedEngine.evaluate(restaurant,
                RestaurantEvidence.available("TEST", recent, now));

        assertThat(futureResult.confidence()).isEqualTo(0.25);
        assertThat(staleFetchResult.confidence()).isEqualTo(0.25);
        assertThat(freshResult.confidence()).isGreaterThan(0.25);
    }

    @Test
    void mildTrendDownDoesNotTriggerFullTrendRisk() {
        Instant now = Instant.parse("2026-08-19T00:00:00Z");
        Clock clock = Clock.fixed(now, ZoneOffset.UTC);
        RuleBasedRiskEngine fixedEngine = new RuleBasedRiskEngine(properties,
                new JaccardTemplateCommentDetector(properties),
                new SlidingWindowBurstDetector(properties),
                new RuleBasedRecentTrendDetector(properties, clock), clock);
        var restaurant = TestRestaurants.full("p1", 4.6, 300);
        List<ReviewEvidence> reviews = new ArrayList<>();
        for (int i = 0; i < 30; i++) {
            reviews.add(new ReviewEvidence("h" + i, "历史体验记录各有不同" + i,
                    4.8, now.minus(40L + i, ChronoUnit.DAYS)));
        }
        for (int i = 0; i < 10; i++) {
            reviews.add(new ReviewEvidence("r" + i, "近期体验记录各有不同" + i,
                    4.2, now.minus(i + 1L, ChronoUnit.DAYS)));
        }
        RiskResult result = fixedEngine.evaluate(restaurant,
                RestaurantEvidence.available("TEST", reviews, now));
        // 小幅下降(0.6,刚过阈值)+ 小样本:连续风险函数不应给出顶格 trend 风险
        assertThat(result.factors().trendRisk()).isLessThan(100);
        assertThat(result.factors().trendRisk()).isGreaterThan(10);
    }
}
