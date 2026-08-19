package com.elma.gohan.domain.recommendation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import com.elma.gohan.TestRestaurants;
import com.elma.gohan.config.RecommendationProperties;
import com.elma.gohan.domain.restaurant.SearchCondition;
import com.elma.gohan.domain.risk.RiskLevel;
import com.elma.gohan.domain.risk.RiskResult;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class LowRegretScorerTest {

    private final RecommendationProperties props = new RecommendationProperties();
    private final LowRegretScorer scorer = new LowRegretScorer(props);

    private RiskResult risk(int score) {
        return new RiskResult(score, RiskLevel.LOW, List.of("评分稳定"), "risk-v0.1");
    }

    @Test
    @DisplayName("分数范围 0~100,数据齐全近距离高分,数据缺失远距离低分")
    void scoreRangeAndOrdering() {
        var condition = new SearchCondition(1000, 40, "ANY", List.of());
        double near = scorer.score(TestRestaurants.full("a", 4.6, 100, 20), risk(0), condition);
        double far = scorer.score(TestRestaurants.full("b", 3.0, 1000, null), risk(60), condition);
        assertThat(near).isCloseTo(90.0, within(15.0));
        assertThat(far).isLessThan(50.0);
        assertThat(far).isGreaterThanOrEqualTo(0);
        assertThat(near).isLessThanOrEqualTo(100);
        assertThat(near).isGreaterThan(far);
    }

    @Test
    @DisplayName("同等条件下距离更近得分更高")
    void closerScoresHigher() {
        var condition = new SearchCondition(3000, null, "ANY", List.of());
        double near = scorer.score(TestRestaurants.full("a", 4.5, 500), risk(10), condition);
        double far = scorer.score(TestRestaurants.full("b", 4.5, 2500), risk(10), condition);
        assertThat(near).isGreaterThan(far);
    }

    @Test
    @DisplayName("推荐理由非空且不超过 5 条")
    void reasonsBounded() {
        var condition = new SearchCondition(1000, null, "ANY", List.of());
        var reasons = scorer.reasons(TestRestaurants.full("a", 4.6, 100), risk(0), condition);
        assertThat(reasons).isNotEmpty();
        assertThat(reasons).hasSizeLessThanOrEqualTo(5);
    }
}
