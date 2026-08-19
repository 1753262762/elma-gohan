package com.elma.gohan.controller.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;

/** 严格对齐 contracts/openapi.yaml 的 CreateRecommendationRequest。 */
public record CreateRecommendationRequest(
        @NotNull(message = "必填")
        @Min(value = -90, message = "必须在 -90 到 90 之间")
        @Max(value = 90, message = "必须在 -90 到 90 之间")
        Double latitude,

        @NotNull(message = "必填")
        @Min(value = -180, message = "必须在 -180 到 180 之间")
        @Max(value = 180, message = "必须在 -180 到 180 之间")
        Double longitude,

        @Min(value = 500, message = "只能是 500、1000、2000 或 3000")
        @Max(value = 3000, message = "只能是 500、1000、2000 或 3000")
        Integer radius,

        @Min(value = 1, message = "必须大于等于 1")
        @Max(value = 10000, message = "必须小于等于 10000")
        Integer maxBudget,

        @Pattern(regexp = "^[A-Z][A-Z0-9_]{0,31}$", message = "必须是大写品类代码")
        String category,

        @Size(max = 10, message = "最多 10 个")
        List<@NotBlank(message = "不能为空")
        @Size(max = 30, message = "长度不能超过 30") String> dislikes
) {
    public CreateRecommendationRequest {
        if (dislikes == null) {
            dislikes = List.of();
        }
    }

    @JsonProperty("maxBudget")
    public Integer maxBudget() {
        return maxBudget;
    }
}
