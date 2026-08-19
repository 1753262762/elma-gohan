package com.elma.gohan.domain.recommendation;

import com.elma.gohan.domain.restaurant.SearchCondition;

/** 本次显式条件 + 历史反馈形成的 TasteProfile。 */
public record UserPreference(SearchCondition condition, TasteProfile tasteProfile) {
    public UserPreference {
        tasteProfile = tasteProfile == null ? TasteProfile.empty() : tasteProfile;
    }

    public UserPreference(SearchCondition condition) {
        this(condition, TasteProfile.empty());
    }
}
