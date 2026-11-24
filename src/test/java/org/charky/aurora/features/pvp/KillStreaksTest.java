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
    void enoughKillsInTheWindowProtectTheVictim() {
        KillStreaks streaks = new KillStreaks();
        for (int kill = 1; kill < KillStreaks.KILLS; kill++) assertFalse(streaks.kill(a, b, kill));
        assertTrue(streaks.kill(a, b, 10));
        assertEquals(KillStreaks.COOLDOWN_MS, streaks.cooldownLeft(a, b, 10));
        // Only that killer, only that victim
        assertEquals(0, streaks.cooldownLeft(b, a, 10));
        assertEquals(0, streaks.cooldownLeft(a, c, 10));
        assertEquals(0, streaks.cooldownLeft(a, b, 10 + KillStreaks.COOLDOWN_MS));
    }

    // Killing someone else in between, or being killed back, changes nothing
    @Test
    void alternatingDoesNotDodgeIt() {
        KillStreaks streaks = new KillStreaks();
        assertFalse(streaks.kill(a, b, 0));
        streaks.kill(a, c, 1);
        streaks.kill(b, a, 2);
        assertFalse(streaks.kill(a, b, 3));
        streaks.kill(a, c, 4);
        assertTrue(streaks.kill(a, b, 5));
    }

    // Kills further apart than the window never add up
    @Test
    void oldKillsFallOutOfTheWindow() {
        KillStreaks streaks = new KillStreaks();
        long gap = KillStreaks.WINDOW_MS / 2 + 1;
        for (int kill = 0; kill < 5; kill++) assertFalse(streaks.kill(a, b, kill * gap));
    }
}
