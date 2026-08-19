package com.elma.gohan.domain.risk;

import com.elma.gohan.domain.restaurant.Restaurant;
import com.elma.gohan.provider.evidence.RestaurantEvidence;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 风险引擎抽象。实现必须自带 algorithmVersion,阈值配置化。
 */
public interface RiskEngine {

    RiskResult evaluate(Restaurant restaurant, RestaurantEvidence evidence);

    /** 批量评估:便于实现内部使用候选池上下文(如价格异常以池均值为基准)。键为 sourcePoiId。 */
    default Map<String, RiskResult> evaluateAll(List<Restaurant> restaurants,
                                                Function<Restaurant, RestaurantEvidence> evidenceSupplier) {
        return restaurants.stream().collect(Collectors.toMap(
                Restaurant::sourcePoiId,
                r -> evaluate(r, evidenceSupplier.apply(r)),
                (a, b) -> a));
    }
}
