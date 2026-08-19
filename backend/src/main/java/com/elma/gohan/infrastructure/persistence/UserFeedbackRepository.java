package com.elma.gohan.infrastructure.persistence;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserFeedbackRepository extends JpaRepository<UserFeedbackEntity, UUID> {
    List<UserFeedbackEntity> findByAnonymousUserIdOrderByCreatedAtAsc(UUID anonymousUserId);
}
