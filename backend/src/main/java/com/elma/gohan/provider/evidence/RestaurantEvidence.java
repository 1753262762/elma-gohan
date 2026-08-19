package com.elma.gohan.provider.evidence;

/**
 * 多平台评论证据。V0.1 为空;poolAveragePrice 由编排层注入(候选池人均均价,
 * 供 RiskEngine 价格异常规则使用),不属于外部证据。
 */
public record RestaurantEvidence(Double poolAveragePrice) {

    public static RestaurantEvidence empty() {
        return new RestaurantEvidence(null);
    }

    public RestaurantEvidence withPoolAveragePrice(double avg) {
        return new RestaurantEvidence(avg);
    }
}
