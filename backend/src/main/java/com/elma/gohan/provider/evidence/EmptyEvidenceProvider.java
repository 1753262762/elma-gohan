package com.elma.gohan.provider.evidence;

import com.elma.gohan.domain.restaurant.Restaurant;
import org.springframework.stereotype.Component;

@Component
public class EmptyEvidenceProvider implements EvidenceProvider {

    @Override
    public RestaurantEvidence getEvidence(Restaurant restaurant) {
        return RestaurantEvidence.empty();
    }
}
