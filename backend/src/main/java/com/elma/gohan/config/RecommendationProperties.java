package com.elma.gohan.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * RecommendationEngine 排序权重与 Top-K / 候选池参数。
 */
@ConfigurationProperties(prefix = "elma.recommendation")
public class RecommendationProperties {

    private String algorithmVersion = "lowregret-v0.1";
    private int topK = 5;
    /** 会话候选池大小(契约上限 A/B/C = 3)。 */
    private int poolSize = 3;
    private int walkingSpeedMetersPerMinute = 80;
    private Weights weights = new Weights();

    public String getAlgorithmVersion() { return algorithmVersion; }
    public void setAlgorithmVersion(String algorithmVersion) { this.algorithmVersion = algorithmVersion; }
    public int getTopK() { return topK; }
    public void setTopK(int topK) { this.topK = topK; }
    public int getPoolSize() { return poolSize; }
    public void setPoolSize(int poolSize) { this.poolSize = poolSize; }
    public int getWalkingSpeedMetersPerMinute() { return walkingSpeedMetersPerMinute; }
    public void setWalkingSpeedMetersPerMinute(int walkingSpeedMetersPerMinute) {
        this.walkingSpeedMetersPerMinute = walkingSpeedMetersPerMinute;
    }
    public Weights getWeights() { return weights; }
    public void setWeights(Weights weights) { this.weights = weights; }

    public static class Weights {
        private double rating = 25;
        private double distance = 20;
        private double budget = 15;
        private double category = 10;
        private double completeness = 10;
        private double risk = 20;

        public double getRating() { return rating; }
        public void setRating(double rating) { this.rating = rating; }
        public double getDistance() { return distance; }
        public void setDistance(double distance) { this.distance = distance; }
        public double getBudget() { return budget; }
        public void setBudget(double budget) { this.budget = budget; }
        public double getCategory() { return category; }
        public void setCategory(double category) { this.category = category; }
        public double getCompleteness() { return completeness; }
        public void setCompleteness(double completeness) { this.completeness = completeness; }
        public double getRisk() { return risk; }
        public void setRisk(double risk) { this.risk = risk; }
    }
}
