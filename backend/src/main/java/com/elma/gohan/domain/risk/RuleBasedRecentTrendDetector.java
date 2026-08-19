package com.elma.gohan.domain.risk;

import com.elma.gohan.config.RiskProperties;
import com.elma.gohan.provider.evidence.ReviewEvidence;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class RuleBasedRecentTrendDetector implements RecentTrendDetector {

    private final RiskProperties.Trend properties;
    private final Clock clock;

    @Autowired
    public RuleBasedRecentTrendDetector(RiskProperties riskProperties) {
        this(riskProperties, Clock.systemUTC());
    }

    public RuleBasedRecentTrendDetector(RiskProperties riskProperties, Clock clock) {
        this.properties = riskProperties.getTrend();
        this.clock = clock;
    }

    @Override
    public RecentTrend detect(List<ReviewEvidence> reviews) {
        Instant now = clock.instant();
        Instant recentStart = now.minus(properties.getRecentDays(), ChronoUnit.DAYS);
        Instant baselineStart = recentStart.minus(properties.getBaselineDays(), ChronoUnit.DAYS);
        List<ReviewEvidence> recent = valid(reviews).stream()
                .filter(r -> !r.createdAt().isBefore(recentStart) && !r.createdAt().isAfter(now))
                .toList();
        List<ReviewEvidence> baseline = valid(reviews).stream()
                .filter(r -> !r.createdAt().isBefore(baselineStart)
                        && r.createdAt().isBefore(recentStart))
                .toList();
        if (recent.size() < properties.getMinRecentReviews()
                || baseline.size() < properties.getMinBaselineReviews()) {
            return RecentTrend.UNKNOWN;
        }
        double recentAverage = average(recent);
        double baselineAverage = average(baseline);
        double recentNegative = negativeRatio(recent);
        double baselineNegative = negativeRatio(baseline);
        if (baselineAverage - recentAverage >= properties.getRatingDelta()
                || recentNegative - baselineNegative >= properties.getNegativeRatioDelta()) {
            return RecentTrend.DOWN;
        }
        if (recentAverage - baselineAverage >= properties.getRatingDelta()
                || baselineNegative - recentNegative >= properties.getNegativeRatioDelta()) {
            return RecentTrend.UP;
        }
        return RecentTrend.STABLE;
    }

    private List<ReviewEvidence> valid(List<ReviewEvidence> reviews) {
        return (reviews == null ? List.<ReviewEvidence>of() : reviews).stream()
                .filter(r -> r.createdAt() != null && r.rating() != null)
                .toList();
    }

    private double average(List<ReviewEvidence> reviews) {
        return reviews.stream().mapToDouble(ReviewEvidence::rating).average().orElse(0.0);
    }

    private double negativeRatio(List<ReviewEvidence> reviews) {
        long negative = reviews.stream()
                .filter(r -> r.rating() <= properties.getNegativeRatingMax()).count();
        return (double) negative / reviews.size();
    }
}
