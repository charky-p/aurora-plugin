package org.charky.aurora.features.player;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;
import org.charky.aurora.core.database.Database;
import org.charky.aurora.core.database.PlayerStore;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

// Each online player's profile: loaded on join, saved on quit and every 10 minutes
public class PlayerManager {
    private static final long AUTOSAVE_TICKS = 12000L;

    private final ConcurrentMap<UUID, PlayerData> profiles = new ConcurrentHashMap<>();
    private final Plugin plugin;
    private final Database db;
    private final PlayerStore store;
    private final BukkitTask autosave;

    public PlayerManager(Plugin plugin, Database db, PlayerStore store) {
        this.plugin = plugin;
        this.db = db;
        this.store = store;

        this.autosave = Bukkit.getScheduler().runTaskTimer(plugin, () -> db.async(() -> {
            store.saveAll(profiles);
            plugin.getLogger().info("Auto-saved all player data to SQLite.");
        }), AUTOSAVE_TICKS, AUTOSAVE_TICKS);
    }

    // Loads run on the database thread after any save still queued from a quit,
    // so a quick rejoin can never read the old row. onLoaded runs on the main thread
    public void handleJoin(Player player, Runnable onLoaded) {
        UUID uuid = player.getUniqueId();
        db.async(() -> {
            PlayerData data = store.load(uuid);

            Bukkit.getScheduler().runTask(plugin, () -> {
                // Left again before the load came back
                if (!player.isOnline()) return;

                profiles.put(uuid, data);
                onLoaded.run();
            });
        });
    }

    // The profile leaves the map here on the main thread, then its own copy is saved,
    // so a late save can never remove the profile of a player who already rejoined
    public void handleQuit(Player player) {
        UUID uuid = player.getUniqueId();
        PlayerData data = profiles.remove(uuid);
        if (data != null) db.async(() -> store.save(uuid, data));
    }

    // Null while the profile is still loading
    public PlayerData get(Player player) {
        return profiles.get(player.getUniqueId());
    }

    public void addMined(Player player) {
        PlayerData data = get(player);
        if (data != null) data.addMined(1);
    }

    public void addKill(Player player) {
        PlayerData data = get(player);
        if (data != null) data.addKills(1);
    }

    public void addEssence(Player player, int essence) {
        PlayerData data = get(player);
        if (data != null) data.addEssence(essence);
    }

    public void shutdown() {
        autosave.cancel();
        store.saveAll(profiles);
    }
}
