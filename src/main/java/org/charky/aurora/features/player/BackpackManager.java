package org.charky.aurora.features.player;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;
import org.charky.aurora.core.DataRegistry;
import org.charky.aurora.core.database.BackpackStore;
import org.charky.aurora.core.database.Database;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class BackpackManager {
    // Every 5 minutes, so a crash loses at most that much mining
    private static final long AUTOSAVE_TICKS = 6000L;

    private Map<UUID, BackpackData> backpacks;
    private final DataRegistry registry;
    private Database db;
    private BackpackStore store;
    private BukkitTask autosave;

    public BackpackManager(DataRegistry registry) {
        this.registry = registry;
    }

    public void loadBackpacks(Database db, BackpackStore store) {
        this.backpacks = store.loadAll(registry);
        this.db = db;
        this.store = store;
    }

    // The copy is made on the main thread where backpacks change, then saved off it
    public void startAutosave(Plugin plugin) {
        this.autosave = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            Map<UUID, Map<String, Long>> copy = snapshot();
            db.async(() -> store.saveAll(copy));
        }, AUTOSAVE_TICKS, AUTOSAVE_TICKS);
    }

    public BackpackData get(UUID uuid) {
        return backpacks.computeIfAbsent(uuid, k -> new BackpackData(registry));
    }

    public void open(Player player) {
        new BackpackMenu(get(player.getUniqueId()), registry).open(player);
    }

    public void clear(UUID uuid) {
        BackpackData data = backpacks.get(uuid);
        if (data != null) data.clear();
    }

    public void shutdown() {
        if (autosave != null) autosave.cancel();
        store.saveAll(snapshot());
    }

    private Map<UUID, Map<String, Long>> snapshot() {
        Map<UUID, Map<String, Long>> copy = new HashMap<>();
        backpacks.forEach((uuid, data) -> copy.put(uuid, new HashMap<>(data.getItems())));
        return copy;
    }
}
