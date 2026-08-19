package com.elma.gohan.domain.recommendation;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class WeightedRandomSelectorTest {

    @Test
    @DisplayName("同 seed 结果确定")
    void deterministic() {
        var a = new WeightedRandomSelector(42).select(List.of("a", "b", "c", "d"),
                List.of(1.0, 2.0, 3.0, 4.0), 3);
        var b = new WeightedRandomSelector(42).select(List.of("a", "b", "c", "d"),
                List.of(1.0, 2.0, 3.0, 4.0), 3);
        assertThat(a).isEqualTo(b);
    }

    @Test
    @DisplayName("不放回抽取,数量不超过输入")
    void withoutReplacement() {
        var picked = new WeightedRandomSelector(7).select(List.of("a", "b", "c"),
                List.of(1.0, 1.0, 1.0), 5);
        assertThat(picked).hasSize(3);
        assertThat(picked).doesNotHaveDuplicates();
    }

    @Test
    @DisplayName("全零权重也能抽取(退化为均匀)")
    void zeroWeights() {
        var picked = new WeightedRandomSelector(1).select(List.of("a", "b"), List.of(0.0, 0.0), 2);
        assertThat(picked).containsExactlyInAnyOrder("a", "b");
    }
}
