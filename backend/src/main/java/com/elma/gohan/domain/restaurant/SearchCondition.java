package com.elma.gohan.domain.restaurant;

import java.util.List;

/**
 * 一次推荐请求的搜索条件(radius 已在入口校验为 500/1000/2000/3000;
 * maxBudget 为 null 表示不限;category 缺省为 MEAL;dislikes 为关键词短语)。
 */
public record SearchCondition(
        int radius,
        Integer maxBudget,
        String category,
        List<String> dislikes
) {
    public static final String CATEGORY_MEAL = "MEAL";
    public static final String CATEGORY_ANY = "ANY";

    public SearchCondition {
        category = CategoryFilter.fromRequest(category).name();
        dislikes = dislikes == null ? List.of() : List.copyOf(dislikes);
    }

    public boolean categoryUnlimited() {
        return categoryFilter() == CategoryFilter.ANY;
    }

    public CategoryFilter categoryFilter() {
        return CategoryFilter.fromRequest(category);
    }
}
