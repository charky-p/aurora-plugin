package org.charky.aurora.features.player;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerExpChangeEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.plugin.Plugin;
import org.charky.aurora.common.util.TextUtils;

// Levels live on the vanilla XP bar: the number is the player's Aurora level and the
// bar how far through it they are. XP comes from mining and kills, see Levels
public class LevelManager implements Listener {
    private final Plugin plugin;
    private final PlayerManager players;
    private final ScoreboardManager scoreboard;

    public LevelManager(Plugin plugin, PlayerManager players, ScoreboardManager scoreboard) {
        this.plugin = plugin;
        this.players = players;
        this.scoreboard = scoreboard;
    }

    public void addXp(Player player, long amount) {
        PlayerData data = players.get(player);
        // Profile still loading
        if (data == null || amount <= 0) return;

        Levels.Gain gain = Levels.add(data.getLevel(), data.getXp(), amount);
        data.setProgress(gain.level(), gain.xp());
        show(player);

        if (gain.levelsGained() == 0) return;

        data.addEssence(gain.essence());
        scoreboard.updateBoard(player);
        announce(player, gain);
    }

    // Puts the player's level and progress on their XP bar
    public void show(Player player) {
        PlayerData data = players.get(player);
        if (data == null) return;

        player.setLevel(data.getLevel());
        player.setExp(Levels.progress(data.getLevel(), data.getXp()));
    }

    private void announce(Player player, Levels.Gain gain) {
        TextUtils.send(player, "<gradient:#7F7FD5:#91EAE4><b>LEVEL UP!</b></gradient> <gray>You are now level <white>"
                + gain.level() + " <dark_gray>(<gold>+" + gain.essence() + " Essence<dark_gray>)");

        if (Levels.isMilestone(gain.level())) {
            player.showTitle(Title.title(
                    TextUtils.parse("<gradient:#7F7FD5:#91EAE4><b>LEVEL " + gain.level() + "</b></gradient>"),
                    Component.text("+" + Levels.essenceFor(gain.level()) + " Essence")));
            player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.7f, 1f);
        } else {
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.7f, 1f);
        }
    }

    // The bar is ours. Vanilla XP from any other source would move it off the level
    @EventHandler
    public void onVanillaXp(PlayerExpChangeEvent event) {
        event.setAmount(0);
    }

    // Death clears the vanilla bar, next tick so it is the respawned player's
    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (player.isOnline()) show(player);
        });
    }
}
