package com.elma.gohan.domain.restaurant;

import java.util.List;

/**
 * 一次推荐请求的搜索条件(radius 已在入口校验为 500/1000/2000/3000;
 * maxBudget 为 null 表示不限;category 为 null 或 ANY 表示不限;dislikes 为关键词短语)。
 */
public record SearchCondition(
        int radius,
        Integer maxBudget,
        String category,
        List<String> dislikes
) {
    public static final String CATEGORY_ANY = "ANY";

    public boolean categoryUnlimited() {
        return category == null || category.isBlank() || CATEGORY_ANY.equalsIgnoreCase(category);
    }
}
