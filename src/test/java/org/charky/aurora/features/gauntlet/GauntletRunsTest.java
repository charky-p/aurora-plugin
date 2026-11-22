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
}
