package org.charky.aurora.features.gauntlet;

import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.charky.aurora.common.util.TextUtils;

import java.io.File;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

// The day's best runs: the furthest floor each player cleared today, in any run, alone
// or in a party. When the day turns the last day's top three are paid (daily_1 to
// daily_3 Shards), told at once or when they next join
final class GauntletDaily {
    // Stat names from when this was the Daily Tower, kept so the stats carry on
    static final String DAY = "tower_day";
    static final String BEST = "tower_best";
    private static final String NOTICE = "tower_notice";
    // Days topped, counted for an achievement
    private static final String WINS = "tower_wins";

    private final GauntletManager manager;
    private final GauntletPaid paid;

    GauntletDaily(GauntletManager manager, File dataFolder, Logger logger) {
        this.manager = manager;
        this.paid = new GauntletPaid(dataFolder, "gauntlet_tower_day.txt", logger);
    }

    static long today() {
        return LocalDate.now().toEpochDay();
    }

    // How far a day's players got, furthest first
    List<Map.Entry<UUID, GauntletProfile>> top(long day, int count) {
        return manager.topBy(BEST, Integer.MAX_VALUE).stream()
                .filter(entry -> entry.getValue().stat(DAY) == day && entry.getValue().stat(BEST) > 0).limit(count).toList();
    }

    // The player's floor today, 0 before their first run of the day
    int bestToday(GauntletProfile profile) {
        return profile.stat(DAY) == today() ? profile.stat(BEST) : 0;
    }

    // A run's floors cleared; a new best of the day is kept, a new leader told to the whole Gauntlet
    void result(Player player, int cleared) {
        GauntletProfile profile = manager.profile(player);
        long day = today();
        if (cleared <= bestToday(profile)) return;
        List<Map.Entry<UUID, GauntletProfile>> leader = top(day, 1);
        boolean leads = leader.isEmpty() || cleared > leader.getFirst().getValue().stat(BEST);
        manager.setStat(player.getUniqueId(), DAY, (int) day);
        manager.setStat(player.getUniqueId(), BEST, cleared);
        if (leads) manager.announce("<aqua><b>BEST TODAY</b> <white>" + player.getName() + " <gray>has the day's best run, floor <aqua>"
                + cleared + "</aqua> cleared");
    }

    // Every minute: once the day has turned, the last day's top three are paid
    void check() {
        long now = today();
        long last = paid.read();
        if (last == now) return;
        if (last != 0) payout(last);
        paid.write(now);
    }

    private void payout(long day) {
        List<Map.Entry<UUID, GauntletProfile>> top = top(day, 3);
        for (int place = 0; place < top.size(); place++) {
            UUID uuid = top.get(place).getKey();
            if (place == 0) manager.addStat(uuid, WINS, 1);
            int shards = (int) manager.data().setting("daily_" + (place + 1), 0);
            if (shards <= 0) continue;
            GauntletProfile profile = top.get(place).getValue();
            profile.shards += shards;
            manager.save(uuid);
            manager.addStat(uuid, NOTICE, shards);
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) notice(player);
            manager.announce("<aqua><b>BEST TODAY</b> <white>" + GauntletManager.nameOf(uuid) + " <gray>placed <white>#" + (place + 1)
                    + "</white> yesterday (floor " + profile.stat(BEST) + ") <aqua>+" + shards + " Shards");
        }
    }

    // A prize for the day's best not told yet, on joining
    void notice(Player player) {
        int shards = manager.profile(player).stat(NOTICE);
        if (shards <= 0) return;
        manager.setStat(player.getUniqueId(), NOTICE, 0);
        manager.refreshBoard(player);
        player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.7f, 1f);
        TextUtils.send(player, "<aqua><b>BEST TODAY</b> <gray>Your place in yesterday's best runs paid you <aqua>" + shards + " Shards");
    }
}
