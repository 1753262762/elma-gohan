package com.elma.gohan.infrastructure.persistence;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RestaurantDeepEvidenceRepository
        extends JpaRepository<RestaurantDeepEvidenceEntity, UUID> {
    Optional<RestaurantDeepEvidenceEntity> findByRestaurantIdAndSource(
            UUID restaurantId, String source);
}
