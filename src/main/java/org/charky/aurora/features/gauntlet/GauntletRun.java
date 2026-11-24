package org.charky.aurora.features.gauntlet;

import net.kyori.adventure.bossbar.BossBar;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.charky.aurora.common.util.TextUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

// One run up the tower in its own copy of the arena, alone or with a party: the floor
// it is on, the party's pot of Shards, the floor's mobs, and each player's own part
final class GauntletRun {
    enum State { STARTING, FIGHTING, CHOOSING }

    // What each player of a run has of their own
    static final class Member {
        final UUID id;
        final Map<String, Integer> boons = new LinkedHashMap<>();
        // Boons offered right now, empty once chosen; an altar waiting for this player
        List<String> offered = List.of();
        boolean altar;
        double altarHealth;
        int rerolls;
        int revives;
        boolean daily;
        int bestBefore;
        boolean newBest;
        // Down until the floor is cleared, watching: the game mode to give back
        boolean down;
        GameMode mode;
        int kills;
        int eliteKills;
        // Their kills this run by mob, for the bestiary
        final Map<String, Integer> slain = new HashMap<>();
        long lastKill;
        int combo;
        int bestCombo;
        // Cores from boss floors cleared for the first time, theirs alone
        int cores;

        Member(UUID id) {
            this.id = id;
        }

        int stacks(String boon) {
            return boons.getOrDefault(boon, 0);
        }

        boolean choosing() {
            return altar || !offered.isEmpty();
        }
    }

    final int slot;
    final long started = System.currentTimeMillis();
    final Map<UUID, Member> members = new LinkedHashMap<>();
    int startFloor = 1;
    int heat;
    int floor;
    int shards;
    State state = State.STARTING;
    long choosingSince;
    boolean over;
    final Set<UUID> mobs = new HashSet<>();
    final Set<UUID> elites = new HashSet<>();
    // Elites' affixes, by mob
    final Map<UUID, String> affixes = new HashMap<>();
    // This floor: its mutator, how many mobs it started with and when, its boss, and
    // how many more mobs it has for the party (their kills pay that much less)
    GauntletData.Mutator mutator;
    int floorMobs;
    double floorScale = 1;
    long floorStarted;
    UUID boss;
    int bossPhase;
    // The party's run so far: how many started it, every mob killed by any of them
    int partySize;
    int kills;
    int fastClears;
    int bossKills;
    UUID hoarder;
    int hoarders;
    BossBar bar;
    // Everyone is down and on the way out: no "left the run" for each, no second wipe
    boolean wiping;
    // Hard Mode's floor, 0 without it: every floor below it plays and pays as it
    int hardFloor;
    // Where its care packages landed, to clear when it is over
    final Set<Location> packages = new HashSet<>();

    GauntletRun(int slot) {
        this.slot = slot;
    }

    // The floor whose numbers this floor's mobs and pay use: Hard Mode's floor below it
    int statFloor() {
        return Math.max(floor, hardFloor);
    }

    int cleared() {
        return state == State.FIGHTING || state == State.STARTING ? floor - 1 : floor;
    }

    boolean anyChoosing() {
        return members.values().stream().anyMatch(Member::choosing);
    }

    List<Player> online() {
        List<Player> players = new ArrayList<>();
        for (UUID id : members.keySet()) {
            Player player = Bukkit.getPlayer(id);
            if (player != null) players.add(player);
        }
        return players;
    }

    List<Player> standing() {
        List<Player> players = new ArrayList<>();
        for (Member member : members.values()) {
            Player player = Bukkit.getPlayer(member.id);
            if (player != null && !member.down) players.add(player);
        }
        return players;
    }

    // To everyone in the run
    void tell(String message) {
        for (Player player : online()) TextUtils.send(player, message);
    }

    void sound(Sound sound, float volume, float pitch) {
        for (Player player : online()) player.playSound(player.getLocation(), sound, volume, pitch);
    }

    // 1 for one player, and percent more for every player past the first
    static double partyScale(int players, double percent) {
        return 1 + Math.max(0, players - 1) * percent / 100;
    }
}
