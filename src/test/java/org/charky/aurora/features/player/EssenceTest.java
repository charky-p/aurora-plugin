package org.charky.aurora.features.player;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EssenceTest {
    @Test
    void chanceIsATenthOfAPercentPlusATenthPerLuck() {
        assertEquals(0.001, Essence.chance(0), 1e-12);
        assertEquals(0.002, Essence.chance(1), 1e-12);
        assertEquals(0.011, Essence.chance(10), 1e-12);
    }
}
