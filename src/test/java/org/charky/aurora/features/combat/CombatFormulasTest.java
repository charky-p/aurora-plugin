package org.charky.aurora.features.combat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;

// Expected values come from scripts/balance/balance_sim.py, so if the plugin and
// the simulator drift apart, this fails
class CombatFormulasTest {
    private static final double EPSILON = 1e-6;

    // hearts(gap) in the sim, the gap in tiers
    @ParameterizedTest
    @CsvSource({"-2, 0.263737", "-1, 0.351501", "-0.5, 0.52591", "0, 1.0", "0.5, 1.517456",
            "1, 2.0", "2, 2.877223", "3, 3.658546", "4, 4.362887", "5, 5.004064"})
    void heartsMatchTheSimulator(double gap, double hearts) {
        assertEquals(hearts, CombatFormulas.heartsPerHit(gap * CombatFormulas.LEVELS_PER_TIER, 0), EPSILON);
    }

    @Test
    void heartsRiseWithAttackAndNeverDropBelowTheFloor() {
        double previous = 0;
        for (int attack = 0; attack <= 80; attack++) {
            double hearts = CombatFormulas.heartsPerHit(attack, 40);
            assertTrue(hearts > previous, "attack " + attack);
            previous = hearts;
        }
        assertTrue(CombatFormulas.heartsPerHit(0, 1000) >= CombatFormulas.ARMOUR_FLOOR);
    }

    @ParameterizedTest
    @CsvSource({"-3, 0", "0, 0", "1, 1", "5, 5", "6, 8", "10, 20", "11, 25", "50, 920", "51, 965", "60, 1395"})
    void sharpnessMatchesTheSimulator(int level, double bonus) {
        assertEquals(bonus, CombatFormulas.sharpnessBonus(level), EPSILON);
    }

    // A share off every hit, steep at first (a quarter off by level 5), more every level
    // up to the cap and no more past it
    @Test
    void protectionTakesAShareUpToTheCap() {
        int cap = CombatFormulas.MAX_PROTECTION;
        double atCap = 1 - CombatFormulas.REDUCTION_AT_MAX;
        assertEquals(1.0, CombatFormulas.protectionMultiplier(0), EPSILON);
        assertEquals(1.0, CombatFormulas.protectionMultiplier(-5), EPSILON);
        assertEquals(0.73, CombatFormulas.protectionMultiplier(5), 0.005);
        assertEquals(atCap, CombatFormulas.protectionMultiplier(cap), EPSILON);
        assertEquals(atCap, CombatFormulas.protectionMultiplier(cap * 2.5), EPSILON);
        double previous = 1.0;
        for (int level = 1; level <= cap; level++) {
            double multiplier = CombatFormulas.protectionMultiplier(level);
            assertTrue(multiplier < previous, "level " + level);
            previous = multiplier;
        }
    }

    @Test
    void chargeRunsFromTwentyPercentToFull() {
        assertEquals(0.2, CombatFormulas.chargeMultiplier(0), EPSILON);
        assertEquals(0.4, CombatFormulas.chargeMultiplier(625 / 2), 0.01);
        assertEquals(1.0, CombatFormulas.chargeMultiplier(625), EPSILON);
        assertEquals(1.0, CombatFormulas.chargeMultiplier(5000), EPSILON);
    }

    // Shields: Abandoned City 11 (deflection 0), Dunes 15 (3), Canyons 19 (6), Sun Temple 23 (9).
    // A zone's shield stops its own mobs, one zone up lets part through, two zones up breaks it
    @Test
    void shieldOfTheZoneStopsItsOwnMobs() {
        assertEquals(0.0, CombatFormulas.deflectionGap(15, 3), EPSILON);
        assertEquals(-1.0, CombatFormulas.deflectionGap(11, 3), EPSILON);
        assertEquals(0.0, CombatFormulas.throughShield(CombatFormulas.deflectionGap(11, 0)), EPSILON);
        assertEquals(0.0, CombatFormulas.throughShield(CombatFormulas.deflectionGap(23, 9)), EPSILON);

        double oneUp = CombatFormulas.throughShield(CombatFormulas.deflectionGap(15, 0));
        assertTrue(oneUp > 0 && oneUp < 1, "got " + oneUp);
        assertFalse(CombatFormulas.breaksShield(CombatFormulas.deflectionGap(15, 0)));

        assertTrue(CombatFormulas.breaksShield(CombatFormulas.deflectionGap(19, 0)));
        assertEquals(1.0, CombatFormulas.throughShield(CombatFormulas.deflectionGap(19, 0)), EPSILON);
    }

    @Test
    void moreGetsThroughTheHigherTheAttack() {
        double previous = -1;
        for (int attack = 11; attack <= 30; attack++) {
            double through = CombatFormulas.throughShield(CombatFormulas.deflectionGap(attack, 3));
            assertTrue(through >= previous && through <= 1, "attack " + attack);
            previous = through;
        }
    }
}
