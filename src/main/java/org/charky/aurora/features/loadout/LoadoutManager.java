package org.charky.aurora.features.loadout;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.charky.aurora.common.util.TextUtils;
import org.charky.aurora.core.DataRegistry;
import org.charky.aurora.core.database.Database;
import org.charky.aurora.core.database.LoadoutStore;
import org.charky.aurora.core.database.LoadoutStore.Stash;
import org.charky.aurora.features.build.BuildManager;
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

// Every player has two loadouts: the main one, played in every world the game is built
// in, and an experimental one for the test worlds that starts from nothing. Admins have
// two more main-world slots, 2 and 3, picked with /aurora loadout. Going from
// one kind of world to the other swaps everything Aurora keeps for the player: the
// inventory and ender chest, backpack, tools, level, Essence, collections, gear, portal
// access, perks, cosmetics, ranks and the tutorial. Build worlds keep whichever is in
// play. Each loadout put away is kept under its own id (Loadout.homeOf), so every
// manager goes on using the player's real id for the one in play. A main loadout that
// starts fresh starts like a first join, tutorial included; an experimental one does not.
// /aurora testworld starts an experimental loadout as a copy of the main one instead
public class LoadoutManager implements Listener {
    private final DataRegistry registry;
    private final Database db;
    private final LoadoutStore store;
    private final PlayerManager players;
    private final BuildManager build;
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
    // Admins who picked a loadout with /aurora loadout: it is played in every world until
    // they pick auto or leave, instead of the one the world plays with
    private final Map<UUID, Loadout> pinned = new ConcurrentHashMap<>();

    public LoadoutManager(DataRegistry registry, Database db, LoadoutStore store, PlayerManager players,
                          BuildManager build, ToolManager tools, List<LoadoutData> holders, Consumer<Player> shown,
                          Consumer<Player> firstJoin) {
        this.registry = registry;
        this.db = db;
        this.store = store;
        this.players = players;
        this.build = build;
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
        check(event.getPlayer());
    }

    // HIGHEST, after a build world has handed back the inventory it put away
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onChangedWorld(PlayerChangedWorldEvent event) {
        check(event.getPlayer());
    }

    // Played the experimental loadout before: it is in play now, or put away
    public boolean hasExperimental(UUID uuid) {
        return activeOf(uuid) == Loadout.EXPERIMENTAL
                || stashes.getOrDefault(uuid, Map.of()).containsKey(Loadout.EXPERIMENTAL);
    }

    // /aurora testworld: to a test world on the experimental loadout. True if it was new
    // and starts as a copy of the main one, false if the player already had one
    public boolean visit(Player player, Location to) {
        boolean copied = !hasExperimental(player.getUniqueId());
        if (copied) copyMainToExperimental(player);
        // A loadout picked with /aurora loadout would keep main in play there
        pinned.remove(player.getUniqueId());
        // The world change switches to it
        player.teleport(to);
        return copied;
    }

    // The main loadout in play becomes the put-away experimental one too: memory, then the
    // rows, queued after the profile's own save, then the inventory and ender chest
    private void copyMainToExperimental(Player player) {
        UUID uuid = player.getUniqueId();
        UUID shadow = Loadout.EXPERIMENTAL.homeOf(uuid);
        player.closeInventory();
        for (LoadoutData holder : holders) holder.copyLoadout(player, shadow);
        players.save(player);
        db.async(() -> store.copy(uuid, shadow));
        putAway(player, Loadout.EXPERIMENTAL);
    }

    // A pick lasts for the session, the next join follows the world again
    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        pinned.remove(event.getPlayer().getUniqueId());
    }

    // /aurora loadout: plays this loadout in every world from now on, null goes back to
    // the one each world plays with. Switches straight away when it differs
    public void pin(Player player, Loadout loadout) {
        if (loadout == null) pinned.remove(player.getUniqueId());
        else pinned.put(player.getUniqueId(), loadout);
        check(player);
    }

    // The loadout picked with /aurora loadout, null when the world decides
    public Loadout pinnedOf(UUID uuid) {
        return pinned.get(uuid);
    }

    private void check(Player player) {
        Loadout pick = pinned.get(player.getUniqueId());
        Loadout next = toPlay(pick != null ? pick : wanted(player.getWorld()), activeOf(player.getUniqueId()));
        if (next != null) switchTo(player, next);
    }

    // The loadout to switch to, null to stay: a build world (wanted null) keeps the one in play
    static Loadout toPlay(Loadout wanted, Loadout active) {
        return wanted == null || wanted == active ? null : wanted;
    }

    // The loadout a world plays with, null for a build world: it keeps the one in play
    private Loadout wanted(World world) {
        if (build.isBuildWorld(world)) return null;
        return Loadout.of(world.getName());
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
            if (target.isMainWorld()) firstJoin.accept(player);
        }
        // So a crash can never bring back the inventory of the loadout put away
        player.saveData();

        TextUtils.send(player, switchMessage(target, fresh));
    }

    static String switchMessage(Loadout target, boolean fresh) {
        return switch (target) {
            case EXPERIMENTAL -> "<aqua>Experimental loadout <gray>- the test worlds have their own progress"
                    + (fresh ? ", starting from nothing" : "") + ". Your main one is kept for when you leave";
            case MAIN -> "<green>Main loadout <gray>- back to your own progress";
            default -> "<yellow>Loadout slot " + target.label() + " <gray>- its own progress"
                    + (fresh ? ", starting from nothing" : "") + ", never on the leaderboards";
        };
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
