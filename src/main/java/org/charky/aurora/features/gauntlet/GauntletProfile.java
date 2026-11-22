package org.charky.aurora.features.gauntlet;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

// What a player keeps from the gauntlet between runs: Shards and Cores to spend in
// the forge, the level of each piece of gear unlocked (missing means locked), the
// best floor cleared, runs played, the care package level, milestones paid, the
// starting kit and where it all sits in the inventory
public class GauntletProfile {
    public int shards;
    public int cores;
    public int best;
    public int runs;
    public int care = 1;
    public final Map<String, Integer> gear = new LinkedHashMap<>();
    public final Set<Integer> milestones = new HashSet<>();
    // Starting kit items bought and their level
    public final Map<String, Integer> setup = new LinkedHashMap<>();
    // Talents bought and their level
    public final Map<String, Integer> talents = new LinkedHashMap<>();
    // Counted stats by name: kills, runs, PvP wins, achievements done...
    public final Map<String, Integer> stats = new ConcurrentHashMap<>();
    // Where each setup item sits in the inventory, by slot: gear:sword, setup:totem, menu
    public final Map<Integer, String> layout = new ConcurrentHashMap<>();

    public boolean owns(String gearId) {
        return gear.containsKey(gearId);
    }

    public int level(String gearId) {
        return gear.getOrDefault(gearId, -1);
    }

    public int talent(String id) {
        return talents.getOrDefault(id, 0);
    }

    public int stat(String name) {
        return stats.getOrDefault(name, 0);
    }

    // Takes a price if it can be paid. Null if it was, otherwise what is missing
    public String pay(int shards, int cores) {
        if (this.shards < shards) return "You need " + (shards - this.shards) + " more Shards";
        if (this.cores < cores) return "You need " + (cores - this.cores) + " more Cores";
        this.shards -= shards;
        this.cores -= cores;
        return null;
    }
}
