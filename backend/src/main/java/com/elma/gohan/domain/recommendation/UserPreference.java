package com.elma.gohan.domain.recommendation;

import com.elma.gohan.domain.restaurant.SearchCondition;

/** V0.1 的用户偏好即本次请求条件(无画像)。 */
public record UserPreference(SearchCondition condition) {
}
