package com.elma.gohan.domain.recommendation;

import static org.assertj.core.api.Assertions.assertThat;

import com.elma.gohan.TestRestaurants;
import com.elma.gohan.config.RecommendationProperties;
import com.elma.gohan.domain.restaurant.Restaurant;
import com.elma.gohan.domain.restaurant.SearchCondition;
import com.elma.gohan.domain.risk.RiskLevel;
import com.elma.gohan.domain.risk.RiskResult;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class DefaultRecommendationEngineTest {

    private final RecommendationProperties props = new RecommendationProperties();
    private final DefaultRecommendationEngine engine = new DefaultRecommendationEngine(
            new HardFilter(), new LowRegretScorer(props), props);

    private Map<String, RiskResult> risks(List<Restaurant> restaurants) {
        return restaurants.stream().collect(Collectors.toMap(
                Restaurant::sourcePoiId,
                r -> new RiskResult(10, RiskLevel.LOW, List.of("评分稳定"), "risk-v0.1")));
    }

    @Test
    @DisplayName("8 家合格候选 -> 候选池大小 3,且互不相同")
    void poolSizeThree() {
        List<Restaurant> restaurants = java.util.stream.IntStream.rangeClosed(1, 8)
                .mapToObj(i -> TestRestaurants.full("p" + i, 4.0 + i * 0.05, 200 + i * 10))
                .toList();
        var result = engine.recommend(restaurants, risks(restaurants),
                new UserPreference(new SearchCondition(1000, null, "ANY", List.of())));
        assertThat(result.pool()).hasSize(3);
        assertThat(result.pool()).extracting(c -> c.restaurant().sourcePoiId())
                .doesNotHaveDuplicates();
        assertThat(result.algorithmVersion()).isEqualTo("lowregret-v0.1");
    }

    @Test
    @DisplayName("HIGH 风险候选不进入候选池")
    void highRiskBlocked() {
        Restaurant good = TestRestaurants.full("good", 4.6, 200);
        Restaurant bad = TestRestaurants.full("bad", 4.6, 200);
        var risks = Map.of(
                "good", new RiskResult(10, RiskLevel.LOW, List.of("评分稳定"), "risk-v0.1"),
                "bad", new RiskResult(80, RiskLevel.HIGH, List.of("评分偏低"), "risk-v0.1"));
        var result = engine.recommend(List.of(good, bad), risks,
                new UserPreference(new SearchCondition(1000, null, "ANY", List.of())));
        assertThat(result.pool()).extracting(c -> c.restaurant().sourcePoiId()).containsExactly("good");
    }

    @Test
    @DisplayName("全部被硬过滤 -> 空候选池")
    void emptyWhenAllFiltered() {
        Restaurant far = TestRestaurants.full("far", 4.5, 5000);
        var result = engine.recommend(List.of(far), risks(List.of(far)),
                new UserPreference(new SearchCondition(1000, null, "ANY", List.of())));
        assertThat(result.pool()).isEmpty();
    }

    @Test
    @DisplayName("候选少于池大小:池大小跟随候选数")
    void poolFollowsCandidateCount() {
        List<Restaurant> two = List.of(TestRestaurants.full("a", 4.5, 100),
                TestRestaurants.full("b", 4.5, 200));
        var result = engine.recommend(two, risks(two),
                new UserPreference(new SearchCondition(1000, null, "ANY", List.of())));
        assertThat(result.pool()).hasSize(2);
    }
}
