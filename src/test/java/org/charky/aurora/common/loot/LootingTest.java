package org.charky.aurora.common.loot;

import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LootingTest {
    // Below the density nothing drops, at it the amount, past it the Fortune curve:
    // a common drop by the multiplier, a rare one by its cube root (8 is 23x, about 2.84)
    @Test
    void lootingAgainstTheDensity() {
        Random random = new Random(3);
        assertEquals(0, Looting.apply(3, -1, false, random));
        assertEquals(0, Looting.apply(3, -2, true, random));
        assertEquals(3, Looting.apply(3, 0, false, random));
        assertEquals(3, Looting.apply(3, 0, true, random));
        assertEquals(69, Looting.apply(3, 8, false, random));
        for (int i = 0; i < 100; i++) {
            int rare = Looting.apply(1, 8, true, random);
            assertTrue(rare == 2 || rare == 3, "1 became " + rare);
        }
    }
}
