package org.charky.aurora.features.tutorial;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class TutorialStepTest {
    @Test
    void stepsRunMineBackpackTraderUpgradeThenEnd() {
        assertEquals(TutorialStep.BACKPACK, TutorialStep.MINE.next());
        assertEquals(TutorialStep.TRADER, TutorialStep.BACKPACK.next());
        assertEquals(TutorialStep.UPGRADE, TutorialStep.TRADER.next());
        assertNull(TutorialStep.UPGRADE.next());
    }

    @Test
    void stepsAreNumberedFromOne() {
        assertEquals(1, TutorialStep.MINE.number());
        assertEquals(4, TutorialStep.UPGRADE.number());
    }

    @Test
    void savedNamesReadBackAndUnknownOnesAreNull() {
        assertEquals(TutorialStep.TRADER, TutorialStep.of("trader"));
        assertNull(TutorialStep.of("renamed_step"));
    }

    @Test
    void miningWantsTenAndTheRestOneAction() {
        assertEquals(10, TutorialStep.MINE.goal());
        assertEquals(1, TutorialStep.UPGRADE.goal());
    }
}
