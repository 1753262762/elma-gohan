package com.elma.gohan.domain.risk;

import com.elma.gohan.provider.evidence.ReviewEvidence;
import java.util.List;

public interface ReviewBurstDetector {
    BurstDetectionResult detect(List<ReviewEvidence> reviews);
}
