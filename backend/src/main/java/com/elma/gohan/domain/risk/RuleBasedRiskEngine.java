package com.elma.gohan.domain.risk;

import com.elma.gohan.config.RiskProperties;
import com.elma.gohan.domain.restaurant.Restaurant;
import com.elma.gohan.provider.evidence.RestaurantEvidence;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * V0.1 规则风险引擎:评分档位 + 评价数 + 信息完整度 + 价格异常,可加分的阈值全部来自 RiskProperties。
 */
@Component
public class RuleBasedRiskEngine implements RiskEngine {

    private final RiskProperties props;

    public RuleBasedRiskEngine(RiskProperties props) {
        this.props = props;
    }

    @Override
    public RiskResult evaluate(Restaurant r, RestaurantEvidence evidence) {
        int score = 0;
        List<String> reasons = new ArrayList<>();

        Double rating = r.rating();
        if (rating == null) {
            score += props.getPoints().getRatingMissing();
            reasons.add("评分数据缺失");
        } else if (rating >= props.getRating().getExcellentMin()) {
            // 满分档,不加分
        } else if (rating >= props.getRating().getGoodMin()) {
            score += props.getPoints().getRatingGood();
            reasons.add("评分良好但未达优秀");
        } else if (rating >= props.getRating().getFairMin()) {
            score += props.getPoints().getRatingFair();
            reasons.add("评分一般");
        } else {
            score += props.getPoints().getRatingPoor();
            reasons.add("评分偏低");
        }

        Integer reviewCount = r.reviewCount();
        if (reviewCount == null || reviewCount < props.getPoints().getReviewCountThreshold()) {
            score += props.getPoints().getReviewCountLow();
            reasons.add("评价数量不足");
        }

        if (r.openingHours() == null || r.openingHours().isBlank()) {
            score += props.getPoints().getOpeningHoursMissing();
            reasons.add("营业信息缺失");
        }

        if (r.averagePrice() == null) {
            score += props.getPoints().getPriceMissing();
            reasons.add("价格信息缺失");
        } else if (evidence != null && evidence.poolAveragePrice() != null
                && evidence.poolAveragePrice() > 0
                && r.averagePrice() > evidence.poolAveragePrice() * props.getPriceAnomalyRatio()) {
            score += props.getPoints().getPriceAnomaly();
            reasons.add("价格明显高于同批候选");
        }

        if (reasons.isEmpty()) {
            reasons.add("评分稳定");
            reasons.add("店铺信息完整");
        }

        return new RiskResult(
                Math.max(0, Math.min(100, score)),
                levelOf(score),
                List.copyOf(reasons),
                props.getAlgorithmVersion());
    }

    private RiskLevel levelOf(int score) {
        RiskProperties.Levels lv = props.getLevels();
        if (score <= lv.getLowMaxInclusive()) {
            return RiskLevel.LOW;
        }
        if (score <= lv.getMediumLowMaxInclusive()) {
            return RiskLevel.MEDIUM_LOW;
        }
        if (score <= lv.getMediumMaxInclusive()) {
            return RiskLevel.MEDIUM;
        }
        return RiskLevel.HIGH;
    }
}
