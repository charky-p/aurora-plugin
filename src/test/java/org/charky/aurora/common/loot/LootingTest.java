package org.charky.aurora.common.loot;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LootingTest {
    @Test
    void noLootingKeepsTheRange() {
        assertEquals(new Looting.Range(1, 3), Looting.apply(1, 3, 0));
        assertEquals(new Looting.Range(1, 3), Looting.apply(1, 3, -2));
    }

    @Test
    void eachLevelShiftsTheWholeRangeUpByOne() {
        assertEquals(new Looting.Range(2, 4), Looting.apply(1, 3, 1));
        assertEquals(new Looting.Range(6, 8), Looting.apply(1, 3, 5));
    }
}
