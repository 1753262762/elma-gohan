package com.elma.gohan.domain.deep;

import static org.assertj.core.api.Assertions.assertThat;

import com.elma.gohan.config.DeepEvidenceProperties;
import com.elma.gohan.config.RiskProperties;
import com.elma.gohan.domain.risk.RiskLevel;
import com.elma.gohan.provider.deep.DeepEvidenceBatch;
import com.elma.gohan.provider.deep.DeepEvidenceSource;
import com.elma.gohan.provider.deep.WebEvidenceItem;
import com.elma.gohan.provider.evidence.EvidenceStatus;
import java.time.Instant;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class DeepEvidenceRulesTest {

    private final DeepEvidenceProperties properties = new DeepEvidenceProperties();
    private final RuleBasedDeepSignalAnalyzer analyzer =
            new RuleBasedDeepSignalAnalyzer(properties);
    private final RuleBasedDeepRiskEngine riskEngine =
            new RuleBasedDeepRiskEngine(properties, new RiskProperties());

    @Test
    void negativeAndOperationalSignalsRaiseRiskWithinTenPoints() {
        Instant now = Instant.parse("2026-08-20T00:00:00Z");
        Map<DeepEvidenceSource, DeepEvidenceBatch> evidence = new EnumMap<>(DeepEvidenceSource.class);
        for (DeepEvidenceSource source : DeepEvidenceSource.values()) {
            evidence.put(source, batch(source, now,
                    "这家店偏咸，上菜慢，高峰期需要排队"));
        }

        DeepSignalAnalysis analysis = analyzer.analyze(evidence, now);
        DeepRiskResult result = riskEngine.evaluate(
                new BaseRiskSnapshot(55, RiskLevel.MEDIUM, 0.7,
                        List.of("基础数据稳定"), "risk-v0.3"), analysis);

        assertThat(analysis.negative()).anyMatch(value -> value.contains("偏咸"));
        assertThat(analysis.cautions()).anyMatch(value -> value.contains("排队"));
        assertThat(analysis.consistencyLevel()).isEqualTo("HIGH");
        assertThat(result.riskScore()).isBetween(56, 60);
        assertThat(result.algorithmVersion()).isEqualTo("deep-risk-v0.1");
        assertThat(result.confidence()).isBetween(0.0, 1.0);
    }

    @Test
    void negatedPositivePhraseIsNotCountedAndNoEvidenceKeepsBaseRisk() {
        Instant now = Instant.parse("2026-08-20T00:00:00Z");
        Map<DeepEvidenceSource, DeepEvidenceBatch> evidence = new EnumMap<>(DeepEvidenceSource.class);
        evidence.put(DeepEvidenceSource.BILIBILI,
                batch(DeepEvidenceSource.BILIBILI, now, "不好吃，有点踩雷"));
        evidence.put(DeepEvidenceSource.XIAOHONGSHU,
                DeepEvidenceBatch.unavailable(DeepEvidenceSource.XIAOHONGSHU, now));
        evidence.put(DeepEvidenceSource.DIANPING,
                DeepEvidenceBatch.unavailable(DeepEvidenceSource.DIANPING, now));

        DeepSignalAnalysis negative = analyzer.analyze(evidence, now);
        assertThat(negative.positive()).noneMatch(value -> value.contains("好吃"));

        Map<DeepEvidenceSource, DeepEvidenceBatch> empty = new EnumMap<>(DeepEvidenceSource.class);
        for (DeepEvidenceSource source : DeepEvidenceSource.values()) {
            empty.put(source, new DeepEvidenceBatch(source, EvidenceStatus.NO_DATA, List.of(), now));
        }
        DeepRiskResult unchanged = riskEngine.evaluate(
                new BaseRiskSnapshot(18, RiskLevel.LOW, 0.64,
                        List.of("基础判断"), "risk-v0.3"), analyzer.analyze(empty, now));
        assertThat(unchanged.riskScore()).isEqualTo(18);
        assertThat(unchanged.confidence()).isEqualTo(0.64);
        assertThat(unchanged.reasons()).contains("公开结果不足，保持基础判断");
    }

    private DeepEvidenceBatch batch(DeepEvidenceSource source, Instant now, String text) {
        return new DeepEvidenceBatch(source, EvidenceStatus.AVAILABLE,
                List.of(new WebEvidenceItem(source, text, "https://example.test/" + source,
                        text, now.minusSeconds(3600), now, 0.95, List.of())), now);
    }
}
