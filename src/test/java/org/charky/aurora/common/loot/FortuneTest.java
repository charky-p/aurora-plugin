package org.charky.aurora.common.loot;

import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FortuneTest {
    @Test
    void nothingBelowTheDensityThenTheSoftQuadratic() {
        assertEquals(0, Fortune.multiplier(2, 3));
        assertEquals(1, Fortune.multiplier(3, 3));
        assertEquals(3.5, Fortune.multiplier(5, 3));
        assertEquals(116, Fortune.multiplier(20, 0));
    }

    @Test
    void rareDropsGrowWithTheCubeRoot() {
        assertEquals(1, Fortune.rareMultiplier(0, 0));
        assertEquals(Math.cbrt(116), Fortune.rareMultiplier(20, 0), 1e-9);
        assertEquals(0, Fortune.rareMultiplier(0, 1));
    }

    @Test
    void commonDropsRoundAndNeverFallBelowOne() {
        assertEquals(580, Fortune.scale(5, 116));
        assertEquals(1, Fortune.scale(1, 0.1));
    }

    @Test
    void rareDropsAtFortuneTwentyStayInTheirOwnRange() {
        Random random = new Random(1);
        double multiplier = Fortune.rareMultiplier(20, 0);
        for (int i = 0; i < 1000; i++) {
            int one = Fortune.scaleRare(1, multiplier, random);
            int two = Fortune.scaleRare(2, multiplier, random);
            assertTrue(one == 4 || one == 5, "1 became " + one);
            assertTrue(two == 9 || two == 10, "2 became " + two);
        }
    }

    @Test
    void theFractionIsAChanceOfOneMore() {
        Random random = new Random(2);
        int extra = 0;
        for (int i = 0; i < 10000; i++) {
            if (Fortune.scaleRare(1, 1.3, random) == 2) extra++;
        }
        assertTrue(extra > 2700 && extra < 3300, "got " + extra);
    }
}
