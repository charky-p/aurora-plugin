package org.charky.aurora.features.pvp;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

// Stops farming one player: kill the same player KILLS times within WINDOW_MS and you
// can't hurt them for COOLDOWN_MS. Killing others in between, or trading kills, does
// not reset it. Pure logic, tested directly; KillStreakListener applies it
public class KillStreaks {
    static final int KILLS = 3;
    static final long WINDOW_MS = 5 * 60_000L;
    static final long COOLDOWN_MS = 5 * 60_000L;

    private record Pair(UUID killer, UUID victim) {
    }

    // When each killer last killed each victim, within the window
    private final Map<Pair, Deque<Long>> kills = new HashMap<>();
    private final Map<Pair, Long> protectedUntil = new HashMap<>();

    // A kill. True when it was the one too many and the victim is now protected
    public boolean kill(UUID killer, UUID victim, long now) {
        Pair pair = new Pair(killer, victim);
        Deque<Long> times = kills.computeIfAbsent(pair, k -> new ArrayDeque<>());
        while (!times.isEmpty() && now - times.peekFirst() >= WINDOW_MS) times.pollFirst();
        times.addLast(now);
        if (times.size() < KILLS) return false;
        kills.remove(pair);
        protectedUntil.put(pair, now + COOLDOWN_MS);
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
