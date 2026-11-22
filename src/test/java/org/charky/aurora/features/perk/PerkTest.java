package org.charky.aurora.features.perk;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PerkTest {
    @Test
    void everyLevelCostsMoreThanTheLast() {
        assertEquals(50, Perk.VITALITY.cost(1));
        assertEquals(1_000, Perk.VITALITY.cost(20));
        assertEquals(10, Perk.CRIT_RATE.cost(1));
        assertEquals(1_000, Perk.CRIT_RATE.cost(100));
        assertEquals(5_000, Perk.KEYRING.cost(1));
        for (Perk perk : Perk.values()) {
            for (int level = 1; level < perk.maxLevel(); level++) {
                assertTrue(perk.cost(level + 1) > perk.cost(level), perk.id());
            }
        }
    }
}
