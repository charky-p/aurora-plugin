package org.charky.aurora.features.gauntlet;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GauntletPvpFormulaTest {
    @Test
    void equalLevelsLeaveVanillasHit() {
        for (int level : new int[]{0, 10, 30, 50}) assertEquals(1.0, GauntletPvpFormula.levelFactor(level, level, 0.03), 1e-9);
    }

    @Test
    void levelsAheadHitHarderAndBehindSofter() {
        assertEquals(1.3, GauntletPvpFormula.levelFactor(10, 0, 0.03), 1e-9);
        assertTrue(GauntletPvpFormula.levelFactor(0, 30, 0.03) < 0.6);
    }

    // Vanilla's numbers: full diamond takes a 10 damage hit down to 3
    @Test
    void armourIsVanillas() {
        double[] diamond = GauntletPvpFormula.armour(List.of("DIAMOND_HELMET", "DIAMOND_CHESTPLATE", "DIAMOND_LEGGINGS", "DIAMOND_BOOTS"));
        assertArrayEquals(new double[]{20, 8}, diamond);
        assertEquals(0.3, GauntletPvpFormula.armourMultiplier(10, diamond[0], diamond[1]), 1e-9);
        assertEquals(1.0, GauntletPvpFormula.armourMultiplier(10, 0, 0), 1e-9, "no armour takes nothing off");
        assertArrayEquals(new double[]{0, 0}, GauntletPvpFormula.armour(List.of("ELYTRA", "AIR")));
    }
}
