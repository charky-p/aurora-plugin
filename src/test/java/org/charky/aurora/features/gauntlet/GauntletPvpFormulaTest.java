package org.charky.aurora.features.gauntlet;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GauntletPvpFormulaTest {
    private static final double BASE = 2;
    private static final double EFFECT = 0.03;

    @Test
    void equalLevelsTakeTenSwordHits() {
        for (int level : new int[]{0, 10, 30, 50}) assertEquals(10, GauntletPvpFormula.hitsToKill(BASE, level, level, EFFECT));
    }

    // Level 25 against level 30 is near even, level 1 against level 50 loses but lasts
    @Test
    void levelsMatterButGently() {
        int lower = GauntletPvpFormula.hitsToKill(BASE, 25, 30, EFFECT);
        int higher = GauntletPvpFormula.hitsToKill(BASE, 30, 25, EFFECT);
        assertTrue(lower - higher <= 2, "25 against 30: " + lower + " hits against " + higher);
        int newcomer = GauntletPvpFormula.hitsToKill(BASE, 1, 50, EFFECT);
        int veteran = GauntletPvpFormula.hitsToKill(BASE, 50, 1, EFFECT);
        assertTrue(veteran >= 4, "level 50 needs a few hits on level 1, not one: " + veteran);
        assertTrue(newcomer <= 30, "level 1 can still hurt level 50: " + newcomer + " hits");
        assertTrue(newcomer > 3 * veteran, "but loses clearly");
    }

    @Test
    void sharesAndQualityScaleTheHit() {
        double sword = GauntletPvpFormula.damage(BASE, 1, 1, 20, 20, EFFECT);
        assertEquals(2.0, sword, 1e-9);
        assertEquals(2.4, GauntletPvpFormula.damage(BASE, 1.2, 1, 20, 20, EFFECT), 1e-9, "an axe hits harder");
        assertEquals(3.0, GauntletPvpFormula.damage(BASE, 1, 1.5, 20, 20, EFFECT), 1e-9, "a crit hits half again");
    }
}
