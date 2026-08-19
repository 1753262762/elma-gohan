package com.elma.gohan.infrastructure.persistence;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RecommendationLogRepository extends JpaRepository<RecommendationLogEntity, UUID> {

    Optional<RecommendationLogEntity> findByIdAndAnonymousUserId(UUID id, UUID anonymousUserId);
}
