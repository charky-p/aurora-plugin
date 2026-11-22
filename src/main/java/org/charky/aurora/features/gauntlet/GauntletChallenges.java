package org.charky.aurora.features.gauntlet;

import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.charky.aurora.common.util.TextUtils;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.UUID;

// Daily challenges (challenges.csv): every day the same few for everyone, picked from
// the pool by the date. Each counts a stat over the day (kills, elites, fast clears,
// boss kills, PvP kills, runs, or the best floor cleared in one run) and pays Shards,
// more the higher the player's best floor, the moment it is done. Progress lives in
// the player's counted stats and starts over with the day
public class GauntletChallenges {
    private static final String DAY = "ch_day";
    private static final String COUNT = "ch:";
    private static final String DONE = "ch_done:";

    private final GauntletManager manager;

    public GauntletChallenges(GauntletManager manager) {
        this.manager = manager;
    }

    private static long day() {
        return LocalDate.now().toEpochDay();
    }

    // Today's challenges, the same for everyone
    public List<GauntletData.Challenge> today(long day) {
        List<GauntletData.Challenge> pool = new ArrayList<>(manager.data().challenges().values());
        Collections.shuffle(pool, new Random(day * 31 + 7));
        int count = (int) manager.data().setting("challenges_per_day", 3);
        return List.copyOf(pool.subList(0, Math.min(count, pool.size())));
    }

    public List<GauntletData.Challenge> today() {
        return today(day());
    }

    // Counts a stat towards today's challenges
    public void add(Player player, String stat, int amount) {
        if (amount <= 0) return;
        fresh(player);
        int value = manager.addStat(player.getUniqueId(), COUNT + stat, amount);
        check(player, stat, value);
    }

    // For a stat that counts its best, the floor cleared in one run
    public void best(Player player, String stat, int value) {
        fresh(player);
        UUID uuid = player.getUniqueId();
        if (value <= manager.profile(player).stat(COUNT + stat)) return;
        manager.setStat(uuid, COUNT + stat, value);
        check(player, stat, value);
    }

    public int progress(Player player, GauntletData.Challenge challenge) {
        fresh(player);
        return manager.profile(player).stat(COUNT + challenge.stat());
    }

    public boolean done(Player player, GauntletData.Challenge challenge) {
        fresh(player);
        return manager.profile(player).stat(DONE + challenge.id()) > 0;
    }

    public int goal(Player player, GauntletData.Challenge challenge) {
        return challenge.goalFor(manager.profile(player).best);
    }

    public int reward(Player player, GauntletData.Challenge challenge) {
        double perFloor = manager.data().setting("challenge_reward_per_floor", 10);
        return (int) Math.round(challenge.shards() * (1 + manager.profile(player).best * perFloor / 100));
    }

    // How many of today's are done
    public int doneToday(Player player) {
        int done = 0;
        for (GauntletData.Challenge challenge : today()) if (done(player, challenge)) done++;
        return done;
    }

    // A new day wipes yesterday's counts
    private void fresh(Player player) {
        GauntletProfile profile = manager.profile(player);
        long day = day();
        if (profile.stat(DAY) == day) return;
        UUID uuid = player.getUniqueId();
        for (String stat : new ArrayList<>(profile.stats.keySet())) {
            if ((stat.startsWith(COUNT) || stat.startsWith(DONE)) && profile.stat(stat) != 0) manager.setStat(uuid, stat, 0);
        }
        manager.setStat(uuid, DAY, (int) day);
    }

    private void check(Player player, String stat, int value) {
        for (GauntletData.Challenge challenge : today()) {
            if (!challenge.stat().equals(stat) || done(player, challenge) || value < goal(player, challenge)) continue;
            manager.setStat(player.getUniqueId(), DONE + challenge.id(), 1);
            int shards = reward(player, challenge);
            manager.giveShards(player, shards);
            if (challenge.cores() > 0) {
                manager.profile(player).cores += challenge.cores();
                manager.save(player.getUniqueId());
            }
            player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.6f, 1.3f);
            TextUtils.send(player, "<green><b>DAILY CHALLENGE</b> <white>" + challenge.describe(manager.profile(player).best)
                    + " <aqua>+" + shards + " Shards" + (challenge.cores() > 0 ? " <light_purple>+" + challenge.cores() + " Core" : "")
                    + " <dark_gray>(" + doneToday(player) + "/" + today().size() + " today)");
        }
    }
}
