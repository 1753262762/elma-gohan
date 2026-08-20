package com.elma.gohan.provider.evidence;

import static org.assertj.core.api.Assertions.assertThat;

import com.elma.gohan.TestRestaurants;
import com.elma.gohan.config.EntityResolutionProperties;
import com.elma.gohan.domain.restaurant.BusinessStatus;
import com.elma.gohan.domain.restaurant.DataCompleteness;
import com.elma.gohan.domain.restaurant.Restaurant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class EntityResolverTest {

    private final EntityResolver resolver = new EntityResolver(new EntityResolutionProperties());

    @Test
    void normalizesStoreSuffixAndMatchesNearbySameAddress() {
        Restaurant restaurant = TestRestaurants.full("a1", "湘味小馆（旗舰店）", 4.6, 50, 45);
        PlatformEvidence baidu = evidence("b1", "湘味小馆", "麓山南路 1 号",
                28.2291, 112.9412, null);

        EntityMatchResult result = resolver.resolve(List.of(restaurant), List.of(baidu), Set.of())
                .get("a1");

        assertThat(result.status()).isEqualTo(EntityMatchStatus.MATCHED);
        assertThat(result.confidence()).isGreaterThanOrEqualTo(0.78);
        assertThat(result.evidence().providerPoiId()).isEqualTo("b1");
    }

    @Test
    void closeDuplicateCandidatesAreMarkedAmbiguous() {
        Restaurant restaurant = TestRestaurants.full("a1", "湘味小馆", 4.6, 50, 45);
        PlatformEvidence first = evidence("b1", "湘味小馆", "麓山南路 1 号",
                28.2291, 112.9412, null);
        PlatformEvidence second = evidence("b2", "湘味小馆", "麓山南路 1 号",
                28.2291, 112.9412, null);

        EntityMatchResult result = resolver.resolve(List.of(restaurant),
                List.of(first, second), Set.of()).get("a1");

        assertThat(result.status()).isEqualTo(EntityMatchStatus.AMBIGUOUS);
        assertThat(result.evidence()).isNull();
    }

    @Test
    void sameBaiduUidCannotBindTwoAmapRestaurants() {
        Restaurant first = TestRestaurants.full("a1", "湘味小馆", 4.6, 50, 45);
        Restaurant second = TestRestaurants.full("a2", "湘味小馆", 4.6, 55, 45);
        PlatformEvidence baidu = evidence("b1", "湘味小馆", "麓山南路 1 号",
                28.2291, 112.9412, null);

        Map<String, EntityMatchResult> result = resolver.resolve(List.of(first, second),
                List.of(baidu), Set.of());

        assertThat(result.values()).filteredOn(value -> value.status() == EntityMatchStatus.MATCHED)
                .hasSize(1);
        assertThat(result.values()).filteredOn(value -> value.status() == EntityMatchStatus.NO_MATCH)
                .hasSize(1);
    }

    @Test
    void farPoiIsRejectedUnlessTelephoneAndNameAgree() {
        Restaurant restaurant = new Restaurant(null, "AMAP", "a1", "湘味小馆",
                28.2291, 112.9412, 50, "CHINESE", "中餐厅", 4.6, 100, 45,
                BusinessStatus.UNKNOWN, "09:00-21:00", "麓山南路 1 号",
                "0731-12345678", DataCompleteness.FULL);
        PlatformEvidence withoutPhone = evidence("b1", "湘味小馆", "其他地址",
                28.2391, 112.9512, null);
        PlatformEvidence withPhone = evidence("b2", "湘味小馆", "其他地址",
                28.2391, 112.9512, "0731 12345678");

        assertThat(resolver.resolve(List.of(restaurant), List.of(withoutPhone), Set.of())
                .get("a1").status()).isEqualTo(EntityMatchStatus.NO_MATCH);
        assertThat(resolver.resolve(List.of(restaurant), List.of(withPhone), Set.of())
                .get("a1").status()).isEqualTo(EntityMatchStatus.MATCHED);
    }

    private static PlatformEvidence evidence(String id, String name, String address,
                                              double latitude, double longitude,
                                              String telephone) {
        return new PlatformEvidence("BAIDU", id, EvidenceStatus.AVAILABLE, null,
                name, address, latitude, longitude, 4.5, null, null, null,
                100, 45, "09:00-21:00", null, telephone);
    }
}
