package org.charky.aurora.features.boss;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GormakPhasesTest {
    @Test
    void noWaveAtFullHealth() {
        assertEquals(0, GormakPhases.wavesDue(1.0));
        assertEquals(0, GormakPhases.wavesDue(0.76));
    }

    @Test
    void aWaveForEachThresholdPassed() {
        assertEquals(1, GormakPhases.wavesDue(0.75));
        assertEquals(1, GormakPhases.wavesDue(0.51));
        assertEquals(2, GormakPhases.wavesDue(0.50));
        assertEquals(3, GormakPhases.wavesDue(0.25));
        assertEquals(3, GormakPhases.wavesDue(0.0));
    }

    // One hit that crosses two thresholds still owes both waves
    @Test
    void aBigHitDoesNotSkipAWave() {
        assertEquals(2, GormakPhases.wavesDue(0.40) - GormakPhases.wavesDue(0.80));
    }

    @Test
    void aWaveIsCutShortAtTheCap() {
        assertEquals(3, GormakPhases.ghostsToSummon(3, 0));
        assertEquals(2, GormakPhases.ghostsToSummon(3, 4));
        assertEquals(0, GormakPhases.ghostsToSummon(3, 6));
        assertEquals(0, GormakPhases.ghostsToSummon(3, 9));
    }

    // He has to come back with less than the last threshold, or the totem would
    // put him above a wave he already called and the bar would read oddly
    @Test
    void theTotemBringsHimBackBelowHalf() {
        assertTrue(GormakPhases.REVIVE_SHARE > 0.25);
        assertTrue(GormakPhases.REVIVE_SHARE < 0.5);
    }
}
