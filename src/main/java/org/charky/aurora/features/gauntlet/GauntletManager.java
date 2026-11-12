package org.charky.aurora.features.gauntlet;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.charky.aurora.common.util.TextUtils;
import org.charky.aurora.core.Aurora;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

// Unfinished wave mode behind /gauntlet, admin only. The arena is still a fixed
// spot in the main world
public class GauntletManager {
    private static final String ARENA_WORLD = "world";
    private static final int LOBBY_SECONDS = 15;
    private static final long WAVE_GAP_TICKS = 100L;

    private final Aurora plugin;
    private final Set<UUID> players = new HashSet<>();
    private final List<Entity> activeMobs = new ArrayList<>();
    private GameState state = GameState.WAITING;
    private int waveNumber = 0;
    private int countdown = LOBBY_SECONDS;
    // Set while the next wave is on its way, so several deaths in the gap between
    // waves start one wave, not one each
    private boolean waveQueued = false;

    public GauntletManager(Aurora plugin) {
        this.plugin = plugin;
    }

    public enum GameState {
        WAITING, STARTING, ACTIVE, CLEANUP
    }

    // Null when the arena's world is not loaded
    public Location lobby() {
        World world = Bukkit.getWorld(ARENA_WORLD);
        return world == null ? null : new Location(world, -246, -59, 207);
    }

    public void joinQueue(Player player) {
        if (state == GameState.ACTIVE) {
            TextUtils.error(player, "Gauntlet is already in progress");
            return;
        }

        players.add(player.getUniqueId());
        broadcast("<white>" + player.getName() + " joined the gauntlet");

        if (state == GameState.WAITING) {
            startLobbyCooldown();
        }
    }

    public void startLobbyCooldown() {
        state = GameState.STARTING;

        new BukkitRunnable() {
            @Override
            public void run() {
                // Everyone left during the countdown
                if (state != GameState.STARTING) {
                    this.cancel();
                    return;
                }

                if (countdown <= 0) {
                    startGame();
                    this.cancel();
                    return;
                }

                if (countdown % 10 == 0 || countdown <= 5) {
                    broadcast("<yellow>Gauntlet starting in <white>" + countdown + "s...");
                }
                countdown--;
            }
        }.runTaskTimer(plugin, 0, 20);
    }

    public void startGame() {
        state = GameState.ACTIVE;

        for (UUID uuid : players) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                preparePlayer(player);
            }
        }

        startNextWave();
    }

    private void preparePlayer(Player player) {
        player.getInventory().clear();
        player.getInventory().setHelmet(new ItemStack(Material.LEATHER_HELMET));
        player.getInventory().setChestplate(new ItemStack(Material.LEATHER_CHESTPLATE));
        player.getInventory().setLeggings(new ItemStack(Material.LEATHER_LEGGINGS));
        player.getInventory().setBoots(new ItemStack(Material.LEATHER_BOOTS));
        player.getInventory().addItem(new ItemStack(Material.WOODEN_SWORD));
    }

    public void startNextWave() {
        waveQueued = false;
        if (state != GameState.ACTIVE) return;

        World world = Bukkit.getWorld(ARENA_WORLD);
        if (world == null) {
            broadcast("<red>The gauntlet arena is not loaded, the gauntlet is over");
            stopGame();
            return;
        }

        waveNumber++;
        broadcast("<dark_red><b>WAVE " + waveNumber + "</b> <gray>is starting!");

        // Basic logic: spawn zombies equal to waveNumber * 2
        Location spawnLoc = new Location(world, -250, -59, 210);
        for (int i = 0; i < (waveNumber * 2); i++) {
            activeMobs.add(world.spawnEntity(spawnLoc, EntityType.ZOMBIE));
        }
    }

    // Called on every entity death while a game is on
    public void checkWaveComplete() {
        if (state != GameState.ACTIVE || waveQueued) return;

        activeMobs.removeIf(entity -> entity.isDead() || !entity.isValid());
        if (!activeMobs.isEmpty()) return;

        waveQueued = true;
        broadcast("<green>Wave Clear! Next wave in 5 seconds...");
        Bukkit.getScheduler().runTaskLater(plugin, this::startNextWave, WAVE_GAP_TICKS);
    }

    public void handlePlayerDeath(Player player) {
        if (!players.remove(player.getUniqueId())) return;

        broadcast("<red>" + player.getName() + " has fallen!");

        if (players.isEmpty()) {
            broadcast("<red>Everyone has fallen, the gauntlet is over");
            stopGame();
        }
    }

    public GameState getState() {
        return state;
    }

    public void stopGame() {
        state = GameState.WAITING;

        for (Entity entity : activeMobs) {
            if (entity != null && entity.isValid()) {
                entity.remove();
            }
        }

        activeMobs.clear();
        players.clear();
        waveNumber = 0;
        countdown = LOBBY_SECONDS;
    }

    private static void broadcast(String message) {
        Bukkit.broadcast(TextUtils.parse(message));
    }
}
