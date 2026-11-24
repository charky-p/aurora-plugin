package org.charky.aurora.features.loadout;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.charky.aurora.common.util.TextUtils;
import org.charky.aurora.core.DataRegistry;
import org.charky.aurora.core.database.Database;
import org.charky.aurora.core.database.LoadoutStore;
import org.charky.aurora.core.database.LoadoutStore.Stash;
import org.charky.aurora.features.player.PlayerManager;
import org.charky.aurora.features.player.StarterKit;
import org.charky.aurora.features.tool.ToolManager;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

// Every player has one loadout, the main one, in every world (a build world puts the
// inventory away on its own, see BuildManager). There used to be more: an experimental
// one for the test worlds and admin slots 2 and 3. Whoever still has one of those in play
// is switched back to main when they join, and never off it again. A switch swaps
// everything Aurora keeps for the player: the inventory and ender chest, backpack, tools,
// level, Essence, collections, gear, portal access, perks, cosmetics, ranks and the
// tutorial. The one put away stays under its own id (Loadout.homeOf), off the leaderboards
public class LoadoutManager implements Listener {
    private final DataRegistry registry;
    private final Database db;
    private final LoadoutStore store;
    private final PlayerManager players;
    private final ToolManager tools;
    // Everything holding per-player data of a loadout in memory
    private final List<LoadoutData> holders;
    // What a loaded profile is shown in: the scoreboard, the level bar, the tab list
    private final Consumer<Player> shown;
    // Starts the tutorial
    private final Consumer<Player> firstJoin;
    // Players who have switched before, everyone else plays MAIN
    private final Map<UUID, Loadout> active;
    private final Map<UUID, Map<Loadout, Stash>> stashes;

    public LoadoutManager(DataRegistry registry, Database db, LoadoutStore store, PlayerManager players,
                          ToolManager tools, List<LoadoutData> holders, Consumer<Player> shown,
                          Consumer<Player> firstJoin) {
        this.registry = registry;
        this.db = db;
        this.store = store;
        this.players = players;
        this.tools = tools;
        this.holders = List.copyOf(holders);
        this.shown = shown;
        this.firstJoin = firstJoin;
        int moved = store.migrate();
        if (moved > 0) org.bukkit.Bukkit.getLogger().info("[Aurora] Loadouts: " + moved + " players moved to per-slot ids");
        // Read off the main thread too, by the leaderboards
        this.active = new ConcurrentHashMap<>(store.loadActive());
        this.stashes = store.loadStashes();
    }

    public Loadout activeOf(UUID uuid) {
        return active.getOrDefault(uuid, Loadout.MAIN);
    }

    // HIGHEST, after the join spawn has moved them and their profile has started loading
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (activeOf(player.getUniqueId()) != Loadout.MAIN) switchTo(player, Loadout.MAIN);
    }

    private void switchTo(Player player, Loadout target) {
        UUID uuid = player.getUniqueId();
        Loadout from = activeOf(uuid);
        UUID away = from.homeOf(uuid);
        UUID in = target.homeOf(uuid);
        // A menu open now shows the loadout being put away, and closing it may still change it
        player.closeInventory();

        putAway(player, from);
        boolean fresh = !takeOut(player, target);

        // Memory first, then the rows behind it, queued after every save already made for
        // the loadout being put away and before the profile loads again. The one in play
        // goes to its own id, then the target comes from its id to the player's
        for (LoadoutData holder : holders) {
            holder.swapLoadout(player, away);
            holder.swapLoadout(player, in);
        }
        active.put(uuid, target);
        players.reload(player, () -> store.switchTo(uuid, away, in, target), () -> shown.accept(player));

        // Tools are drawn from levels, which just changed
        tools.refresh(player);
        if (fresh) {
            StarterKit.give(player, registry);
            tools.giveStarters(player);
            firstJoin.accept(player);
        }
        // So a crash can never bring back the inventory of the loadout put away
        player.saveData();

        TextUtils.send(player, "<green>Main loadout <gray>- there is only one loadout now, your own progress");
    }

    private void putAway(Player player, Loadout loadout) {
        UUID uuid = player.getUniqueId();
        Stash stash = new Stash(serialize(player.getInventory().getContents()),
                serialize(player.getEnderChest().getContents()));
        stashes.computeIfAbsent(uuid, k -> new HashMap<>()).put(loadout, stash);
        db.async(() -> store.saveStash(uuid, loadout, stash));
    }

    // False if the loadout was never put away: it starts empty
    private boolean takeOut(Player player, Loadout loadout) {
        Stash stash = stashes.getOrDefault(player.getUniqueId(), Map.of()).get(loadout);
        player.getInventory().clear();
        player.getEnderChest().clear();
        if (stash != null) {
            player.getInventory().setContents(ItemStack.deserializeItemsFromBytes(stash.items()));
            player.getEnderChest().setContents(ItemStack.deserializeItemsFromBytes(stash.enderChest()));
        }
        player.updateInventory();
        return stash != null;
    }

    private static byte[] serialize(ItemStack[] contents) {
        return ItemStack.serializeItemsAsBytes(Arrays.stream(contents)
                .map(item -> item == null ? ItemStack.empty() : item).toArray(ItemStack[]::new));
    }
}
