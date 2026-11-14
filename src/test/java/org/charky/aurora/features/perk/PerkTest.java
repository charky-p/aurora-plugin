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

    // One percent a level, so the last level makes every block a crit
    @Test
    void critRateReachesEveryBlock() {
        assertEquals(100, Perk.CRIT_RATE.maxLevel());
    }

    @Test
    void theKeyringIsBoughtOnce() {
        assertEquals(1, Perk.KEYRING.maxLevel());
        assertEquals(5_000, Perk.KEYRING.cost(1));
    }

    @Test
    void everyLevelCostsMoreThanTheLast() {
        assertEquals(50, Perk.VITALITY.cost(1));
        assertEquals(1_000, Perk.VITALITY.cost(20));
        assertEquals(10, Perk.CRIT_RATE.cost(1));
        assertEquals(1_000, Perk.CRIT_RATE.cost(100));
        for (Perk perk : Perk.values()) {
            for (int level = 1; level < perk.maxLevel(); level++) {
                assertTrue(perk.cost(level + 1) > perk.cost(level), perk.id());
            }
        }
    }

    // A first run pays a few hundred Essence, so a full perk is a long way off
    @Test
    void aWholePerkIsExpensive() {
        assertEquals(0, Perk.VITALITY.totalCost(0));
        assertEquals(150, Perk.VITALITY.totalCost(2));
        assertEquals(10_500, Perk.VITALITY.totalCost(20));
        assertEquals(50_500, Perk.CRIT_RATE.totalCost(100));
    }

    @Test
    void perksAreFoundByTheirSavedId() {
        assertSame(Perk.VITALITY, Perk.byId("vitality"));
        assertSame(Perk.CRIT_RATE, Perk.byId("CRIT_RATE"));
        assertNull(Perk.byId("flight"));
    }
}
