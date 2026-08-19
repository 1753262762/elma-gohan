package com.elma.gohan.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "risk_result")
public class RiskResultEntity {

    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "restaurant_id", nullable = false)
    private UUID restaurantId;

    @Column(name = "risk_score", nullable = false)
    private int riskScore;

    @Column(name = "risk_level", length = 16, nullable = false)
    private String riskLevel;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "reasons_json", columnDefinition = "jsonb", nullable = false)
    private String reasonsJson;

    @Column(name = "algorithm_version", length = 32, nullable = false)
    private String algorithmVersion;

    @Column(name = "calculated_at", nullable = false)
    private LocalDateTime calculatedAt;

    public RiskResultEntity() {
    }

    public RiskResultEntity(UUID id, UUID restaurantId, int riskScore, String riskLevel,
                            String reasonsJson, String algorithmVersion, LocalDateTime calculatedAt) {
        this.id = id;
        this.restaurantId = restaurantId;
        this.riskScore = riskScore;
        this.riskLevel = riskLevel;
        this.reasonsJson = reasonsJson;
        this.algorithmVersion = algorithmVersion;
        this.calculatedAt = calculatedAt;
    }

    public UUID getId() { return id; }
    public UUID getRestaurantId() { return restaurantId; }
    public int getRiskScore() { return riskScore; }
    public String getRiskLevel() { return riskLevel; }
    public String getReasonsJson() { return reasonsJson; }
    public String getAlgorithmVersion() { return algorithmVersion; }
    public LocalDateTime getCalculatedAt() { return calculatedAt; }
}
