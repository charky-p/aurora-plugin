package org.charky.aurora.features.gauntlet;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GauntletBoardsTest {
    @Test
    void anEmptyBoardSaysSo() {
        assertEquals(List.of("<gray>Nobody yet"), GauntletBoards.rows(List.of(), profile -> "", "Nobody yet"));
    }
}
