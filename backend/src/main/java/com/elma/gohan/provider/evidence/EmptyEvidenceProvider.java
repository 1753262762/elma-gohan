package com.elma.gohan.provider.evidence;

import com.elma.gohan.domain.restaurant.Restaurant;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "elma.evidence", name = "provider", havingValue = "empty")
public class EmptyEvidenceProvider implements EvidenceProvider {

    @Override
    public RestaurantEvidence getEvidence(Restaurant restaurant) {
        return RestaurantEvidence.empty();
    }
}
