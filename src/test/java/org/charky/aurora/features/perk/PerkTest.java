package org.charky.aurora.features.perk;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class PerkTest {
    @Test
    void everyLevelCostsMoreThanTheLast() {
        for (Perk perk : Perk.values()) {
            assertTrue(perk.cost(1) > 0, perk.id());
            for (int level = 1; level < perk.maxLevel(); level++) {
                assertTrue(perk.cost(level + 1) > perk.cost(level), perk.id());
            }
        }
    }
}
