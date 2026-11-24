package org.charky.aurora.features.gauntlet;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GauntletRunsTest {
    @Test
    void namesReadAsASentence() {
        assertEquals("Ana", GauntletRuns.names(List.of("Ana")));
        assertEquals("Ana and Bo", GauntletRuns.names(List.of("Ana", "Bo")));
        assertEquals("Ana, Bo and Cy", GauntletRuns.names(List.of("Ana", "Bo", "Cy")));
    }

    @Test
    void hardModeRaisesOnlyTheFloorsBelowIt() {
        GauntletRun run = new GauntletRun(0);
        run.floor = 3;
        assertEquals(3, run.statFloor(), "without Hard Mode a floor is itself");
        run.hardFloor = 36;
        assertEquals(36, run.statFloor());
        run.floor = 40;
        assertEquals(40, run.statFloor(), "past the Hard floor the tower is as it is");
    }

    @Test
    void timesReadAsAClock() {
        assertEquals("0:05", GauntletRuns.clock(5));
        assertEquals("12:34", GauntletRuns.clock(754));
    }
}
