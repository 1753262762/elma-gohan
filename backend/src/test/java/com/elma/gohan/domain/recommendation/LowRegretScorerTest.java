package com.elma.gohan.domain.recommendation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import com.elma.gohan.TestRestaurants;
import com.elma.gohan.config.RecommendationProperties;
import com.elma.gohan.domain.restaurant.SearchCondition;
import com.elma.gohan.domain.risk.RiskLevel;
import com.elma.gohan.domain.risk.RiskFactors;
import com.elma.gohan.domain.risk.RiskResult;
import java.util.List;
import java.util.Map;
import java.time.LocalDateTime;
import com.elma.gohan.domain.restaurant.BusinessStatus;
import com.elma.gohan.domain.restaurant.DataCompleteness;
import com.elma.gohan.domain.restaurant.Restaurant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class LowRegretScorerTest {

    private final RecommendationProperties props = new RecommendationProperties();
    private final LowRegretScorer scorer = new LowRegretScorer(props);

    private RiskResult risk(int score) {
        return new RiskResult(score, RiskLevel.LOW, List.of("评分稳定"), "risk-v0.3");
    }

    @Test
    @DisplayName("分数范围 0~100,数据齐全近距离高分,数据缺失远距离低分")
    void scoreRangeAndOrdering() {
        var condition = new SearchCondition(1000, 40, "ANY", List.of());
        double near = scorer.score(TestRestaurants.full("a", 4.6, 100, 20), risk(0), condition);
        double far = scorer.score(TestRestaurants.full("b", 3.0, 1000, null), risk(60), condition);
        assertThat(near).isCloseTo(90.0, within(15.0));
        assertThat(far).isLessThan(65.0);
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

    @Test
    @DisplayName("新用户排序中性，老用户历史品类偏好改变分数")
    void tasteProfileChangesOldUserRanking() {
        Restaurant chinese = category("c", "CHINESE");
        Restaurant foreign = category("f", "FOREIGN");
        var condition = new SearchCondition(1000, null, "ANY", List.of());
        double newChinese = scorer.score(chinese, risk(10), new UserPreference(condition));
        double newForeign = scorer.score(foreign, risk(10), new UserPreference(condition));
        assertThat(newChinese).isEqualTo(newForeign);

        TasteProfile profile = new TasteProfile(2, Map.of("CHINESE", 3.0, "FOREIGN", -3.0),
                Map.of(), Map.of(), 6, LocalDateTime.now());
        var oldUser = new UserPreference(condition, profile);
        assertThat(scorer.score(chinese, risk(10), oldUser))
                .isGreaterThan(scorer.score(foreign, risk(10), oldUser));
    }

    @Test
    @DisplayName("低可信的表面低风险会按不确定性风险校正")
    void lowConfidenceRiskDoesNotGainFalseAdvantage() {
        Restaurant restaurant = category("c", "CHINESE");
        var condition = new SearchCondition(1000, null, "ANY", List.of());
        RiskResult trusted = new RiskResult(0, RiskLevel.LOW, 1.0, RiskFactors.empty(),
                List.of("证据充分"), "risk-v0.3");
        RiskResult uncertain = new RiskResult(0, RiskLevel.LOW, 0.0, RiskFactors.empty(),
                List.of("证据不足"), "risk-v0.3");

        assertThat(scorer.score(restaurant, trusted, condition))
                .isGreaterThan(scorer.score(restaurant, uncertain, condition));
    }

    @Test
    @DisplayName("距离因子与搜索半径解耦:同一餐厅不因 radius 改变得分")
    void distanceFactorIndependentOfRadius() {
        Restaurant r = TestRestaurants.full("a", 4.5, 800);
        double smallRadius = scorer.score(r, risk(10),
                new SearchCondition(500, null, "ANY", List.of()));
        double largeRadius = scorer.score(r, risk(10),
                new SearchCondition(3000, null, "ANY", List.of()));
        assertThat(smallRadius).isEqualTo(largeRadius);
    }

    @Test
    @DisplayName("自由文本 dislike 命中名称时软降权而非剔除")
    void dislikePenaltyLowersScore() {
        var clean = new SearchCondition(1000, null, "ANY", List.of());
        var disliked = new SearchCondition(1000, null, "ANY", List.of("面对面"));
        Restaurant r = TestRestaurants.full("a", "面对面餐厅", 4.5, 300, 30);
        assertThat(scorer.score(r, risk(10), disliked))
                .isLessThan(scorer.score(r, risk(10), clean));
    }

    @Test
    @DisplayName("口味校正按剩余空间施加:高分候选不饱和堆顶")
    void tasteAdjustmentBoundedAtHighBase() {
        Restaurant strong = TestRestaurants.full("a", 5.0, 10, 10);
        var condition = new SearchCondition(1000, 40, "ANY", List.of());
        TasteProfile positive = new TasteProfile(2, Map.of("CHINESE", 3.0),
                Map.of(), Map.of(), 6, LocalDateTime.now());
        double withoutTaste = scorer.score(strong, risk(0), new UserPreference(condition));
        double withTaste = scorer.score(strong, risk(0), new UserPreference(condition, positive));
        // 基础分越高,正向口味校正的实际加成越小
        assertThat(withTaste).isGreaterThan(withoutTaste);
        assertThat(withTaste).isLessThanOrEqualTo(100.0);
        assertThat(withTaste - withoutTaste).isLessThan(15.0);
    }

    @Test
    @DisplayName("用户未设预算时不生成\"预算合适\"理由")
    void budgetReasonOnlyWhenBudgetProvided() {
        var condition = new SearchCondition(1000, null, "ANY", List.of());
        var reasons = scorer.reasons(TestRestaurants.full("a", 4.6, 100), risk(0), condition);
        assertThat(reasons).doesNotContain("预算合适");
    }

    private Restaurant category(String id, String category) {
        return new Restaurant(null, "AMAP", id, "餐厅" + id, 28, 112, 300,
                category, category, 4.5, 100, 50, BusinessStatus.OPEN,
                "09:00-21:00", "地址", DataCompleteness.FULL);
    }
}
