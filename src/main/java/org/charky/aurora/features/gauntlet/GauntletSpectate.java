package org.charky.aurora.features.gauntlet;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.scheduler.BukkitTask;
import org.charky.aurora.common.util.TextUtils;
import org.charky.aurora.core.Aurora;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

// Watching someone else's run or duel: /gauntlet spectate <player>, or the Spectate
// button. The watcher goes into spectator mode beside them and is kept within
// KEEP_CLOSE blocks; the sidebar shows the run. It ends with the run or duel, with
// /spawn or /gauntlet spectate stop, and the watcher is back in the lobby with their setup
public class GauntletSpectate implements Listener {
    private static final double KEEP_CLOSE = 40;

    private final Aurora plugin;
    private final GauntletManager manager;
    // Watchers and who they watch
    private final Map<UUID, UUID> watching = new HashMap<>();
    private BukkitTask task;

    public GauntletSpectate(Aurora plugin, GauntletManager manager) {
        this.plugin = plugin;
        this.manager = manager;
    }

    public void start() {
        task = plugin.getServer().getScheduler().runTaskTimer(plugin, this::tick, 20L, 20L);
    }

    public void shutdown() {
        if (task != null) task.cancel();
        for (UUID id : Map.copyOf(watching).keySet()) {
            Player viewer = Bukkit.getPlayer(id);
            if (viewer != null) stop(viewer, null);
        }
    }

    public boolean isWatching(Player player) {
        return watching.containsKey(player.getUniqueId());
    }

    // Who a watcher watches, null if nobody
    public Player target(Player viewer) {
        UUID target = watching.get(viewer.getUniqueId());
        return target == null ? null : Bukkit.getPlayer(target);
    }

    private boolean live(Player target) {
        return target != null && (manager.inRun(target) || manager.inDuel(target));
    }

    // Null if it worked, otherwise why not
    public String watch(Player viewer, Player target) {
        if (target == null) return "That player is not online";
        if (viewer.equals(target)) return "You can't spectate yourself";
        if (!manager.isHere(viewer)) return "Go to the Gauntlet first";
        if (manager.inRun(viewer) || manager.inDuel(viewer)) return "Not during your own run or duel";
        if (manager.pvp() != null && manager.pvp().inZone(viewer)) return "Leave the PvP zone first";
        if (!live(target)) return target.getName() + " is not in a run or a duel";
        watching.put(viewer.getUniqueId(), target.getUniqueId());
        viewer.closeInventory();
        viewer.setGameMode(GameMode.SPECTATOR);
        viewer.teleport(target.getLocation().add(0, 3, 0));
        TextUtils.send(viewer, "<gray>Spectating <white>" + target.getName() + "<gray>. <white>/spawn</white> or "
                + "<click:run_command:'/gauntlet spectate stop'><yellow>[STOP]</click> <gray>to go back");
        return null;
    }

    public void stop(Player viewer, String why) {
        if (watching.remove(viewer.getUniqueId()) == null) return;
        viewer.setGameMode(Bukkit.getDefaultGameMode());
        if (manager.isHere(viewer)) {
            Location lobby = manager.arena().lobby();
            if (lobby != null) viewer.teleport(lobby);
            manager.giveKit(viewer);
        }
        if (why != null) TextUtils.send(viewer, "<gray>" + why + ", back to the lobby");
    }

    // Every second: done when the run or duel is, kept close, the action bar says what is on
    private void tick() {
        for (Map.Entry<UUID, UUID> entry : Map.copyOf(watching).entrySet()) {
            Player viewer = Bukkit.getPlayer(entry.getKey());
            if (viewer == null) {
                watching.remove(entry.getKey());
                continue;
            }
            Player target = Bukkit.getPlayer(entry.getValue());
            if (!live(target)) {
                stop(viewer, "The run is over");
                continue;
            }
            if (!viewer.getWorld().equals(target.getWorld()) || viewer.getLocation().distance(target.getLocation()) > KEEP_CLOSE) {
                viewer.teleport(target.getLocation().add(0, 3, 0));
            }
            viewer.sendActionBar(TextUtils.parse("<gray>Spectating <white>" + target.getName()
                    + (manager.inRun(target) ? " <dark_gray>| <red>Floor " + manager.runs().floorOf(target) : " <dark_gray>| <gold>Duel")));
            manager.refreshBoard(viewer);
        }
    }

    // A watcher stays with the one watched: no hopping to other players
    @EventHandler(ignoreCancelled = true)
    public void onTeleport(PlayerTeleportEvent event) {
        if (isWatching(event.getPlayer()) && event.getCause() == PlayerTeleportEvent.TeleportCause.SPECTATE) event.setCancelled(true);
    }

    // /spawn and /gauntlet leave stop watching, nothing starts while watching
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        if (!isWatching(player)) return;
        String command = event.getMessage().toLowerCase().trim().replaceAll("\\s+", " ");
        if (command.equals("/spawn") || command.startsWith("/spawn ") || command.equals("/gauntlet leave")) {
            event.setCancelled(true);
            stop(player, "Stopped spectating");
        } else if (command.startsWith("/gauntlet start") || command.startsWith("/gauntlet duel")) {
            event.setCancelled(true);
            TextUtils.error(player, "Stop spectating first, /gauntlet spectate stop");
        }
    }

    // Leaving the server ends watching, out of spectator mode
    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        if (watching.remove(player.getUniqueId()) != null) player.setGameMode(Bukkit.getDefaultGameMode());
    }
}
