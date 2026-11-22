package org.charky.aurora.features.boss;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GormakPhasesTest {
    // A wave for each threshold passed, so one hit from 0.80 to 0.40 still owes two
    @ParameterizedTest
    @CsvSource({"1.0, 0", "0.76, 0", "0.75, 1", "0.51, 1", "0.50, 2", "0.40, 2", "0.25, 3", "0.0, 3"})
    void aWaveForEachThresholdPassed(double health, int waves) {
        assertEquals(waves, GormakPhases.wavesDue(health));
    }

    @Test
    void aWaveIsCutShortAtTheCap() {
        assertEquals(3, GormakPhases.ghoulsToSummon(3, 0));
        assertEquals(2, GormakPhases.ghoulsToSummon(3, 4));
        assertEquals(0, GormakPhases.ghoulsToSummon(3, 9));
    }

    // He has to come back with less than the last threshold, or the totem would
    // put him above a wave he already called and the bar would read oddly
    @Test
    void theTotemBringsHimBackBelowHalf() {
        assertTrue(GormakPhases.REVIVE_SHARE > 0.25);
        assertTrue(GormakPhases.REVIVE_SHARE < 0.5);
    }
}
