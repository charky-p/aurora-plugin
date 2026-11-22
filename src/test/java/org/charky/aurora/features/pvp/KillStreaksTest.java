package org.charky.aurora.features.pvp;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KillStreaksTest {
    private final UUID a = UUID.randomUUID();
    private final UUID b = UUID.randomUUID();
    private final UUID c = UUID.randomUUID();

    @Test
    void enoughKillsInARowProtectTheVictim() {
        KillStreaks streaks = new KillStreaks();
        for (int kill = 1; kill < KillStreaks.KILLS_IN_A_ROW; kill++) assertFalse(streaks.kill(a, b, 0));
        assertTrue(streaks.kill(a, b, 2));
        assertEquals(KillStreaks.COOLDOWN_MS, streaks.cooldownLeft(a, b, 2));
        // Only that killer, only that victim
        assertEquals(0, streaks.cooldownLeft(b, a, 2));
        assertEquals(0, streaks.cooldownLeft(a, c, 2));
        assertEquals(0, streaks.cooldownLeft(a, b, 2 + KillStreaks.COOLDOWN_MS));
    }

    // Killing someone else, or being killed back, starts the streak again
    @Test
    void anotherKillStartsAgain() {
        for (UUID[] between : new UUID[][]{{a, c}, {b, a}}) {
            KillStreaks streaks = new KillStreaks();
            for (int kill = 1; kill < KillStreaks.KILLS_IN_A_ROW; kill++) streaks.kill(a, b, 0);
            streaks.kill(between[0], between[1], 2);
            assertFalse(streaks.kill(a, b, 3));
        }
    }
}
