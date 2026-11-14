package org.charky.aurora.features.collection;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;
import org.charky.aurora.common.loot.MenuItem;
import org.charky.aurora.common.util.TextUtils;
import org.charky.aurora.core.DataRegistry;
import org.charky.aurora.core.database.CollectionStore;
import org.charky.aurora.core.database.Database;
import org.charky.aurora.features.player.PlayerManager;
import org.charky.aurora.features.player.ScoreboardManager;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

// Collections count every resource a player has ever mined or looted. Passing a
// tier pays Essence, see CollectionTiers. Keys are not collected, only resources
public class CollectionManager {
    // Every 5 minutes like the backpack, so a crash loses at most that much
    private static final long AUTOSAVE_TICKS = 6000L;

    private final DataRegistry registry;
    private final Database db;
    private final CollectionStore store;
    private final PlayerManager players;
    private final ScoreboardManager scoreboard;
    private final Map<UUID, Map<String, Long>> collections;
    // Players whose totals changed since the last save
    private final Set<UUID> changed = new HashSet<>();
    private final BukkitTask autosave;

    public CollectionManager(Plugin plugin, DataRegistry registry, Database db, CollectionStore store,
                             PlayerManager players, ScoreboardManager scoreboard) {
        this.registry = registry;
        this.db = db;
        this.store = store;
        this.players = players;
        this.scoreboard = scoreboard;
        this.collections = store.loadAll();
        // The copy is made on the main thread where totals change, then saved off it
        this.autosave = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            Map<UUID, Map<String, Long>> copy = takeChanged();
            db.async(() -> store.saveAll(copy));
        }, AUTOSAVE_TICKS, AUTOSAVE_TICKS);
    }

    public long get(UUID uuid, String itemId) {
        return collections.getOrDefault(uuid, Map.of()).getOrDefault(itemId, 0L);
    }

    // Counts what was just mined or looted, and pays for any tier it passed
    public void add(Player player, List<MenuItem> drops) {
        for (MenuItem drop : drops) {
            if (drop.getAmount() <= 0 || !registry.isLootable(drop.getId())) continue;

            Map<String, Long> totals = collections.computeIfAbsent(player.getUniqueId(), k -> new HashMap<>());
            long before = totals.getOrDefault(drop.getId(), 0L);
            long after = before + drop.getAmount();
            totals.put(drop.getId(), after);
            changed.add(player.getUniqueId());

            int essence = CollectionTiers.reward(before, after);
            if (essence > 0) reward(player, drop.getId(), CollectionTiers.tier(after), essence);
        }
    }

    private void reward(Player player, String itemId, int tier, int essence) {
        players.addEssence(player, essence);
        scoreboard.updateBoard(player);

        ItemStack item = registry.getAllItems().get(itemId);
        Component name = item == null ? Component.text(itemId) : item.effectiveName();
        player.sendMessage(TextUtils.parse("<gold><b>COLLECTION</b> ").append(name)
                .append(TextUtils.parse(" <yellow>" + CollectionTiers.numeral(tier)
                        + " <dark_gray>(<gold>+" + essence + " Essence<dark_gray>)")));
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.6f, 1.4f);
    }

    public void open(Player player) {
        new CollectionMenu(this, registry).open(player);
    }

    public void shutdown() {
        autosave.cancel();
        store.saveAll(takeChanged());
    }

    private Map<UUID, Map<String, Long>> takeChanged() {
        Map<UUID, Map<String, Long>> copy = new HashMap<>();
        for (UUID uuid : changed) copy.put(uuid, new HashMap<>(collections.getOrDefault(uuid, Map.of())));
        changed.clear();
        return copy;
    }
}
