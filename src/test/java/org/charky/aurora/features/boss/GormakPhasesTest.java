package org.charky.aurora.features.boss;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GormakPhasesTest {
    @Test
    void aWaveForEachThresholdPassed() {
        double[] thresholds = GormakPhases.WAVE_THRESHOLDS;
        assertEquals(0, GormakPhases.wavesDue(1.0));
        for (int i = 0; i < thresholds.length; i++) {
            assertEquals(i, GormakPhases.wavesDue(thresholds[i] + 0.01), "just above " + thresholds[i]);
            assertEquals(i + 1, GormakPhases.wavesDue(thresholds[i]), "at " + thresholds[i]);
        }
        assertEquals(thresholds.length, GormakPhases.wavesDue(0.0));
        // One hit past two thresholds still owes both waves
        assertEquals(2, GormakPhases.wavesDue(thresholds[1] - 0.01) - GormakPhases.wavesDue(thresholds[0] + 0.01));
    }

    @Test
    void aWaveIsCutShortAtTheCap() {
        int cap = GormakPhases.MAX_GHOULS;
        assertEquals(3, GormakPhases.ghoulsToSummon(3, 0));
        assertEquals(2, GormakPhases.ghoulsToSummon(3, cap - 2));
        assertEquals(0, GormakPhases.ghoulsToSummon(3, cap));
        assertEquals(0, GormakPhases.ghoulsToSummon(3, cap + 3));
    }

    // The totem brings him back between the last two thresholds: below the one before
    // the last, or he would sit above a wave he already called and the bar would read oddly
    @Test
    void theTotemBringsHimBackBetweenTheLastTwoThresholds() {
        double[] thresholds = GormakPhases.WAVE_THRESHOLDS;
        assertTrue(GormakPhases.REVIVE_SHARE > thresholds[thresholds.length - 1]);
        assertTrue(GormakPhases.REVIVE_SHARE < thresholds[thresholds.length - 2]);
    }
}
