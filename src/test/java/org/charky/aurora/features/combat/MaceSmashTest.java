package org.charky.aurora.features.combat;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MaceSmashTest {
    @Test
    void aHopIsNoSmash() {
        assertEquals(1.0, PlayerDamage.smash(0));
        assertEquals(1.0, PlayerDamage.smash(1.5));
    }

    @Test
    void everyBlockFallenAddsAFifth() {
        assertEquals(1.4, PlayerDamage.smash(2), 1e-9);
        assertEquals(2.0, PlayerDamage.smash(5), 1e-9);
    }

    @Test
    void neverMoreThanThreeTimes() {
        assertEquals(3.0, PlayerDamage.smash(10));
        assertEquals(3.0, PlayerDamage.smash(40));
    }
}
