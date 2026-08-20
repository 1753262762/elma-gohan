package com.elma.gohan.domain.deep;

import com.elma.gohan.provider.deep.DeepEvidenceBatch;
import com.elma.gohan.provider.deep.DeepEvidenceSource;
import java.time.Instant;
import java.util.Map;

public interface DeepSignalAnalyzer {
    DeepSignalAnalysis analyze(Map<DeepEvidenceSource, DeepEvidenceBatch> evidence, Instant now);
}
