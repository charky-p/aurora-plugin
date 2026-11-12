package org.charky.aurora.features.trader;

import org.bukkit.*;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.charky.aurora.core.Aurora;
import org.charky.aurora.core.DataRegistry;
import org.charky.aurora.core.database.TraderStore;

import java.util.*;

public class TraderManager {
    // How often spots are checked for a missing trader, 2 seconds
    private static final long CHECK_TICKS = 40;

    private final Aurora plugin;
    private final DataRegistry registry;
    private final TraderStore store;
    // Sorted by id, so lists come out in the order traders were placed
    private final Map<Integer, TraderSpot> spots = new TreeMap<>();
    // The entity standing at each spot right now
    private final Map<Integer, UUID> spawned = new HashMap<>();
    // Spots already warned about, so a broken spot logs once instead of every check
    private final Set<Integer> warned = new HashSet<>();
    private BukkitTask task;

    public TraderManager(Aurora plugin, TraderStore store) {
        this.plugin = plugin;
        this.registry = plugin.getRegistry();
        this.store = store;

        for (TraderSpot spot : store.loadAll()) {
            spots.put(spot.id(), spot);
        }
    }

    // Traders are not saved with their chunk, so a spot whose chunk loads, or whose
    // trader was killed, gets a new one on the next check
    public void startRespawning() {
        if (task != null) task.cancel();
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::respawnMissing, 1, CHECK_TICKS);
    }

    private void respawnMissing() {
        for (TraderSpot spot : spots.values()) {
            if (!spot.isChunkLoaded()) continue;

            UUID uuid = spawned.get(spot.id());
            Entity entity = uuid == null ? null : Bukkit.getEntity(uuid);
            if (entity != null && entity.isValid()) continue;

            spawnAt(spot);
        }
    }

    private void spawnAt(TraderSpot spot) {
        Trader trader = registry.getTrader(spot.traderId());
        Location location = spot.toLocation();
        if (trader == null || location == null) {
            if (warned.add(spot.id())) {
                plugin.getLogger().warning("Trader spot " + spot.describe() + ": "
                        + (trader == null ? "trader is not in the registry" : "world is not loaded"));
            }
            return;
        }

        Entity entity = trader.spawn(location, spot.id());
        if (entity != null) spawned.put(spot.id(), entity.getUniqueId());
    }

    public boolean traderExists(String traderId) {
        return registry.hasTrader(traderId);
    }

    // Saves the spot and spawns its trader. Keeps the location's yaw, so a trader
    // placed where you stand can face you. Null if the save failed
    public TraderSpot placeTrader(String traderId, Location location) {
        if (!traderExists(traderId) || location.getWorld() == null) return null;

        int id = store.add(traderId, location.getWorld().getName(),
                location.getX(), location.getY(), location.getZ(), location.getYaw());
        if (id < 0) return null;

        TraderSpot spot = new TraderSpot(id, traderId, location.getWorld().getName(),
                location.getX(), location.getY(), location.getZ(), location.getYaw());
        spots.put(id, spot);
        spawnAt(spot);
        return spot;
    }

    // Removes the spot for good, its trader with it
    public boolean removeSpot(int id) {
        TraderSpot spot = spots.remove(id);
        if (spot == null) return false;

        despawn(id);
        warned.remove(id);
        store.remove(id);
        return true;
    }

    // After a /refresh, so traders pick up new names and types on the next check
    public void respawnAll() {
        for (int id : new ArrayList<>(spawned.keySet())) {
            despawn(id);
        }
        warned.clear();
    }

    private void despawn(int id) {
        UUID uuid = spawned.remove(id);
        if (uuid == null) return;

        Entity entity = Bukkit.getEntity(uuid);
        if (entity != null) entity.remove();
    }

    public TraderSpot getSpot(int id) {
        return spots.get(id);
    }

    public List<TraderSpot> getSpots() {
        return new ArrayList<>(spots.values());
    }

    public List<String> getSpotIds() {
        return spots.keySet().stream().map(String::valueOf).toList();
    }

    public List<String> getTraders() {
        return registry.getTraders();
    }

    public void handleTrade(Player player, String traderId) {
        Trader trader = registry.getTrader(traderId);
        if (trader == null) return;

        this.handleMenu(player, trader.getMenuId());
    }

    public void handleMenu(Player player, String menuId) {
        TraderNode menu = registry.getGui(menuId);
        if (menu == null) return;
        menu.onClick(player);
    }

    public void shutdown() {
        if (task != null) task.cancel();
        for (int id : new ArrayList<>(spawned.keySet())) {
            despawn(id);
        }
    }
}
