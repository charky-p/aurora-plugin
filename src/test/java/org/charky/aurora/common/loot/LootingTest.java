package org.charky.aurora.common.loot;

import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LootingTest {
    private final Random random = new Random(3);

    @Test
    void noLootingKeepsTheAmount() {
        assertEquals(3, Looting.apply(3, 0, false, random));
        assertEquals(3, Looting.apply(3, -2, true, random));
    }

    @Test
    void commonDropsFollowTheFortuneCurve() {
        assertEquals(2, Looting.apply(1, 1, false, random));
        assertEquals(11, Looting.apply(1, 5, false, random));
        assertEquals(69, Looting.apply(3, 8, false, random));
    }

    @Test
    void rareDropsGrowWithTheCubeRoot() {
        // Looting 8 is 23x, its cube root about 2.84
        for (int i = 0; i < 1000; i++) {
            int one = Looting.apply(1, 8, true, random);
            int two = Looting.apply(2, 8, true, random);
            assertTrue(one == 2 || one == 3, "1 became " + one);
            assertTrue(two == 5 || two == 6, "2 became " + two);
        }
    }
}
