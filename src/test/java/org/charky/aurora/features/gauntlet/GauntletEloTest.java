package org.charky.aurora.features.gauntlet;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GauntletEloTest {
    @Test
    void anEvenDuelMovesHalfOfK() {
        assertArrayEquals(new int[]{1016, 984}, GauntletElo.after(1000, 1000, 32));
    }

    @Test
    void anUpsetMovesMoreThanAnExpectedWin() {
        int upset = GauntletElo.after(900, 1200, 32)[0] - 900;
        int expected = GauntletElo.after(1200, 900, 32)[0] - 1200;
        assertTrue(upset > expected, upset + " should be more than " + expected);
        assertEquals(1, GauntletElo.after(3000, 1000, 32)[0] - 3000, "a win always moves the rating");
    }
}
