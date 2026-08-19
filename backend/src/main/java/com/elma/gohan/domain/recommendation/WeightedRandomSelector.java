package com.elma.gohan.domain.recommendation;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * 加权不放回抽取:权重非负,同一 seed 结果确定(便于测试)。
 */
public class WeightedRandomSelector {

    private final Random random;

    public WeightedRandomSelector(long seed) {
        this.random = new Random(seed);
    }

    public <T> List<T> select(List<T> items, List<Double> weights, int count) {
        if (items.size() != weights.size()) {
            throw new IllegalArgumentException("items and weights must align");
        }
        List<T> remaining = new ArrayList<>(items);
        List<Double> remainingWeights = new ArrayList<>(weights);
        List<T> picked = new ArrayList<>();
        int n = Math.min(count, items.size());
        for (int i = 0; i < n; i++) {
            int idx = drawIndex(remainingWeights);
            picked.add(remaining.remove(idx));
            remainingWeights.remove(idx);
        }
        return picked;
    }

    private int drawIndex(List<Double> weights) {
        double total = weights.stream().mapToDouble(Double::doubleValue).sum();
        if (total <= 0) {
            return 0;
        }
        double point = random.nextDouble() * total;
        double acc = 0;
        for (int i = 0; i < weights.size(); i++) {
            acc += weights.get(i);
            if (point < acc) {
                return i;
            }
        }
        return weights.size() - 1;
    }
}
