package org.charky.aurora.features.fishing;

import org.charky.aurora.core.models.FishingSpotModel.FishModel;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

// The numbers scripts/balance/fishing_prices.py prices the rod from
class FishingRulesTest {
    private static FishModel fish(String id, int score, int density) {
        FishModel fish = new FishModel();
        fish.id = id;
        fish.score = score;
        fish.density = density;
        return fish;
    }

    private static final List<FishModel> POND = List.of(
            fish("weed", 0, 0), fish("perch", 20, 0), fish("trout", 60, 1), fish("carp", 95, 2));

    @Test
    void theHighestScoreNotAboveTheRollBites() {
        assertEquals("weed", FishingRules.bite(POND, 1, 0).id);
        assertEquals("weed", FishingRules.bite(POND, 19, 0).id);
        assertEquals("perch", FishingRules.bite(POND, 20, 0).id);
        assertEquals("trout", FishingRules.bite(POND, 94, 0).id);
        assertEquals("carp", FishingRules.bite(POND, 95, 0).id);
    }

    // Two score a Luck level, as LUCK_SCORE in the script
    @Test
    void luckRaisesTheRoll() {
        assertEquals("perch", FishingRules.bite(POND, 10, 5).id);
        assertEquals("carp", FishingRules.bite(POND, 85, 5).id);
    }

    @Test
    void aFishBelowTheRodsFortuneGetsAway() {
        assertEquals(0, FishingRules.amount(POND.get(3), 1, 1));
        assertEquals(1, FishingRules.amount(POND.get(3), 1, 2));
        // Past the density, the block Fortune curve: 1, 2, 3.5 ...
        assertEquals(4, FishingRules.amount(POND.get(1), 2, 1));
    }

    @Test
    void lureShortensTheWaitDownToAFloor() {
        assertArrayEquals(new int[]{100, 400}, FishingRules.biteRange(0));
        assertArrayEquals(new int[]{82, 325}, FishingRules.biteRange(3));
        assertArrayEquals(new int[]{28, 100}, FishingRules.biteRange(12));
        assertArrayEquals(new int[]{20, 60}, FishingRules.biteRange(40));
    }
}
