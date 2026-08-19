package com.elma.gohan.infrastructure.persistence;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserFeedbackRepository extends JpaRepository<UserFeedbackEntity, UUID> {
}
