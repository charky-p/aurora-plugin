package org.charky.aurora.features.combat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MaceSmashTest {
    // A hop is no smash, every block fallen adds a fifth, never more than three times
    @ParameterizedTest
    @CsvSource({"0, 1", "1.5, 1", "2, 1.4", "5, 2", "10, 3", "40, 3"})
    void everyBlockFallenAddsAFifth(double fallen, double expected) {
        assertEquals(expected, PlayerDamage.smash(fallen), 1e-9);
    }
}
