package com.elma.gohan.provider.evidence;

import static org.assertj.core.api.Assertions.assertThat;

import com.elma.gohan.TestRestaurants;
import com.elma.gohan.config.EvidenceProperties;
import com.elma.gohan.config.RiskProperties;
import com.elma.gohan.domain.risk.RuleBasedRiskEngine;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.DefaultResourceLoader;

class FileEvidenceProviderTest {

    @Test
    void mapsFileDtoToStandardEvidence() {
        EvidenceProperties properties = new EvidenceProperties();
        properties.setLocation("classpath:evidence/file-evidence.json");
        var provider = new FileEvidenceProvider(properties,
                new ObjectMapper().findAndRegisterModules(), new DefaultResourceLoader());

        var restaurant = TestRestaurants.full("POI-FILE", 4.5, 100);
        RestaurantEvidence evidence = provider.getEvidence(restaurant);
        assertThat(evidence.status()).isEqualTo(EvidenceStatus.AVAILABLE);
        assertThat(evidence.source()).isEqualTo("TEST_FILE");
        assertThat(evidence.reviews()).hasSize(1);
        assertThat(evidence.reviews().get(0).externalReviewId()).isEqualTo("R-1");

        var engine = new RuleBasedRiskEngine(new RiskProperties());
        assertThat(engine.evaluate(restaurant, evidence).riskScore())
                .isNotEqualTo(engine.evaluate(restaurant, RestaurantEvidence.empty()).riskScore());
        assertThat(engine.evaluate(restaurant, evidence).confidence())
                .isGreaterThan(engine.evaluate(restaurant, RestaurantEvidence.empty()).confidence());
    }

    @Test
    void missingRestaurantAndBrokenFileDegradeGracefully() {
        EvidenceProperties valid = new EvidenceProperties();
        valid.setLocation("classpath:evidence/file-evidence.json");
        var noMatch = new FileEvidenceProvider(valid, new ObjectMapper().findAndRegisterModules(),
                new DefaultResourceLoader());
        assertThat(noMatch.getEvidence(TestRestaurants.full("UNKNOWN", 4.5, 100)).status())
                .isEqualTo(EvidenceStatus.NO_DATA);

        EvidenceProperties broken = new EvidenceProperties();
        broken.setLocation("classpath:evidence/does-not-exist.json");
        var unavailable = new FileEvidenceProvider(broken,
                new ObjectMapper().findAndRegisterModules(), new DefaultResourceLoader());
        assertThat(unavailable.getEvidence(TestRestaurants.full("X", 4.5, 100)).status())
                .isEqualTo(EvidenceStatus.UNAVAILABLE);

        EvidenceProperties malformed = new EvidenceProperties();
        malformed.setLocation("classpath:evidence/malformed-evidence.json");
        var malformedProvider = new FileEvidenceProvider(malformed,
                new ObjectMapper().findAndRegisterModules(), new DefaultResourceLoader());
        assertThat(malformedProvider.getEvidence(TestRestaurants.full("X", 4.5, 100)).status())
                .isEqualTo(EvidenceStatus.UNAVAILABLE);
    }
}
