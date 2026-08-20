package com.elma.gohan.provider.deep;

import com.elma.gohan.domain.restaurant.Restaurant;

public interface DeepEvidenceProvider {
    DeepEvidenceBatch fetch(DeepEvidenceSource source, Restaurant restaurant);
}
