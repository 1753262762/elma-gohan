package com.elma.gohan.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * RiskEngine 规则阈值。所有数字阈值集中于此,禁止散落在规则代码中。
 */
@ConfigurationProperties(prefix = "elma.risk")
public class RiskProperties {

    private String algorithmVersion = "risk-v0.1";
    private RatingThresholds rating = new RatingThresholds();
    private Points points = new Points();
    /** 人均价高于候选池均值 × 该倍数视为价格异常。 */
    private double priceAnomalyRatio = 1.5;
    private Levels levels = new Levels();

    public String getAlgorithmVersion() { return algorithmVersion; }
    public void setAlgorithmVersion(String algorithmVersion) { this.algorithmVersion = algorithmVersion; }
    public RatingThresholds getRating() { return rating; }
    public void setRating(RatingThresholds rating) { this.rating = rating; }
    public Points getPoints() { return points; }
    public void setPoints(Points points) { this.points = points; }
    public double getPriceAnomalyRatio() { return priceAnomalyRatio; }
    public void setPriceAnomalyRatio(double priceAnomalyRatio) { this.priceAnomalyRatio = priceAnomalyRatio; }
    public Levels getLevels() { return levels; }
    public void setLevels(Levels levels) { this.levels = levels; }

    public static class RatingThresholds {
        /** < fairMin 视为差评档。 */
        private double fairMin = 4.0;
        private double goodMin = 4.2;
        private double excellentMin = 4.5;

        public double getFairMin() { return fairMin; }
        public void setFairMin(double fairMin) { this.fairMin = fairMin; }
        public double getGoodMin() { return goodMin; }
        public void setGoodMin(double goodMin) { this.goodMin = goodMin; }
        public double getExcellentMin() { return excellentMin; }
        public void setExcellentMin(double excellentMin) { this.excellentMin = excellentMin; }
    }

    public static class Points {
        private int ratingGood = 5;
        private int ratingFair = 15;
        private int ratingPoor = 30;
        private int ratingMissing = 30;
        private int reviewCountLow = 10;
        private int reviewCountThreshold = 10;
        private int openingHoursMissing = 10;
        private int priceMissing = 5;
        private int priceAnomaly = 10;

        public int getRatingGood() { return ratingGood; }
        public void setRatingGood(int ratingGood) { this.ratingGood = ratingGood; }
        public int getRatingFair() { return ratingFair; }
        public void setRatingFair(int ratingFair) { this.ratingFair = ratingFair; }
        public int getRatingPoor() { return ratingPoor; }
        public void setRatingPoor(int ratingPoor) { this.ratingPoor = ratingPoor; }
        public int getRatingMissing() { return ratingMissing; }
        public void setRatingMissing(int ratingMissing) { this.ratingMissing = ratingMissing; }
        public int getReviewCountLow() { return reviewCountLow; }
        public void setReviewCountLow(int reviewCountLow) { this.reviewCountLow = reviewCountLow; }
        public int getReviewCountThreshold() { return reviewCountThreshold; }
        public void setReviewCountThreshold(int reviewCountThreshold) { this.reviewCountThreshold = reviewCountThreshold; }
        public int getOpeningHoursMissing() { return openingHoursMissing; }
        public void setOpeningHoursMissing(int openingHoursMissing) { this.openingHoursMissing = openingHoursMissing; }
        public int getPriceMissing() { return priceMissing; }
        public void setPriceMissing(int priceMissing) { this.priceMissing = priceMissing; }
        public int getPriceAnomaly() { return priceAnomaly; }
        public void setPriceAnomaly(int priceAnomaly) { this.priceAnomaly = priceAnomaly; }
    }

    public static class Levels {
        /** 0..lowMaxInclusive -> LOW。 */
        private int lowMaxInclusive = 20;
        private int mediumLowMaxInclusive = 40;
        private int mediumMaxInclusive = 60;

        public int getLowMaxInclusive() { return lowMaxInclusive; }
        public void setLowMaxInclusive(int lowMaxInclusive) { this.lowMaxInclusive = lowMaxInclusive; }
        public int getMediumLowMaxInclusive() { return mediumLowMaxInclusive; }
        public void setMediumLowMaxInclusive(int mediumLowMaxInclusive) { this.mediumLowMaxInclusive = mediumLowMaxInclusive; }
        public int getMediumMaxInclusive() { return mediumMaxInclusive; }
        public void setMediumMaxInclusive(int mediumMaxInclusive) { this.mediumMaxInclusive = mediumMaxInclusive; }
    }
}
