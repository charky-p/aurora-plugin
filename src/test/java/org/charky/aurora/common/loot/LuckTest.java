package org.charky.aurora.common.loot;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LuckTest {
    // A tenth of the chance a level, none below 0, never past certain
    @ParameterizedTest
    @CsvSource({"2, 0, 2", "2, -3, 2", "2, 1, 2.2", "2, 10, 4", "15, 15, 37.5", "80, 10, 100"})
    void everyLevelAddsATenthOfTheChance(double chance, int level, double expected) {
        assertEquals(expected, Luck.chance(chance, level), 1e-9);
    }
}
