package org.charky.aurora.features.gauntlet;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.charky.aurora.common.util.TextUtils;
import org.charky.aurora.core.Aurora;

import java.util.*;

public class GauntletManager {
    private final Aurora plugin;
    private final Set<UUID> players = new HashSet<>();
    private GameState state = GameState.WAITING;
    private int waveNumber = 0;
    private int countdown = 15;
    private final List<Entity> activeMobs = new ArrayList<>();

    public GauntletManager(Aurora plugin) {
        this.plugin = plugin;
    }

    public enum GameState {
        WAITING, STARTING, ACTIVE, CLEANUP
    }

    public void joinQueue(Player player) {
        if (state == GameState.ACTIVE) {
            TextUtils.error(player, "Gauntlet is already in progress");
            return;
        }

        players.add(player.getUniqueId());
        Bukkit.broadcast(Component.text(player.getName() + " joined the gauntlet"));

        if (state == GameState.WAITING) {
            startLobbyCooldown();
        }
    }

    public void startLobbyCooldown() {
        state = GameState.STARTING;
        new BukkitRunnable() {
            @Override
            public void run() {
                if (countdown <= 0) {
                    startGame();
                    this.cancel();
                    return;
                }
                if (countdown % 10 == 0 || countdown <= 5) {
                    Bukkit.broadcastMessage("§eGauntlet starting in §f" + countdown + "s...");
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
        waveNumber++;
        Bukkit.broadcastMessage("§4§lWAVE " + waveNumber + " §7is starting!");

        // Basic logic: spawn zombies equal to waveNumber * 2
        Location spawnLoc = new Location(Bukkit.getWorld("world"), -250, -59, 210);
        for (int i = 0; i < (waveNumber * 2); i++) {
            Entity mob = spawnLoc.getWorld().spawnEntity(spawnLoc, EntityType.ZOMBIE);
            activeMobs.add(mob);
        }
    }

    // You will call this from an EntityDeathEvent listener!
    public void checkWaveComplete() {
        activeMobs.removeIf(entity -> entity.isDead() || !entity.isValid());

        if (activeMobs.isEmpty()) {
            Bukkit.broadcastMessage("§aWave Clear! Next wave in 5 seconds...");

            new BukkitRunnable() {
                @Override
                public void run() {
                    startNextWave();
                }
            }.runTaskLater(plugin, 100);
        }
    }

    public void handlePlayerDeath(Player player) {
        if (!players.contains(player.getUniqueId())) return;

        players.remove(player.getUniqueId());
        Bukkit.broadcastMessage("§c" + player.getName() + " has fallen!");

        if (players.isEmpty()) {
            Bukkit.broadcast(TextUtils.parse("<red>Everyone has fallen, the gauntlet is over"));
            stopGame();
        }
    }

    public GameState getState() {
        return state;
    }

    public void stopGame() {
        state =  GameState.WAITING;

        for (Entity entity : activeMobs) {
            if (entity != null && entity.isValid()) {
                entity.remove();
            }
        }
        activeMobs.clear();

        waveNumber = 0;
        countdown = 15;
    }
}
