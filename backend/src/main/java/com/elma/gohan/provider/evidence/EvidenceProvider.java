package com.elma.gohan.provider.evidence;

import com.elma.gohan.domain.restaurant.Restaurant;

/**
 * 多平台评论证据扩展点。实现必须先映射为统一 RestaurantEvidence。
 */
public interface EvidenceProvider {

    RestaurantEvidence getEvidence(Restaurant restaurant);
}
