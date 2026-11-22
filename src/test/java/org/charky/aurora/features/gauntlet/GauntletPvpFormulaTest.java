package org.charky.aurora.features.gauntlet;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GauntletPvpFormulaTest {
    // Gear at level L: Sharpness 2L, Defence L a piece (4L a set), Protection 2L
    private static double equalGear(int level) {
        return GauntletPvpFormula.hearts(2 * level, GauntletPvpFormula.defence(4 * level, 0), 2 * level);
    }

    @Test
    void equalGearKillsInAFewHitsAtEveryLevel() {
        for (int level : new int[]{0, 5, 10, 20, 30}) {
            double hits = 10 / equalGear(level);
            assertTrue(hits >= 4.5 && hits <= 9, "level " + level + " takes " + hits + " hits");
        }
    }

    @Test
    void betterGearHitsHarderButNeverOneShots() {
        double even = equalGear(10);
        double ahead = GauntletPvpFormula.hearts(40, GauntletPvpFormula.defence(40, 0), 20);
        assertTrue(ahead > even);
        assertTrue(ahead < 10, "a hit ten levels ahead still takes more than one");
    }

    @Test
    void theGapIsHeld() {
        assertEquals(GauntletPvpFormula.BASE_HEARTS * GauntletPvpFormula.MAX_GAP, GauntletPvpFormula.hearts(1000, 0, 0), 1e-9);
        assertEquals(GauntletPvpFormula.BASE_HEARTS * GauntletPvpFormula.MIN_GAP, GauntletPvpFormula.hearts(0, 1000, 0), 1e-9);
    }
}
