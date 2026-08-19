package com.elma.gohan.config;

import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "elma.taste")
public class TasteProperties {

    private double decay = 0.95;
    private double maxAbsoluteWeight = 3.0;
    private Feedback feedback = new Feedback();
    private List<Integer> priceBandUpperBounds = new ArrayList<>(List.of(30, 60, 100));
    private List<Integer> distanceBandUpperBounds = new ArrayList<>(List.of(500, 1000, 2000));

    public double getDecay() { return decay; }
    public void setDecay(double v) { decay = v; }
    public double getMaxAbsoluteWeight() { return maxAbsoluteWeight; }
    public void setMaxAbsoluteWeight(double v) { maxAbsoluteWeight = v; }
    public Feedback getFeedback() { return feedback; }
    public void setFeedback(Feedback v) { feedback = v; }
    public List<Integer> getPriceBandUpperBounds() { return priceBandUpperBounds; }
    public void setPriceBandUpperBounds(List<Integer> v) { priceBandUpperBounds = v; }
    public List<Integer> getDistanceBandUpperBounds() { return distanceBandUpperBounds; }
    public void setDistanceBandUpperBounds(List<Integer> v) { distanceBandUpperBounds = v; }

    public static class Feedback {
        private double like = 1.0;
        private double normal = 0.1;
        private double dislike = -1.0;
        public double getLike() { return like; }
        public void setLike(double v) { like = v; }
        public double getNormal() { return normal; }
        public void setNormal(double v) { normal = v; }
        public double getDislike() { return dislike; }
        public void setDislike(double v) { dislike = v; }
    }
}
