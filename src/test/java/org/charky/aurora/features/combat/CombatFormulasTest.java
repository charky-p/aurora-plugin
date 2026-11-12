package org.charky.aurora.features.combat;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

// Expected values come from scripts/balance/balance_sim.py, so if the plugin and
// the simulator drift apart, this fails
class CombatFormulasTest {
    private static final double LEVELS_PER_TIER = 4.0;
    private static final double EPSILON = 1e-6;

    // hearts(gap) in the sim, for gaps -2, -1, -0.5, 0, 0.5, 1, 2, 3, 4, 5 tiers
    private static final double[] GAPS = {-2, -1, -0.5, 0, 0.5, 1, 2, 3, 4, 5};
    private static final double[] SIM_HEARTS = {
            0.509158, 0.567668, 0.68394, 1.0, 1.517456, 2.0, 2.877223, 3.658546, 4.362887, 5.004064};

    @Test
    void heartsMatchTheSimulator() {
        for (int i = 0; i < GAPS.length; i++) {
            double hearts = CombatFormulas.heartsPerHit(GAPS[i] * LEVELS_PER_TIER, 0);
            assertEquals(SIM_HEARTS[i], hearts, EPSILON, "gap " + GAPS[i]);
        }
    }

    @Test
    void sameTierIsOneHeart() {
        assertEquals(1.0, CombatFormulas.heartsPerHit(20, 20), EPSILON);
    }

    @Test
    void heartsNeverDropBelowHalf() {
        assertTrue(CombatFormulas.heartsPerHit(0, 1000) >= 0.5);
    }

    @Test
    void heartsRiseWithAttack() {
        double previous = 0;
        for (int attack = 0; attack <= 80; attack++) {
            double hearts = CombatFormulas.heartsPerHit(attack, 40);
            assertTrue(hearts > previous, "attack " + attack);
            previous = hearts;
        }
    }

    @Test
    void sharpnessMatchesTheSimulator() {
        int[] levels = {0, 1, 5, 6, 10, 11, 50, 51, 60};
        double[] simBonus = {0, 1, 5, 8, 20, 25, 920, 965, 1395};
        for (int i = 0; i < levels.length; i++) {
            assertEquals(simBonus[i], CombatFormulas.sharpnessBonus(levels[i]), EPSILON, "sharpness " + levels[i]);
        }
    }

    @Test
    void negativeSharpnessGivesNothing() {
        assertEquals(0.0, CombatFormulas.sharpnessBonus(-3));
    }

    @Test
    void chargeRunsFromTwentyPercentToFull() {
        assertEquals(0.2, CombatFormulas.chargeMultiplier(0), EPSILON);
        assertEquals(0.4, CombatFormulas.chargeMultiplier(625 / 2), 0.01);
        assertEquals(1.0, CombatFormulas.chargeMultiplier(625), EPSILON);
        assertEquals(1.0, CombatFormulas.chargeMultiplier(5000), EPSILON);
    }

    // Each zone's shields against each zone's mobs: Abandoned City 11, Dunes 15,
    // Canyons 19, Sun Temple 23, Royal Tomb 25
    @Test
    void shieldOfTheZoneStopsItsOwnMobs() {
        assertEquals(0.0, CombatFormulas.throughShield(CombatFormulas.deflectionGap(11, 0)), EPSILON);
        assertEquals(0.0, CombatFormulas.throughShield(CombatFormulas.deflectionGap(15, 3)), EPSILON);
        assertEquals(0.0, CombatFormulas.throughShield(CombatFormulas.deflectionGap(19, 6)), EPSILON);
        assertEquals(0.0, CombatFormulas.throughShield(CombatFormulas.deflectionGap(23, 9)), EPSILON);
    }

    @Test
    void sameTierShieldIsExactlyCovered() {
        assertEquals(0.0, CombatFormulas.deflectionGap(15, 3), EPSILON);
        assertEquals(-1.0, CombatFormulas.deflectionGap(11, 3), EPSILON);
    }

    @Test
    void oneZoneUpLetsPartOfTheHitThrough() {
        // City shield against Dunes mobs, one tier above
        double through = CombatFormulas.throughShield(CombatFormulas.deflectionGap(15, 0));
        assertTrue(through > 0 && through < 1, "got " + through);
        assertFalse(CombatFormulas.breaksShield(CombatFormulas.deflectionGap(15, 0)));
    }

    @Test
    void twoZonesUpBreaksTheShield() {
        // City shield against Canyon mobs
        assertTrue(CombatFormulas.breaksShield(CombatFormulas.deflectionGap(19, 0)));
        assertEquals(1.0, CombatFormulas.throughShield(CombatFormulas.deflectionGap(19, 0)), EPSILON);
    }

    @Test
    void moreGetsThroughTheHigherTheAttack() {
        double previous = -1;
        for (int attack = 11; attack <= 30; attack++) {
            double through = CombatFormulas.throughShield(CombatFormulas.deflectionGap(attack, 3));
            assertTrue(through >= previous, "attack " + attack);
            assertTrue(through >= 0 && through <= 1);
            previous = through;
        }
    }
}
