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
    void theThirdKillInARowProtectsTheVictim() {
        KillStreaks streaks = new KillStreaks();
        assertFalse(streaks.kill(a, b, 0));
        assertFalse(streaks.kill(a, b, 1));
        assertTrue(streaks.kill(a, b, 2));
        assertEquals(KillStreaks.COOLDOWN_MS, streaks.cooldownLeft(a, b, 2));
        // Only that killer, only that victim
        assertEquals(0, streaks.cooldownLeft(b, a, 2));
        assertEquals(0, streaks.cooldownLeft(a, c, 2));
        assertEquals(0, streaks.cooldownLeft(a, b, 2 + KillStreaks.COOLDOWN_MS));
    }

    @Test
    void killingSomeoneElseStartsAgain() {
        KillStreaks streaks = new KillStreaks();
        streaks.kill(a, b, 0);
        streaks.kill(a, b, 1);
        streaks.kill(a, c, 2);
        assertFalse(streaks.kill(a, b, 3));
    }

    @Test
    void beingKilledBackStartsAgain() {
        KillStreaks streaks = new KillStreaks();
        streaks.kill(a, b, 0);
        streaks.kill(a, b, 1);
        streaks.kill(b, a, 2);
        assertFalse(streaks.kill(a, b, 3));
    }
}
