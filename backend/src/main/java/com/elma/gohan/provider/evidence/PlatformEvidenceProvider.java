package com.elma.gohan.provider.evidence;

import com.elma.gohan.domain.restaurant.Location;

/** 批量平台 Evidence 扩展点；单次推荐不得退化为逐餐厅远程调用。 */
public interface PlatformEvidenceProvider {
    PlatformSearchResult searchV3(Location center, int radiusMeters);
    PlatformSearchResult searchV2(Location center, int radiusMeters);
}
