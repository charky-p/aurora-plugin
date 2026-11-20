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
import org.charky.aurora.common.util.Swaps;
import org.charky.aurora.features.loadout.LoadoutData;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

// Collections count every resource a player has ever mined or looted. Passing a
// tier pays Essence, see CollectionTiers. Keys are not collected, only resources
public class CollectionManager implements LoadoutData {
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

            int base = registry.getCollectionBase(drop.getId());
            int essence = CollectionTiers.reward(base, before, after);
            if (essence > 0) reward(player, drop.getId(), CollectionTiers.tier(base, after), essence);
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

    public void openGear(Player player) {
        new CollectionMenu(this, registry).onGear().open(player);
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

    // Every total back to nothing
    public void reset(UUID uuid) {
        collections.remove(uuid);
        changed.remove(uuid);
        db.async(() -> store.deleteAll(uuid));
    }

    // A copy for a new loadout of the one in play, see LoadoutManager
    @Override
    public void copyLoadout(Player player, UUID other) {
        Swaps.copy(collections, player.getUniqueId(), other, HashMap::new);
        // Saved under its new id with the next autosave
        changed.add(other);
    }

    // Trades what it holds for the player with their other loadout, see LoadoutManager
    @Override
    public void swapLoadout(Player player, UUID other) {
        Swaps.keys(collections, player.getUniqueId(), other);
        // Totals not saved yet are saved under whichever id they now belong to
        Swaps.members(changed, player.getUniqueId(), other);
    }
}
