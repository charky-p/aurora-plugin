package org.charky.aurora.listeners;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.charky.aurora.Aurora;
import org.charky.aurora.Database;
import org.charky.aurora.models.PlayerData;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class ScoreboardListener implements Listener {
    private final ConcurrentMap<UUID, Scoreboard> boards = new ConcurrentHashMap<>();
    private final ConcurrentMap<UUID, PlayerData> profiles = new ConcurrentHashMap<>();
//    private final Aurora plugin;
    private final Database db;
    private final BukkitTask task;

    public ScoreboardListener(Aurora plugin, Database db) {
//        this.plugin = plugin;
        this.db = db;

        this.task = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            this.db.saveAllPlayerData(this.profiles);
            plugin.getLogger().info("Auto-saved all player data to SQLite.");
        }, 12000L, 12000L);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        PlayerData data = this.db.loadPlayerData(uuid);
        this.profiles.put(uuid, data);
        this.setupBoard(player);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        this.boards.remove(event.getPlayer().getUniqueId());
        saveProfile(event.getPlayer());
    }

    public void setupBoard(Player player) {
        Scoreboard board = Bukkit.getScoreboardManager().getNewScoreboard();
        Objective obj = board.registerNewObjective("aurora", Criteria.DUMMY,
                Component.text("AURORA")
                        .color(NamedTextColor.AQUA)
                        .decoration(TextDecoration.BOLD, true)
                        .decoration(TextDecoration.ITALIC, false)
        );
        obj.setDisplaySlot(DisplaySlot.SIDEBAR);

        this.renderLines(obj, this.getBoardData(player));

        player.setScoreboard(board);
        this.boards.put(player.getUniqueId(), board);
    }

    public void updateBoard(Player player) {
        Scoreboard board = this.boards.get(player.getUniqueId());
        Objective obj = board.getObjective("aurora");
        if (obj == null) return;

        for (String entry : board.getEntries()) {
            board.resetScores(entry);
        }

        this.renderLines(obj, this.getBoardData(player));
    }

    private void renderLines(Objective obj, Component[] lines) {
        for (int i = 0; i < lines.length; i++) {
            String lineText = LegacyComponentSerializer.legacySection().serialize(lines[i]);
            obj.getScore(lineText).setScore(lines.length - i);
        }
    }

    public Component[] getBoardData(Player player) {
        UUID uuid = player.getUniqueId();
        PlayerData data = this.profiles.get(uuid);

        return new Component[]{
                Component.text("PLAYER: " + player.getName()),
                Component.text("    \uD83D\uDC51 RANK: " + data.getRank()),
                Component.text("    ⭐ LEVEL: " + data.getLevel()),
                Component.text("STATS:"),
                Component.text("    ⚔ KILLS: " + data.getKills()),
                Component.text("    ⛏ BLOCKS MINED: " + data.getMined()),
                Component.text("    \uD83D\uDD25 ESSENCE: " + data.getEssence()),
                Component.text("SERVER:"),
                Component.text("    ☀ ONLINE: " + Bukkit.getOnlinePlayers().size()),
                Component.text("    \uD83C\uDFAE EVENT: DEVELOPMENT")
        };
    }

    public void incrementMined(Player player) {
        PlayerData data = this.profiles.get(player.getUniqueId());
        if (data == null) return;

        data.addMined(1);
        this.updateBoard(player);
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
