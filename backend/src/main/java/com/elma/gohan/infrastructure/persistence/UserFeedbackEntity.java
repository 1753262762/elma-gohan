package com.elma.gohan.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "user_feedback")
public class UserFeedbackEntity {

    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "recommendation_log_id", nullable = false)
    private UUID recommendationLogId;

    @Column(name = "restaurant_id", nullable = false)
    private UUID restaurantId;

    @Column(name = "anonymous_user_id", nullable = false)
    private UUID anonymousUserId;

    @Column(name = "result", length = 16, nullable = false)
    private String result;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public UserFeedbackEntity() {
    }

    public UserFeedbackEntity(UUID id, UUID recommendationLogId, UUID restaurantId,
                              UUID anonymousUserId, String result, LocalDateTime createdAt) {
        this.id = id;
        this.recommendationLogId = recommendationLogId;
        this.restaurantId = restaurantId;
        this.anonymousUserId = anonymousUserId;
        this.result = result;
        this.createdAt = createdAt;
    }

    public UUID getId() { return id; }
    public UUID getRecommendationLogId() { return recommendationLogId; }
    public UUID getRestaurantId() { return restaurantId; }
    public UUID getAnonymousUserId() { return anonymousUserId; }
    public String getResult() { return result; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
