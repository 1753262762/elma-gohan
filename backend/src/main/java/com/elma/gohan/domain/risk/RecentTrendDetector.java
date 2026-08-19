package com.elma.gohan.domain.risk;

import com.elma.gohan.provider.evidence.ReviewEvidence;
import java.util.List;

public interface RecentTrendDetector {
    RecentTrend detect(List<ReviewEvidence> reviews);
}
