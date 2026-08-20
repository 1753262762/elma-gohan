package com.elma.gohan.domain.risk;

import static org.assertj.core.api.Assertions.assertThat;

import com.elma.gohan.TestRestaurants;
import com.elma.gohan.config.RiskProperties;
import com.elma.gohan.provider.evidence.ConsistencyLevel;
import com.elma.gohan.provider.evidence.CrossPlatformConsistency;
import com.elma.gohan.provider.evidence.EntityMatchResult;
import com.elma.gohan.provider.evidence.EntityMatchStatus;
import com.elma.gohan.provider.evidence.EvidenceBundle;
import com.elma.gohan.provider.evidence.EvidenceStatus;
import com.elma.gohan.provider.evidence.PlatformEvidence;
import com.elma.gohan.provider.evidence.RestaurantEvidence;
import java.util.Map;
import org.junit.jupiter.api.Test;

class CrossPlatformConsistencyAnalyzerTest {

    private final RiskProperties properties = new RiskProperties();
    private final CrossPlatformConsistencyAnalyzer analyzer =
            new CrossPlatformConsistencyAnalyzer(properties);

    @Test
    void differencePointSevenProducesSixtyRiskInBothDirections() {
        CrossPlatformConsistency amapHigher = analyzer.analyze(evidence("AMAP", "a", 4.9),
                matched(evidence("BAIDU", "b", 4.2)));
        CrossPlatformConsistency baiduHigher = analyzer.analyze(evidence("AMAP", "a", 4.2),
                matched(evidence("BAIDU", "b", 4.9)));

        assertThat(amapHigher.level()).isEqualTo(ConsistencyLevel.CONFLICT);
        assertThat(amapHigher.ratingDifference()).isEqualTo(0.7);
        assertThat(amapHigher.crossPlatformConflictRisk()).isEqualTo(60);
        assertThat(amapHigher.reason()).contains("高德评分比百度高");
        assertThat(baiduHigher.crossPlatformConflictRisk()).isEqualTo(60);
        assertThat(baiduHigher.reason()).contains("百度评分比高德高");
    }

    @Test
    void missingRatingDoesNotInventConflictRisk() {
        CrossPlatformConsistency result = analyzer.analyze(evidence("AMAP", "a", 4.8),
                matched(evidence("BAIDU", "b", null)));

        assertThat(result.level()).isEqualTo(ConsistencyLevel.UNKNOWN);
        assertThat(result.ratingDifference()).isNull();
        assertThat(result.crossPlatformConflictRisk()).isZero();
    }

    @Test
    void conflictFactorContributesTwelvePointsAndConsensusChangesRatingRisk() {
        properties.getWeights().setRating(0.0);
        properties.getWeights().setTemplate(0.0);
        properties.getWeights().setBurst(0.0);
        properties.getWeights().setTrend(0.0);
        properties.getWeights().setDataInsufficient(0.0);
        properties.getWeights().setCrossPlatformConflict(0.2);
        RuleBasedRiskEngine engine = new RuleBasedRiskEngine(properties);
        PlatformEvidence amap = evidence("AMAP", "a", 4.9);
        PlatformEvidence baidu = evidence("BAIDU", "b", 4.2);
        EntityMatchResult match = matched(baidu);
        CrossPlatformConsistency consistency = analyzer.analyze(amap, match);

        RiskResult result = engine.evaluate(TestRestaurants.full("a", 4.9, 100),
                new EvidenceBundle(RestaurantEvidence.empty(), amap, baidu, match, consistency));

        assertThat(result.factors().crossPlatformConflictRisk()).isEqualTo(60);
        assertThat(result.riskScore()).isEqualTo(12);
        assertThat(result.evidenceSummary().ratingDifference()).isEqualTo(0.7);

        properties.getWeights().setCrossPlatformConflict(0.0);
        properties.getWeights().setRating(1.0);
        PlatformEvidence lowerBaidu = evidence("BAIDU", "b", 3.5);
        EntityMatchResult lowerMatch = matched(lowerBaidu);
        RiskResult consensus = engine.evaluate(TestRestaurants.full("a", 4.9, 100),
                new EvidenceBundle(RestaurantEvidence.empty(), amap, lowerBaidu, lowerMatch,
                        analyzer.analyze(amap, lowerMatch)));
        assertThat(consensus.factors().ratingRisk()).isEqualTo(20);
        assertThat(consensus.riskScore()).isEqualTo(20);
    }

    private static EntityMatchResult matched(PlatformEvidence evidence) {
        return new EntityMatchResult(EntityMatchStatus.MATCHED, 0.9, evidence,
                Map.of("name", 1.0));
    }

    private static PlatformEvidence evidence(String source, String id, Double rating) {
        return new PlatformEvidence(source, id, EvidenceStatus.AVAILABLE, null,
                "餐厅", "麓山南路1号", 28.2291, 112.9412, rating,
                null, null, null, 100, 50, "09:00-21:00", null, null);
    }
}
