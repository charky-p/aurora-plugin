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

// The Daily Tower's board: who climbed today's tower and how far, one try each (marked
// when it starts, so quitting gives no second one). When the day turns the last day's
// top three are paid (tower_1 to tower_3 Shards), told at once or when they next join
final class GauntletTower {
    static final String DAY = "tower_day";
    static final String BEST = "tower_best";
    private static final String NOTICE = "tower_notice";

    private final GauntletManager manager;
    private final GauntletPaid paid;

    GauntletTower(GauntletManager manager, File dataFolder, Logger logger) {
        this.manager = manager;
        this.paid = new GauntletPaid(dataFolder, "gauntlet_tower_day.txt", logger);
    }

    static long today() {
        return LocalDate.now().toEpochDay();
    }

    boolean climbedToday(GauntletProfile profile) {
        return profile.stat(DAY) == today();
    }

    void started(UUID uuid) {
        manager.setStat(uuid, DAY, (int) today());
        manager.setStat(uuid, BEST, 0);
    }

    // How far a day's climbers got, furthest first
    List<Map.Entry<UUID, GauntletProfile>> top(long day, int count) {
        return manager.topBy(BEST, Integer.MAX_VALUE).stream()
                .filter(entry -> entry.getValue().stat(DAY) == day).limit(count).toList();
    }

    // How far a try went; a new leader of the day is told to the whole Gauntlet
    void result(Player player, long day, int cleared) {
        GauntletProfile profile = manager.profile(player);
        if (profile.stat(DAY) != day || cleared <= profile.stat(BEST)) return;
        List<Map.Entry<UUID, GauntletProfile>> leader = top(day, 1);
        boolean leads = cleared > 0 && (leader.isEmpty() || cleared > leader.getFirst().getValue().stat(BEST));
        manager.setStat(player.getUniqueId(), BEST, cleared);
        if (leads) manager.announce("<aqua><b>DAILY TOWER</b> <white>" + player.getName() + " <gray>leads today's tower, floor <aqua>"
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
            int shards = (int) manager.data().setting("tower_" + (place + 1), 0);
            if (shards <= 0) continue;
            GauntletProfile profile = top.get(place).getValue();
            profile.shards += shards;
            manager.save(uuid);
            manager.addStat(uuid, NOTICE, shards);
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) notice(player);
            manager.announce("<aqua><b>DAILY TOWER</b> <white>" + GauntletManager.nameOf(uuid) + " <gray>placed <white>#" + (place + 1)
                    + "</white> on yesterday's tower (floor " + profile.stat(BEST) + ") <aqua>+" + shards + " Shards");
        }
    }

    // A Daily Tower prize not told yet, on joining
    void notice(Player player) {
        int shards = manager.profile(player).stat(NOTICE);
        if (shards <= 0) return;
        manager.setStat(player.getUniqueId(), NOTICE, 0);
        manager.refreshBoard(player);
        player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.7f, 1f);
        TextUtils.send(player, "<aqua><b>DAILY TOWER</b> <gray>Your place on the last tower paid you <aqua>" + shards + " Shards");
    }
}
