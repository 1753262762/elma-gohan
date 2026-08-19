package com.elma.gohan.application;

/** 条件合法但附近没有可推荐的餐厅 -> 422 NO_RECOMMENDATION_AVAILABLE。 */
public class NoRecommendationAvailableException extends RuntimeException {

    public NoRecommendationAvailableException(String message) {
        super(message);
    }
}
