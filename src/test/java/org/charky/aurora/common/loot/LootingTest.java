package org.charky.aurora.common.loot;

import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;

// Past the density Looting is the Fortune curve, which FortuneTest covers
class LootingTest {
    @Test
    void belowTheDensityNothingAtItTheAmount() {
        Random random = new Random(3);
        assertEquals(0, Looting.apply(3, -1, false, random));
        assertEquals(0, Looting.apply(3, -2, true, random));
        assertEquals(3, Looting.apply(3, 0, false, random));
        assertEquals(3, Looting.apply(3, 0, true, random));
        assertEquals(69, Looting.apply(3, 8, false, random));
    }
}
