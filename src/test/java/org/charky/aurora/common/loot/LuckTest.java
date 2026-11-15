package org.charky.aurora.common.loot;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LuckTest {
    @Test
    void noLuckKeepsTheChance() {
        assertEquals(2.0, Luck.chance(2.0, 0));
        assertEquals(2.0, Luck.chance(2.0, -3));
    }

    @Test
    void everyLevelAddsATenthOfTheChance() {
        assertEquals(2.2, Luck.chance(2.0, 1), 1e-9);
        assertEquals(4.0, Luck.chance(2.0, 10), 1e-9);
        assertEquals(5.0, Luck.chance(2.0, 15), 1e-9);
        // Gormak's blade
        assertEquals(37.5, Luck.chance(15.0, 15), 1e-9);
    }

    @Test
    void aChanceNeverPassesCertain() {
        assertEquals(100.0, Luck.chance(80.0, 10));
    }
}
