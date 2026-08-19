package com.elma.gohan.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "recommendation_candidate")
public class RecommendationCandidateEntity {

    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "recommendation_log_id", nullable = false)
    private UUID recommendationLogId;

    @Column(name = "restaurant_id", nullable = false)
    private UUID restaurantId;

    @Column(name = "slot", nullable = false)
    private int slot;

    @Column(name = "distance_meters", nullable = false)
    private int distanceMeters;

    @Column(name = "risk_score", nullable = false)
    private int riskScore;

    @Column(name = "risk_level", length = 16, nullable = false)
    private String riskLevel;

    @Column(name = "risk_confidence", nullable = false)
    private double riskConfidence;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "risk_factors_json", columnDefinition = "jsonb", nullable = false)
    private String riskFactorsJson;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "risk_reasons_json", columnDefinition = "jsonb", nullable = false)
    private String riskReasonsJson;

    @Column(name = "risk_algorithm_version", length = 32, nullable = false)
    private String riskAlgorithmVersion;

    @Column(name = "low_regret_score", nullable = false)
    private double lowRegretScore;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "reasons_json", columnDefinition = "jsonb", nullable = false)
    private String reasonsJson;

    @Column(name = "shown", nullable = false)
    private boolean shown;

    public RecommendationCandidateEntity() {
    }

    public RecommendationCandidateEntity(UUID id, UUID recommendationLogId, UUID restaurantId, int slot,
                                         int distanceMeters, int riskScore, String riskLevel,
                                         double riskConfidence, String riskFactorsJson,
                                         String riskReasonsJson, String riskAlgorithmVersion,
                                         double lowRegretScore, String reasonsJson, boolean shown) {
        this.id = id;
        this.recommendationLogId = recommendationLogId;
        this.restaurantId = restaurantId;
        this.slot = slot;
        this.distanceMeters = distanceMeters;
        this.riskScore = riskScore;
        this.riskLevel = riskLevel;
        this.riskConfidence = riskConfidence;
        this.riskFactorsJson = riskFactorsJson;
        this.riskReasonsJson = riskReasonsJson;
        this.riskAlgorithmVersion = riskAlgorithmVersion;
        this.lowRegretScore = lowRegretScore;
        this.reasonsJson = reasonsJson;
        this.shown = shown;
    }

    public UUID getId() { return id; }
    public UUID getRecommendationLogId() { return recommendationLogId; }
    public UUID getRestaurantId() { return restaurantId; }
    public int getSlot() { return slot; }
    public int getDistanceMeters() { return distanceMeters; }
    public int getRiskScore() { return riskScore; }
    public String getRiskLevel() { return riskLevel; }
    public double getRiskConfidence() { return riskConfidence; }
    public String getRiskFactorsJson() { return riskFactorsJson; }
    public String getRiskReasonsJson() { return riskReasonsJson; }
    public String getRiskAlgorithmVersion() { return riskAlgorithmVersion; }
    public double getLowRegretScore() { return lowRegretScore; }
    public String getReasonsJson() { return reasonsJson; }
    public boolean isShown() { return shown; }

    public void markShown() {
        this.shown = true;
    }
}
