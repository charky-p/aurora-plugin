package org.charky.aurora.features.player;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EssenceTest {
    @Test
    void luckMultipliesWhatAFindPays() {
        assertEquals(2, Essence.amount(2, 0));
        assertEquals(4, Essence.amount(2, 1));
        assertEquals(33, Essence.amount(3, 10));
        assertEquals(1, Essence.amount(1, -1));
    }
}
