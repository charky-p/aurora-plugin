package org.charky.aurora.features.pvp;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

// Stops farming one player: kill the same player KILLS_IN_A_ROW times in a row and
// you can't hurt them for COOLDOWN_MS. Killing anyone else, or being killed by them,
// starts the count again. Pure logic, tested directly; KillStreakListener applies it
public class KillStreaks {
    static final int KILLS_IN_A_ROW = 3;
    static final long COOLDOWN_MS = 5 * 60_000L;

    private record Streak(UUID victim, int kills) {
    }

    private record Pair(UUID killer, UUID victim) {
    }

    private final Map<UUID, Streak> streaks = new HashMap<>();
    private final Map<Pair, Long> protectedUntil = new HashMap<>();

    // A kill. True when it was the one too many and the victim is now protected
    public boolean kill(UUID killer, UUID victim, long now) {
        // Their own run against the killer is over
        Streak theirs = streaks.get(victim);
        if (theirs != null && theirs.victim().equals(killer)) streaks.remove(victim);

        Streak mine = streaks.get(killer);
        int kills = mine != null && mine.victim().equals(victim) ? mine.kills() + 1 : 1;
        if (kills < KILLS_IN_A_ROW) {
            streaks.put(killer, new Streak(victim, kills));
            return false;
        }
        streaks.remove(killer);
        protectedUntil.put(new Pair(killer, victim), now + COOLDOWN_MS);
        return true;
    }

    // Milliseconds until the attacker may hurt the victim again, 0 when they may now
    public long cooldownLeft(UUID attacker, UUID victim, long now) {
        Pair pair = new Pair(attacker, victim);
        Long until = protectedUntil.get(pair);
        if (until == null) return 0;
        if (now >= until) {
            protectedUntil.remove(pair);
            return 0;
        }
        return until - now;
    }
}
