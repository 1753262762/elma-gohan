package com.elma.gohan.application;

/** 推荐会话不存在或不属于当前匿名用户 -> 404 RECOMMENDATION_NOT_FOUND。 */
public class RecommendationNotFoundException extends RuntimeException {

    public RecommendationNotFoundException(String message) {
        super(message);
    }
}
