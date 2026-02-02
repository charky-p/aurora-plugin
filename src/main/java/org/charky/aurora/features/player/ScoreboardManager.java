package org.charky.aurora.features.player;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.charky.aurora.common.util.TextUtils;
import org.charky.aurora.core.Aurora;
import org.charky.aurora.core.Database;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class ScoreboardManager {
    // TODO: player manager
    private final ConcurrentMap<UUID, Scoreboard> boards = new ConcurrentHashMap<>();
    private final ConcurrentMap<UUID, PlayerData> profiles = new ConcurrentHashMap<>();
    private final Aurora plugin;
    private final Database db;
    private final TabManager tabManager;
    private final BukkitTask task;

    public ScoreboardManager(Aurora plugin, Database db, TabManager tabManager) {
        this.plugin = plugin;
        this.db = db;
        this.tabManager = tabManager;

        // TODO: does not belong here
        this.task = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            this.db.saveAllPlayerData(this.profiles);
            plugin.getLogger().info("Auto-saved all player data to SQLite.");
        }, 12000L, 12000L);
    }

    public void handleJoin(Player player) {
        UUID uuid = player.getUniqueId();
        Bukkit.getScheduler().runTaskAsynchronously(this.plugin, () -> {
            PlayerData data = this.db.loadPlayerData(uuid);

            Bukkit.getScheduler().runTask(plugin, () -> {
                this.profiles.put(uuid, data);

                this.setupBoard(player);
                this.tabManager.updateAll();
            });
        });
    }

    public void handleQuit(Player player) {
        UUID uuid = player.getUniqueId();
        this.boards.remove(uuid);

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            saveProfile(player);
            this.profiles.remove(uuid);
        });

        this.tabManager.updateAll();
    }

    public void setupBoard(Player player) {
        Scoreboard board = Bukkit.getScoreboardManager().getNewScoreboard();
        Objective obj = board.registerNewObjective("aurora", Criteria.DUMMY, TextUtils.getServer());
        obj.setDisplaySlot(DisplaySlot.SIDEBAR);

        this.renderLines(board, obj, this.getBoardData(player));

        player.setScoreboard(board);
        this.boards.put(player.getUniqueId(), board);
    }

    public void updateBoard(Player player) {
        Scoreboard board = this.boards.get(player.getUniqueId());
        if (board == null) return;

        Objective obj = board.getObjective("aurora");
        if (obj == null) return;

        this.renderLines(board, obj, this.getBoardData(player));
    }

    private void renderLines(Scoreboard board, Objective obj, List<Component> lines) {
        for (int i = 0; i < lines.size(); i++) {
            // Unique identifier, color codes have no display
            String entryId = org.bukkit.ChatColor.values()[i].toString() + org.bukkit.ChatColor.RESET;

            org.bukkit.scoreboard.Team team = board.getTeam("line_" + i);
            if (team == null) team = board.registerNewTeam("line_" + i);

            if (!team.hasEntry(entryId)) team.addEntry(entryId);

            team.prefix(lines.get(i));
            obj.getScore(entryId).setScore(lines.size() - i);
        }
    }

    public List<Component> getBoardData(Player player) {
        PlayerData data = this.profiles.get(player.getUniqueId());
        return List.of(
                TextUtils.parse("<gradient:#7F7FD5:#91EAE4><b>PROFILE</b></gradient>"),
                TextUtils.parse("<gray>• RANK: <white>" + data.getRank()),
                TextUtils.parse("<gray>• LEVEL: <white>" + data.getLevel()),
                Component.empty(),
                TextUtils.parse("<gradient:#7F7FD5:#91EAE4><b>STATS</b></gradient>"),
                TextUtils.parse("<gray>• KILLS: <red>" + data.getKills()),
                TextUtils.parse("<gray>• MINED: <green>" + data.getMined()),
                TextUtils.parse("<gray>• ESSENCE: <gold>" + data.getEssence()),
                Component.empty(),
                TextUtils.parse("<light_purple>\uD83C\uDFAE <gray>EVENT: DEVELOPMENT"),
                TextUtils.parse("<aqua>ᴀᴜʀᴏʀᴀᴍɪɴᴇs.ᴍɪɴᴇʜᴜᴛ.ɢɢ")
        );
    }

    public void incrementMined(Player player) {
        PlayerData data = this.profiles.get(player.getUniqueId());
        if (data == null) return;

        data.addMined(1);
        // Manually update score board, this is only for data purposes
    }

    public void saveProfile(Player player) {
        UUID uuid = player.getUniqueId();
        this.db.savePlayerData(uuid, this.profiles.get(uuid));
    }

    public void saveAllProfiles() {
        this.db.saveAllPlayerData(this.profiles);
    }

    public void shutdown() {
        if (task != null) task.cancel();
        saveAllProfiles();
    }
}
