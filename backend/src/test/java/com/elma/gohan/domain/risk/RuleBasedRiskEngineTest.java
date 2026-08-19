package com.elma.gohan.domain.risk;

import static org.assertj.core.api.Assertions.assertThat;

import com.elma.gohan.config.RiskProperties;
import com.elma.gohan.domain.restaurant.BusinessStatus;
import com.elma.gohan.domain.restaurant.DataCompleteness;
import com.elma.gohan.domain.restaurant.Restaurant;
import com.elma.gohan.provider.evidence.RestaurantEvidence;
import com.elma.gohan.TestRestaurants;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class RuleBasedRiskEngineTest {

    private final RiskProperties props = defaultProps();
    private final RuleBasedRiskEngine engine = new RuleBasedRiskEngine(props);

    private static RiskProperties defaultProps() {
        RiskProperties p = new RiskProperties();
        p.setAlgorithmVersion("risk-v0.1");
        p.setRating(new RiskProperties.RatingThresholds());
        p.setPoints(new RiskProperties.Points());
        p.setLevels(new RiskProperties.Levels());
        return p;
    }

    @Test
    @DisplayName("数据齐全且高评分:0 分 LOW,理由为正面文案")
    void excellentFullData() {
        RiskResult result = engine.evaluate(TestRestaurants.full("p1", 4.6, 300),
                RestaurantEvidence.empty());
        assertThat(result.riskScore()).isZero();
        assertThat(result.riskLevel()).isEqualTo(RiskLevel.LOW);
        assertThat(result.reasons()).contains("评分稳定", "店铺信息完整");
        assertThat(result.algorithmVersion()).isEqualTo("risk-v0.1");
    }

    @Test
    @DisplayName("评分档位:4.3 -> +5,4.1 -> +15,3.5 -> +30")
    void ratingBands() {
        assertThat(engine.evaluate(TestRestaurants.full("p1", 4.3, 300), RestaurantEvidence.empty())
                .riskScore()).isEqualTo(5);
        assertThat(engine.evaluate(TestRestaurants.full("p1", 4.1, 300), RestaurantEvidence.empty())
                .riskScore()).isEqualTo(15);
        assertThat(engine.evaluate(TestRestaurants.full("p1", 3.5, 300), RestaurantEvidence.empty())
                .riskScore()).isEqualTo(30);
    }

    @Test
    @DisplayName("评分缺失按差评档处理并给出理由")
    void ratingMissing() {
        RiskResult result = engine.evaluate(TestRestaurants.full("p1", 3.5, 300).withRating(null),
                RestaurantEvidence.empty());
        assertThat(result.riskScore()).isEqualTo(30);
        assertThat(result.reasons()).contains("评分数据缺失");
    }

    @Test
    @DisplayName("评价数不足 +10,营业信息缺失 +10,价格缺失 +5")
    void missingDataPoints() {
        Restaurant r = new Restaurant(null, "AMAP", "p1", "餐厅", 28.0, 112.0, 100,
                "CHINESE", "中餐厅", 4.5, 5, null,
                BusinessStatus.UNKNOWN, null, "地址", DataCompleteness.PARTIAL);
        RiskResult result = engine.evaluate(r, RestaurantEvidence.empty());
        assertThat(result.riskScore()).isEqualTo(25);
        assertThat(result.reasons()).contains("评价数量不足", "营业信息缺失", "价格信息缺失");
    }

    @Test
    @DisplayName("价格高于候选池均值 1.5 倍 -> +10")
    void priceAnomaly() {
        Restaurant expensive = TestRestaurants.full("p1", 4.5, 300, 100);
        RiskResult result = engine.evaluate(expensive,
                RestaurantEvidence.empty().withPoolAveragePrice(20));
        assertThat(result.riskScore()).isEqualTo(10);
        assertThat(result.reasons()).contains("价格明显高于同批候选");
    }

    @Test
    @DisplayName("等级边界:20/40/60 为 LOW/MEDIUM_LOW/MEDIUM 上界,61+ 为 HIGH")
    void levelBoundaries() {
        assertThat(engine.evaluate(ratingPoor(20), RestaurantEvidence.empty()).riskLevel())
                .isEqualTo(RiskLevel.LOW);
        assertThat(engine.evaluate(ratingPoor(21), RestaurantEvidence.empty()).riskLevel())
                .isEqualTo(RiskLevel.MEDIUM_LOW);
        assertThat(engine.evaluate(ratingPoor(40), RestaurantEvidence.empty()).riskLevel())
                .isEqualTo(RiskLevel.MEDIUM_LOW);
        assertThat(engine.evaluate(ratingPoor(41), RestaurantEvidence.empty()).riskLevel())
                .isEqualTo(RiskLevel.MEDIUM);
        assertThat(engine.evaluate(ratingPoor(60), RestaurantEvidence.empty()).riskLevel())
                .isEqualTo(RiskLevel.MEDIUM);
        assertThat(engine.evaluate(ratingPoor(61), RestaurantEvidence.empty()).riskLevel())
                .isEqualTo(RiskLevel.HIGH);
    }

    /** rating-poor 可配置为任意分值,其余因素全部归零,用于精确构造分数。 */
    private Restaurant ratingPoor(int poorPoints) {
        props.getPoints().setRatingPoor(poorPoints);
        return TestRestaurants.full("p1", 3.0, 300);
    }
}
