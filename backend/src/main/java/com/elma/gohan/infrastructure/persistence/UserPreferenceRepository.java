package com.elma.gohan.infrastructure.persistence;

import java.util.UUID;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserPreferenceRepository extends JpaRepository<UserPreferenceEntity, UUID> {
    Optional<UserPreferenceEntity> findFirstByAnonymousUserIdOrderByCreatedAtDesc(UUID anonymousUserId);
}
