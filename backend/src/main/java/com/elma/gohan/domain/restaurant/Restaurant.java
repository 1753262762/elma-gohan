package com.elma.gohan.domain.restaurant;

import java.util.UUID;

/**
 * 内部标准餐厅模型:第三方数据必须先转成本模型才能进入业务核心。
 * rating/reviewCount/averagePrice/openingHours 为 null 表示上游缺失。
 * id 在持久化时分配(新建或命中已有 source+sourcePoiId);来自 Provider 时可为 null。
 */
public record Restaurant(
        UUID id,
        String source,
        String sourcePoiId,
        String name,
        double latitude,
        double longitude,
        int distanceMeters,
        String categoryCode,
        String categoryLabel,
        Double rating,
        Integer reviewCount,
        Integer averagePrice,
        BusinessStatus businessStatus,
        String openingHours,
        String address,
        String telephone,
        DataCompleteness dataCompleteness
) {
    /** V0.2 兼容构造器；旧测试夹具和 File Evidence 不需要提供电话。 */
    public Restaurant(UUID id, String source, String sourcePoiId, String name,
                      double latitude, double longitude, int distanceMeters,
                      String categoryCode, String categoryLabel, Double rating,
                      Integer reviewCount, Integer averagePrice, BusinessStatus businessStatus,
                      String openingHours, String address, DataCompleteness dataCompleteness) {
        this(id, source, sourcePoiId, name, latitude, longitude, distanceMeters,
                categoryCode, categoryLabel, rating, reviewCount, averagePrice, businessStatus,
                openingHours, address, null, dataCompleteness);
    }

    public Restaurant withId(UUID newId) {
        return new Restaurant(newId, source, sourcePoiId, name, latitude, longitude, distanceMeters,
                categoryCode, categoryLabel, rating, reviewCount, averagePrice, businessStatus,
                openingHours, address, telephone, dataCompleteness);
    }

    public Restaurant withDistance(int newDistance) {
        return new Restaurant(id, source, sourcePoiId, name, latitude, longitude, newDistance,
                categoryCode, categoryLabel, rating, reviewCount, averagePrice, businessStatus,
                openingHours, address, telephone, dataCompleteness);
    }

    public Restaurant withRating(Double newRating) {
        return new Restaurant(id, source, sourcePoiId, name, latitude, longitude, distanceMeters,
                categoryCode, categoryLabel, newRating, reviewCount, averagePrice, businessStatus,
                openingHours, address, telephone, dataCompleteness);
    }
}
