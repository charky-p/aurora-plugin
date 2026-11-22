package org.charky.aurora.features.gauntlet;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class GauntletRunsTest {
    @Test
    void namesReadAsASentence() {
        assertEquals("Ana", GauntletRuns.names(List.of("Ana")));
        assertEquals("Ana and Bo", GauntletRuns.names(List.of("Ana", "Bo")));
        assertEquals("Ana, Bo and Cy", GauntletRuns.names(List.of("Ana", "Bo", "Cy")));
    }

    @Test
    void theDailyTowerRollsTheSameForEveryoneThatDay() {
        GauntletRun one = new GauntletRun(0), other = new GauntletRun(3);
        one.towerDay = other.towerDay = 20_400;
        one.floor = other.floor = 12;
        assertEquals(one.floorRandom(2).nextInt(), other.floorRandom(2).nextInt(), "same day and floor, same rolls");
        assertNotEquals(one.floorRandom(1).nextLong(), one.floorRandom(2).nextLong(), "each of a floor's rolls apart");
        other.floor = 13;
        assertNotEquals(one.floorRandom(2).nextLong(), other.floorRandom(2).nextLong(), "another floor, other rolls");
        assertSame(ThreadLocalRandom.current(), new GauntletRun(0).floorRandom(2), "any other run rolls freely");
    }

    @Test
    void timesReadAsAClock() {
        assertEquals("0:05", GauntletRuns.clock(5));
        assertEquals("12:34", GauntletRuns.clock(754));
    }
}
