package org.charky.aurora.features.player;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;
import org.charky.aurora.core.database.Database;
import org.charky.aurora.core.database.PlayerStore;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

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

    public record Standing(UUID uuid, int level, long xp) {
    }

    // The top levels, handed to the callback on the main thread. Players online
    // count with what they have right now, not what was last saved
    public void topLevels(int size, Consumer<List<Standing>> then) {
        db.async(() -> {
            // Extra rows, in case some of them are online and have since fallen behind
            List<Object[]> saved = store.topLevels(size + profiles.size());

            Bukkit.getScheduler().runTask(plugin, () -> {
                Map<UUID, Standing> standings = new HashMap<>();
                for (Object[] row : saved) {
                    UUID uuid = (UUID) row[0];
                    standings.put(uuid, new Standing(uuid, (int) row[1], (long) row[2]));
                }
                profiles.forEach((uuid, data) -> standings.put(uuid, new Standing(uuid, data.getLevel(), data.getXp())));

                then.accept(standings.values().stream()
                        .sorted(Comparator.comparingInt(Standing::level).thenComparingLong(Standing::xp).reversed())
                        .limit(size)
                        .toList());
            });
        });
    }

    // One place on a leaderboard
    public record Entry(UUID uuid, long value, long tiebreak) {
    }

    // The top of one stat and where the asking player stands, handed to the callback on
    // the main thread. Players online count with what they have right now
    public void top(LeaderboardStat stat, int size, UUID asking, BiConsumer<List<Entry>, Integer> then) {
        db.async(() -> {
            List<long[]> saved = store.top(stat.column(), stat.tiebreak(), size + profiles.size());
            PlayerData mine = profiles.get(asking);
            int ahead = mine == null ? -1 : store.ahead(stat.column(), stat.of(mine));

            Bukkit.getScheduler().runTask(plugin, () -> {
                Map<UUID, Entry> entries = new HashMap<>();
                for (long[] row : saved) {
                    UUID uuid = new UUID(row[0], row[1]);
                    entries.put(uuid, new Entry(uuid, row[2], row[3]));
                }
                profiles.forEach((uuid, data) -> entries.put(uuid, new Entry(uuid, stat.of(data), stat.tiebreakOf(data))));
                List<Entry> top = entries.values().stream()
                        .sorted(Comparator.comparingLong(Entry::value).thenComparingLong(Entry::tiebreak).reversed())
                        .limit(size)
                        .toList();
                then.accept(top, ahead < 0 ? -1 : ahead + 1);
            });
        });
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
