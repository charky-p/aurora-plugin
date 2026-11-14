package org.charky.aurora.features.perk;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PerkTest {
    // Half a heart a level on top of ten hearts, so twenty levels is double
    @Test
    void vitalityDoublesHealthAtItsLastLevel() {
        assertEquals(20, Perk.VITALITY.maxLevel());
    }

    @Test
    void everyLevelCostsMoreThanTheLast() {
        assertEquals(50, Perk.VITALITY.cost(1));
        assertEquals(1_000, Perk.VITALITY.cost(20));
        for (int level = 1; level < Perk.VITALITY.maxLevel(); level++) {
            assertTrue(Perk.VITALITY.cost(level + 1) > Perk.VITALITY.cost(level));
        }
    }

    // A first run pays a few hundred Essence, so the full perk is a long way off
    @Test
    void theWholePerkIsExpensive() {
        assertEquals(0, Perk.VITALITY.totalCost(0));
        assertEquals(150, Perk.VITALITY.totalCost(2));
        assertEquals(10_500, Perk.VITALITY.totalCost(20));
    }

    @Test
    void perksAreFoundByTheirSavedId() {
        assertSame(Perk.VITALITY, Perk.byId("vitality"));
        assertSame(Perk.VITALITY, Perk.byId("VITALITY"));
        assertNull(Perk.byId("flight"));
    }
}
