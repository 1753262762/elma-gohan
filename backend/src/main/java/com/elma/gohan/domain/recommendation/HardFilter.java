package com.elma.gohan.domain.recommendation;

import com.elma.gohan.domain.restaurant.BusinessStatus;
import com.elma.gohan.domain.restaurant.Restaurant;
import com.elma.gohan.domain.restaurant.SearchCondition;
import com.elma.gohan.domain.restaurant.TextNormalizer;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Component;

/**
 * 硬条件过滤:距离 / 预算 / 品类 / 营业状态 / 不想吃关键词。
 * averagePrice 缺失且预算非空时不剔除(数据缺失不等于超预算,交给 RiskEngine 加分)。
 *
 * dislike 硬剔除只针对结构化品类:关键词归一化后与品类 label 精确一致、
 * 或等于细品类 code 时才剔除;自由文本命中名称/品类由 LowRegretScorer 软降权,
 * 避免"面"误杀"面对面"这类子串误伤。
 */
@Component
public class HardFilter {

    public List<Restaurant> filter(List<Restaurant> restaurants, SearchCondition condition) {
        return restaurants.stream()
                .filter(r -> passes(r, condition))
                .toList();
    }

    private boolean passes(Restaurant r, SearchCondition c) {
        if (r.distanceMeters() > c.radius()) {
            return false;
        }
        if (c.maxBudget() != null && r.averagePrice() != null && r.averagePrice() > c.maxBudget()) {
            return false;
        }
        if (!c.categoryFilter().matches(r.categoryCode())) {
            return false;
        }
        if (r.businessStatus() == BusinessStatus.CLOSED) {
            return false;
        }
        return !hitsDislike(r, c.dislikes());
    }

    /** 结构化品类排除:归一化关键词与品类 label 完全一致,或等于品类 code(大小写不敏感)。 */
    private boolean hitsDislike(Restaurant r, List<String> dislikes) {
        if (dislikes == null || dislikes.isEmpty()) {
            return false;
        }
        String label = TextNormalizer.normalize(r.categoryLabel());
        String code = r.categoryCode() == null
                ? "" : r.categoryCode().toLowerCase(Locale.ROOT);
        return dislikes.stream()
                .map(TextNormalizer::normalize)
                .anyMatch(d -> !d.isEmpty() && (d.equals(label) || d.equals(code)));
    }
}
