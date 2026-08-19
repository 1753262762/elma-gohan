package com.elma.gohan.infrastructure.persistence;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RecommendationCandidateRepository
        extends JpaRepository<RecommendationCandidateEntity, UUID> {

    List<RecommendationCandidateEntity> findByRecommendationLogIdOrderBySlotAsc(UUID recommendationLogId);
}
