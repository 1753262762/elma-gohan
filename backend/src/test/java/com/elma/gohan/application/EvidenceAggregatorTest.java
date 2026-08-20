package com.elma.gohan.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.elma.gohan.TestRestaurants;
import com.elma.gohan.config.EntityResolutionProperties;
import com.elma.gohan.config.RiskProperties;
import com.elma.gohan.domain.restaurant.Location;
import com.elma.gohan.domain.restaurant.Restaurant;
import com.elma.gohan.domain.risk.CrossPlatformConsistencyAnalyzer;
import com.elma.gohan.infrastructure.persistence.ExternalEntityMappingEntity;
import com.elma.gohan.infrastructure.persistence.ExternalEntityMappingRepository;
import com.elma.gohan.provider.evidence.AmapEvidenceAdapter;
import com.elma.gohan.provider.evidence.EntityMatchStatus;
import com.elma.gohan.provider.evidence.EntityResolver;
import com.elma.gohan.provider.evidence.EvidenceBundle;
import com.elma.gohan.provider.evidence.EvidenceStatus;
import com.elma.gohan.provider.evidence.PlatformEvidence;
import com.elma.gohan.provider.evidence.PlatformEvidenceProvider;
import com.elma.gohan.provider.evidence.PlatformSearchResult;
import com.elma.gohan.provider.evidence.RestaurantEvidence;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class EvidenceAggregatorTest {

    private final EntityResolutionProperties entityProperties = new EntityResolutionProperties();
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Test
    void oneRecommendationUsesAtMostV3AndV2AndMergesFineRatings() {
        CountingProvider provider = new CountingProvider(
                new PlatformSearchResult(EvidenceStatus.AVAILABLE,
                        List.of(baidu("b1", null))),
                new PlatformSearchResult(EvidenceStatus.AVAILABLE,
                        List.of(baidu("b1", 4.0))));
        ExternalEntityMappingRepository repository = emptyRepository();
        EvidenceAggregator aggregator = aggregator(provider, repository);
        Restaurant restaurant = TestRestaurants.full("a1", "湘味小馆", 4.9, 100, 58);

        Map<String, EvidenceBundle> result = aggregator.collect(List.of(restaurant),
                new Location(28.2291, 112.9412), 1200, 58);

        assertThat(provider.v3Calls).isEqualTo(1);
        assertThat(provider.v2Calls).isEqualTo(1);
        assertThat(result.get("a1").entityMatch().status()).isEqualTo(EntityMatchStatus.MATCHED);
        assertThat(result.get("a1").baidu().tasteRating()).isEqualTo(4.0);
        verify(repository).save(any(ExternalEntityMappingEntity.class));
    }

    @Test
    void unavailableBaiduKeepsAmapCandidateAndSkipsV2() {
        CountingProvider provider = new CountingProvider(PlatformSearchResult.unavailable(),
                PlatformSearchResult.unavailable());
        EvidenceAggregator aggregator = aggregator(provider, emptyRepository());
        Restaurant restaurant = TestRestaurants.full("a1", "湘味小馆", 4.9, 100, 58);

        EvidenceBundle result = aggregator.collect(List.of(restaurant),
                new Location(28.2291, 112.9412), 1200, 58).get("a1");

        assertThat(provider.v3Calls).isEqualTo(1);
        assertThat(provider.v2Calls).isZero();
        assertThat(result.amap().overallRating()).isEqualTo(4.9);
        assertThat(result.entityMatch().status()).isEqualTo(EntityMatchStatus.UNAVAILABLE);
        assertThat(result.baidu().status()).isEqualTo(EvidenceStatus.UNAVAILABLE);
    }

    @Test
    void providerExceptionAlsoDegradesWithoutBreakingRecommendation() {
        PlatformEvidenceProvider throwing = new PlatformEvidenceProvider() {
            @Override
            public PlatformSearchResult searchV3(Location center, int radiusMeters) {
                throw new IllegalStateException("simulated provider failure");
            }

            @Override
            public PlatformSearchResult searchV2(Location center, int radiusMeters) {
                throw new AssertionError("V2 must not run after unavailable V3");
            }
        };

        EvidenceBundle result = aggregator(throwing, emptyRepository()).collect(
                List.of(TestRestaurants.full("a1", "湘味小馆", 4.9, 100, 58)),
                new Location(28.2291, 112.9412), 1200, 58).get("a1");

        assertThat(result.entityMatch().status()).isEqualTo(EntityMatchStatus.UNAVAILABLE);
        assertThat(result.amap().overallRating()).isEqualTo(4.9);
    }

    @Test
    void completeFreshCacheAvoidsRemoteCalls() throws Exception {
        PlatformEvidence cachedEvidence = baidu("b1", 4.0);
        ExternalEntityMappingEntity cached = new ExternalEntityMappingEntity(UUID.randomUUID(),
                "AMAP", "a1", "BAIDU", LocalDateTime.now(ZoneOffset.UTC).minusHours(1));
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        cached.refresh("b1", "MATCHED", 0.95, "{\"name\":1.0}",
                objectMapper.writeValueAsString(cachedEvidence), null,
                now.minusHours(1), null, now.plusDays(20), now.minusHours(1));
        ExternalEntityMappingRepository repository = mock(ExternalEntityMappingRepository.class);
        when(repository.findByPrimarySourceAndPrimaryPoiIdInAndEvidenceSource(
                any(), any(), any())).thenReturn(List.of(cached));
        CountingProvider provider = new CountingProvider(PlatformSearchResult.unavailable(),
                PlatformSearchResult.unavailable());

        EvidenceBundle result = aggregator(provider, repository).collect(
                List.of(TestRestaurants.full("a1", "湘味小馆", 4.9, 100, 58)),
                new Location(28.2291, 112.9412), 1200, 58).get("a1");

        assertThat(provider.v3Calls).isZero();
        assertThat(provider.v2Calls).isZero();
        assertThat(result.entityMatch().status()).isEqualTo(EntityMatchStatus.MATCHED);
        assertThat(result.baidu().tasteRating()).isEqualTo(4.0);
    }

    @Test
    void refreshedV3ReusesStillFreshV2Details() throws Exception {
        PlatformEvidence cachedV3 = baidu("b1", null);
        PlatformEvidence cachedV2 = baidu("b1", 4.0);
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        ExternalEntityMappingEntity cached = new ExternalEntityMappingEntity(UUID.randomUUID(),
                "AMAP", "a1", "BAIDU", now.minusDays(1));
        cached.refresh("b1", "MATCHED", 0.95, "{\"name\":1.0}",
                objectMapper.writeValueAsString(cachedV3),
                objectMapper.writeValueAsString(cachedV2), now.minusHours(7),
                now.minusHours(10), now.plusDays(20), now.minusHours(7));
        ExternalEntityMappingRepository repository = mock(ExternalEntityMappingRepository.class);
        when(repository.findByPrimarySourceAndPrimaryPoiIdInAndEvidenceSource(
                any(), any(), any())).thenReturn(List.of(cached));
        CountingProvider provider = new CountingProvider(
                new PlatformSearchResult(EvidenceStatus.AVAILABLE, List.of(baidu("b1", null))),
                PlatformSearchResult.unavailable());

        EvidenceBundle result = aggregator(provider, repository).collect(
                List.of(TestRestaurants.full("a1", "湘味小馆", 4.9, 100, 58)),
                new Location(28.2291, 112.9412), 1200, 58).get("a1");

        assertThat(provider.v3Calls).isEqualTo(1);
        assertThat(provider.v2Calls).isZero();
        assertThat(result.baidu().tasteRating()).isEqualTo(4.0);
        verify(repository).save(any(ExternalEntityMappingEntity.class));
    }

    private EvidenceAggregator aggregator(PlatformEvidenceProvider provider,
                                          ExternalEntityMappingRepository repository) {
        return new EvidenceAggregator(restaurant -> RestaurantEvidence.empty(), provider,
                new AmapEvidenceAdapter(), new EntityResolver(entityProperties),
                new CrossPlatformConsistencyAnalyzer(new RiskProperties()), repository,
                entityProperties, objectMapper);
    }

    private ExternalEntityMappingRepository emptyRepository() {
        ExternalEntityMappingRepository repository = mock(ExternalEntityMappingRepository.class);
        when(repository.findByPrimarySourceAndPrimaryPoiIdInAndEvidenceSource(
                any(), any(), any())).thenReturn(List.of());
        return repository;
    }

    private static PlatformEvidence baidu(String id, Double tasteRating) {
        return new PlatformEvidence("BAIDU", id, EvidenceStatus.AVAILABLE, Instant.now(),
                "湘味小馆", "麓山南路1号", 28.2291, 112.9412, 4.2,
                tasteRating, null, null, 2600, 62, "09:00-21:00", null,
                "0731-12345678");
    }

    private static final class CountingProvider implements PlatformEvidenceProvider {
        private final PlatformSearchResult v3;
        private final PlatformSearchResult v2;
        private int v3Calls;
        private int v2Calls;

        private CountingProvider(PlatformSearchResult v3, PlatformSearchResult v2) {
            this.v3 = v3;
            this.v2 = v2;
        }

        @Override
        public PlatformSearchResult searchV3(Location center, int radiusMeters) {
            v3Calls++;
            return v3;
        }

        @Override
        public PlatformSearchResult searchV2(Location center, int radiusMeters) {
            v2Calls++;
            return v2;
        }
    }
}
