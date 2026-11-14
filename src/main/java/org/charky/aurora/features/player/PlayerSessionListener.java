package org.charky.aurora.features.player;

import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;

// Join and quit for the profile, sidebar, XP bar and tab list
public class PlayerSessionListener implements Listener {
    private final Plugin plugin;
    private final PlayerManager players;
    private final ScoreboardManager scoreboard;
    private final TabManager tab;
    private final LevelManager levels;

    public PlayerSessionListener(Plugin plugin, PlayerManager players, ScoreboardManager scoreboard,
                                 TabManager tab, LevelManager levels) {
        this.plugin = plugin;
        this.players = players;
        this.scoreboard = scoreboard;
        this.tab = tab;
        this.levels = levels;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        players.handleJoin(event.getPlayer(), () -> {
            scoreboard.setupBoard(event.getPlayer());
            levels.show(event.getPlayer());
            tab.updateAll();
        });
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        scoreboard.removeBoard(event.getPlayer());
        players.handleQuit(event.getPlayer());

        // Next tick, the quitting player still counts as online during the event
        Bukkit.getScheduler().runTask(plugin, tab::updateAll);
    }
}
