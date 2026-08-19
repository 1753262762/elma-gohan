package com.elma.gohan;

import com.elma.gohan.domain.restaurant.BusinessStatus;
import com.elma.gohan.domain.restaurant.DataCompleteness;
import com.elma.gohan.domain.restaurant.Restaurant;

/** 测试用餐厅工厂。 */
public final class TestRestaurants {

    private TestRestaurants() {
    }

    /** 数据齐全的优质餐厅(rating/评价数/价格/营业信息全有)。 */
    public static Restaurant full(String poiId, String name, double rating, int distance,
                                  Integer averagePrice) {
        return new Restaurant(null, "AMAP", poiId, name, 28.2291, 112.9412, distance,
                "CHINESE", "中餐厅", rating, 100, averagePrice,
                BusinessStatus.UNKNOWN, "09:00-21:00", "麓山南路 1 号", DataCompleteness.FULL);
    }

    public static Restaurant full(String poiId, double rating, int distance) {
        return full(poiId, "餐厅" + poiId, rating, distance, 30);
    }

    public static Restaurant full(String poiId, double rating, int distance, Integer averagePrice) {
        return full(poiId, "餐厅" + poiId, rating, distance, averagePrice);
    }
}
