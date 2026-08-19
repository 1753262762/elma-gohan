package com.elma.gohan.domain.recommendation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import com.elma.gohan.TestRestaurants;
import com.elma.gohan.config.TasteProperties;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class TasteProfileTest {

    private final TasteProperties properties = new TasteProperties();
    private final LocalDateTime now = LocalDateTime.of(2026, 8, 19, 12, 0);

    @Test
    void likeNormalAndDislikeHaveDifferentWeights() {
        var restaurant = TestRestaurants.full("p1", 4.5, 800, 50);
        TasteProfile like = TasteProfile.empty().apply(restaurant, 800, "LIKE", now, properties);
        TasteProfile normal = TasteProfile.empty().apply(restaurant, 800, "NORMAL", now, properties);
        TasteProfile dislike = TasteProfile.empty().apply(restaurant, 800, "DISLIKE", now, properties);

        assertThat(like.categoryWeight(restaurant)).isEqualTo(1.0);
        assertThat(normal.categoryWeight(restaurant)).isEqualTo(0.1);
        assertThat(dislike.categoryWeight(restaurant)).isEqualTo(-1.0);
        assertThat(like.priceBandWeights()).containsEntry("P1", 1.0);
        assertThat(like.distanceBandWeights()).containsEntry("D1", 1.0);
    }

    @Test
    void priorWeightsDecayBeforeNewFeedback() {
        var restaurant = TestRestaurants.full("p1", 4.5, 800, 50);
        TasteProfile profile = TasteProfile.empty().apply(restaurant, 800, "LIKE", now, properties)
                .apply(restaurant, 800, "LIKE", now.plusDays(1), properties);
        assertThat(profile.categoryWeight(restaurant)).isCloseTo(1.95, within(0.0001));
        assertThat(profile.feedbackCount()).isEqualTo(2);
    }

    @Test
    void weightsAreCappedAtConfiguredBounds() {
        var restaurant = TestRestaurants.full("p1", 4.5, 800, 50);
        TasteProfile profile = TasteProfile.empty();
        for (int i = 0; i < 20; i++) {
            profile = profile.apply(restaurant, 800, "LIKE", now.plusDays(i), properties);
        }
        assertThat(profile.categoryWeight(restaurant)).isEqualTo(3.0);
        assertThat(profile.priceWeight(restaurant, properties)).isEqualTo(3.0);
        assertThat(profile.distanceWeight(restaurant, properties)).isEqualTo(3.0);
    }
}
