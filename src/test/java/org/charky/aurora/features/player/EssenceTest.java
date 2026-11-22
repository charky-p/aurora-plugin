package org.charky.aurora.features.player;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EssenceTest {
    @Test
    void luckMultipliesWhatAFindPays() {
        assertEquals(2, Essence.amount(2, 0));
        assertEquals(4, Essence.amount(2, 1));
        assertEquals(33, Essence.amount(3, 10));
        assertEquals(1, Essence.amount(1, -1));
    }

    @Test
    void essenceCanOnlyBeSpentIfThereIsEnough() {
        PlayerData data = new PlayerData("MEMBER", 0, 0, 0, 100);

        assertFalse(data.spendEssence(101));
        assertEquals(100, data.getEssence());
        assertTrue(data.spendEssence(100));
        assertEquals(0, data.getEssence());
    }
}
