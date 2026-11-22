package org.charky.aurora.features.gauntlet;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.charky.aurora.common.util.TextUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.IsoFields;
import java.time.temporal.TemporalAdjusters;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

// Weekly boards: the best floor and the most PvP wins (zone kills and duels) of the
// week, Monday to Sunday. When a new week starts the top three of each are paid
// (weekly_floor_ and weekly_pvp_ settings), straight into their Shards, told at once or
// when they next join. Each player's week is counted in their stats and starts over
// with the week; the week last paid is kept in a file of its own
public class GauntletWeekly {
    private static final String WEEK = "week_id";
    public static final String FLOOR = "week_floor";
    public static final String PVP = "week_pvp";
    private static final String NOTICE = "week_notice";
    private static final String FILE = "gauntlet_week.txt";

    private final GauntletManager manager;
    private final File file;
    private final Logger logger;

    public GauntletWeekly(GauntletManager manager, File dataFolder, Logger logger) {
        this.manager = manager;
        this.file = new File(dataFolder, FILE);
        this.logger = logger;
    }

    // 202647 for the 47th week of 2026
    public static int week(LocalDate date) {
        return date.get(IsoFields.WEEK_BASED_YEAR) * 100 + date.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR);
    }

    public static int thisWeek() {
        return week(LocalDate.now());
    }

    // Until the board closes, "2d 5h"
    public static String timeLeft() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime next = now.toLocalDate().with(TemporalAdjusters.next(DayOfWeek.MONDAY)).atStartOfDay();
        Duration left = Duration.between(now, next);
        return left.toDays() + "d " + left.toHoursPart() + "h";
    }

    // A new week wipes the player's counts
    private void fresh(UUID uuid) {
        GauntletProfile profile = manager.profile(uuid);
        int week = thisWeek();
        if (profile.stat(WEEK) == week) return;
        if (profile.stat(FLOOR) != 0) manager.setStat(uuid, FLOOR, 0);
        if (profile.stat(PVP) != 0) manager.setStat(uuid, PVP, 0);
        manager.setStat(uuid, WEEK, week);
    }

    public void floorCleared(UUID uuid, int floor) {
        fresh(uuid);
        if (floor > manager.profile(uuid).stat(FLOOR)) manager.setStat(uuid, FLOOR, floor);
    }

    public void pvpWin(UUID uuid) {
        fresh(uuid);
        manager.addStat(uuid, PVP, 1);
    }

    public int mine(UUID uuid, String stat) {
        fresh(uuid);
        return manager.profile(uuid).stat(stat);
    }

    // This week's top of a board
    public List<Map.Entry<UUID, GauntletProfile>> top(String stat, int count) {
        int week = thisWeek();
        return manager.topBy(stat, Integer.MAX_VALUE).stream()
                .filter(entry -> entry.getValue().stat(WEEK) == week)
                .limit(count).toList();
    }

    // Every minute: once the week has turned, pay last week's top three of each board
    public void check() {
        int now = thisWeek();
        int paid = readPaid();
        if (paid == now) return;
        if (paid != 0) {
            payout(paid, FLOOR, "weekly_floor_", "best floor");
            payout(paid, PVP, "weekly_pvp_", "PvP wins");
        }
        writePaid(now);
    }

    private void payout(int week, String stat, String prefix, String board) {
        List<Map.Entry<UUID, GauntletProfile>> top = manager.topBy(stat, Integer.MAX_VALUE).stream()
                .filter(entry -> entry.getValue().stat(WEEK) == week)
                .sorted(Comparator.comparingInt((Map.Entry<UUID, GauntletProfile> entry) -> entry.getValue().stat(stat)).reversed())
                .limit(3).toList();
        for (int place = 0; place < top.size(); place++) {
            UUID uuid = top.get(place).getKey();
            int shards = (int) manager.data().setting(prefix + (place + 1), 0);
            if (shards <= 0) continue;
            GauntletProfile profile = top.get(place).getValue();
            profile.shards += shards;
            manager.save(uuid);
            manager.addStat(uuid, NOTICE, shards);
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) notice(player);
            announce("<gold><b>WEEKLY</b> <white>" + GauntletManager.nameOf(uuid) + " <gray>placed <white>#" + (place + 1)
                    + "</white> for " + board + " (" + profile.stat(stat) + ") <aqua>+" + shards + " Shards");
        }
    }

    // A weekly reward not told yet, on joining
    public void notice(Player player) {
        int shards = manager.profile(player).stat(NOTICE);
        if (shards <= 0) return;
        manager.setStat(player.getUniqueId(), NOTICE, 0);
        manager.refreshBoard(player);
        player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.7f, 1f);
        TextUtils.send(player, "<gold><b>WEEKLY REWARD</b> <gray>Last week's Gauntlet boards paid you <aqua>" + shards + " Shards");
    }

    private void announce(String message) {
        Location lobby = manager.arena().lobby();
        if (lobby == null || lobby.getWorld() == null) return;
        for (Player player : lobby.getWorld().getPlayers()) TextUtils.send(player, message);
    }

    private int readPaid() {
        if (!file.exists()) return 0;
        try {
            return Integer.parseInt(Files.readString(file.toPath()).trim());
        } catch (IOException | NumberFormatException e) {
            return 0;
        }
    }

    private void writePaid(int week) {
        try {
            Files.writeString(file.toPath(), String.valueOf(week));
        } catch (IOException e) {
            logger.warning("Could not save the gauntlet week: " + e.getMessage());
        }
    }
}
