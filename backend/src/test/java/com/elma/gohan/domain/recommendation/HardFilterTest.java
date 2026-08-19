package com.elma.gohan.domain.recommendation;

import static org.assertj.core.api.Assertions.assertThat;

import com.elma.gohan.TestRestaurants;
import com.elma.gohan.domain.restaurant.BusinessStatus;
import com.elma.gohan.domain.restaurant.Restaurant;
import com.elma.gohan.domain.restaurant.SearchCondition;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class HardFilterTest {

    private final HardFilter filter = new HardFilter();

    private SearchCondition condition(Integer radius, Integer budget, String category,
                                      List<String> dislikes) {
        return new SearchCondition(radius, budget, category, dislikes);
    }

    @Test
    @DisplayName("距离超过半径剔除")
    void distanceFilter() {
        var list = List.of(TestRestaurants.full("a", 4.5, 500), TestRestaurants.full("b", 4.5, 501));
        assertThat(filter.filter(list, condition(500, null, null, List.of())))
                .extracting(Restaurant::sourcePoiId)
                .containsExactly("a");
    }

    @Test
    @DisplayName("人均价高于预算剔除;价格缺失且预算非空时保留")
    void budgetFilter() {
        var expensive = TestRestaurants.full("a", 4.5, 300, 50);
        var noPrice = TestRestaurants.full("b", 4.5, 300, null);
        var cheap = TestRestaurants.full("c", 4.5, 300, 20);
        var result = filter.filter(List.of(expensive, noPrice, cheap), condition(1000, 40, null, List.of()));
        assertThat(result).extracting(Restaurant::sourcePoiId).containsExactlyInAnyOrder("b", "c");
    }

    @Test
    @DisplayName("品类:ANY/空 不限;指定 code 只留精确匹配(大小写不敏感)")
    void categoryFilter() {
        var chinese = TestRestaurants.full("a", 4.5, 300);
        var snack = new Restaurant(null, "AMAP", "b", "快餐", 28.0, 112.0, 300,
                "SNACK", "小吃快餐", 4.5, 100, 30,
                BusinessStatus.UNKNOWN, "09:00-21:00", "地址", com.elma.gohan.domain.restaurant.DataCompleteness.FULL);
        assertThat(filter.filter(List.of(chinese, snack), condition(1000, null, "ANY", List.of())))
                .hasSize(2);
        assertThat(filter.filter(List.of(chinese, snack), condition(1000, null, "SNACK", List.of())))
                .extracting(Restaurant::sourcePoiId).containsExactly("b");
    }

    @Test
    @DisplayName("营业状态 CLOSED 剔除,UNKNOWN 保留")
    void businessStatusFilter() {
        var closed = new Restaurant(null, "AMAP", "a", "餐厅", 28.0, 112.0, 300,
                "CHINESE", "中餐厅", 4.5, 100, 30,
                BusinessStatus.CLOSED, "09:00-21:00", "地址", com.elma.gohan.domain.restaurant.DataCompleteness.FULL);
        var unknown = TestRestaurants.full("b", 4.5, 300);
        assertThat(filter.filter(List.of(closed, unknown), condition(1000, null, null, List.of())))
                .extracting(Restaurant::sourcePoiId).containsExactly("b");
    }

    @Test
    @DisplayName("dislikes:命中名称或品类 label 剔除")
    void dislikeFilter() {
        var beefNoodle = TestRestaurants.full("a", "老街牛肉粉", 4.5, 300, 30);
        var dessert = new Restaurant(null, "AMAP", "b", "甜品店", 28.0, 112.0, 300,
                "DESSERT", "蛋糕甜品", 4.5, 100, 30,
                BusinessStatus.UNKNOWN, "09:00-21:00", "地址", com.elma.gohan.domain.restaurant.DataCompleteness.FULL);
        assertThat(filter.filter(List.of(beefNoodle, dessert), condition(1000, null, null, List.of("牛肉", "香菜"))))
                .extracting(Restaurant::sourcePoiId).containsExactly("b");
    }
}
