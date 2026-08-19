package com.elma.gohan.provider.evidence;

import com.elma.gohan.domain.restaurant.Restaurant;

/**
 * 多平台评论证据扩展点。V0.1 只有 EmptyEvidenceProvider。
 */
public interface EvidenceProvider {

    RestaurantEvidence getEvidence(Restaurant restaurant);
}
