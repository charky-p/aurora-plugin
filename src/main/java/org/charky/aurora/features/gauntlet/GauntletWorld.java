package org.charky.aurora.features.gauntlet;

import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.persistence.PersistentDataType;
import org.charky.aurora.common.util.DataKeys;
import org.charky.aurora.common.util.TextUtils;
import org.charky.aurora.core.Aurora;
import org.charky.aurora.core.database.Database;
import org.charky.aurora.core.database.GauntletStore;
import org.charky.aurora.features.build.BuildManager;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

// The Gauntlet's world kept apart from the main game. Walking in puts the player's own
// inventory away and hands out their setup, walking out gives it back, so nothing
// crosses over either way; a player who logged out here comes back in the lobby. New
// players are shown how to start, and walking into the start portal starts a run.
// Main-game gear and tools are taken away and the ender chest stays shut. Players only
// hurt each other in the PvP zone or a duel, and dying here costs nothing
public class GauntletWorld implements Listener {
    private final Aurora plugin;
    private final GauntletManager manager;
    private final Database db;
    private final GauntletStore store;
    private final BuildManager build;
    private final GauntletArena arena;
    // Died in the gauntlet's world, to come back in its lobby
    private final Set<UUID> diedHere = new HashSet<>();

    public GauntletWorld(Aurora plugin, GauntletManager manager, Database db, GauntletStore store, BuildManager build) {
        this.plugin = plugin;
        this.manager = manager;
        this.db = db;
        this.store = store;
        this.build = build;
        this.arena = manager.arena();
    }

    // Every second, from the manager
    void everySecond() {
        sweepAll();
        sweepOutside();
        hintNewPlayers();
        clearLag();
    }

    // ---- Coming and going ----

    // From anywhere: to the lobby. Null if it worked, otherwise why not
    String sendToLobby(Player player) {
        Location lobby = arena.lobby();
        if (lobby == null) return "The Gauntlet is not open yet";
        if (!manager.data().ready()) return "The Gauntlet's data did not load";
        player.teleport(lobby);
        return null;
    }

    void leave(Player player) {
        manager.endRun(player);
        Location spawn = plugin.getMainSpawn();
        player.teleport(spawn != null ? spawn : Bukkit.getWorlds().getFirst().getSpawnLocation());
    }

    // Before the loadouts swap (NORMAL), so a loadout put away on the way out holds
    // the player's own inventory, not their gauntlet gear
    @EventHandler(priority = EventPriority.LOWEST)
    public void onLeaveWorld(PlayerChangedWorldEvent event) {
        Player player = event.getPlayer();
        if (!arena.isGauntletWorld(event.getFrom()) || build.isBuildWorld(event.getFrom())) return;
        if (manager.isHere(player)) return;
        manager.endRun(player);
        // A party is for the Gauntlet, leaving it leaves the party
        GauntletParty party = manager.party();
        if (party != null && party.inParty(player)) party.leave(player);
        giveBack(player);
        manager.refreshBoard(player);
    }

    // After the loadouts swap, so what is put away is the loadout coming in
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onEnterWorld(PlayerChangedWorldEvent event) {
        Player player = event.getPlayer();
        if (!manager.isHere(player) || (arena.isGauntletWorld(event.getFrom()) && !build.isBuildWorld(event.getFrom()))) return;
        putAway(player);
        manager.giveKit(player);
        manager.refreshBoard(player);
        welcome(player);
    }

    // A player who logged out here comes back in the lobby, never inside an arena copy.
    // One who is somewhere else with an inventory still put away gets it back
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (!player.isOnline()) return;
            manager.weekly().notice(player);
            manager.daily().notice(player);
            manager.achievements().check(player.getUniqueId());
            if (manager.isHere(player)) {
                Location lobby = arena.lobby();
                if (lobby != null) player.teleport(lobby);
                manager.giveKit(player);
                welcome(player);
            } else if (!build.isBuildWorld(player.getWorld())) {
                giveBack(player);
            }
        }, 5L);
    }

    // How to get going, the moment they arrive: a title, and buttons in chat
    private void welcome(Player player) {
        String how = arena.hasStart() ? "Walk into the portal, or use Start a Run in your hotbar, to start a run"
                : "Use Start a Run in your hotbar to start a run";
        player.showTitle(Title.title(TextUtils.parse("<gradient:#FF416C:#FF4B2B><b>THE GAUNTLET</b></gradient>"),
                TextUtils.parse("<gray>" + how)));
        TextUtils.send(player, "");
        TextUtils.send(player, "<gradient:#FF416C:#FF4B2B><b>THE GAUNTLET</b></gradient> <gray>Climb the tower floor by floor, "
                + "spend Shards on your gear, climb higher.");
        TextUtils.send(player, "<gray>" + how + ", or click: "
                + "<click:run_command:'/gauntlet start'><hover:show_text:'<gray>Start a run now'><green><b>[START A RUN]</b></hover></click> "
                + "<click:run_command:'/gauntlet forge'><hover:show_text:'<gray>Upgrade your gear'><gold><b>[FORGE]</b></hover></click> "
                + "<click:run_command:'/gauntlet loadout'><hover:show_text:'<gray>Lay out your gear for a run'><gold><b>[LOADOUT]</b></hover></click> "
                + "<click:run_command:'/gauntlet'><hover:show_text:'<gray>Everything else'><yellow><b>[MENU]</b></hover></click>");
        GauntletChallenges challenges = manager.challenges();
        int done = challenges.doneToday(player);
        int total = challenges.today().size();
        if (total > 0) TextUtils.send(player, "<gray>Daily challenges: <white>" + done + "/" + total + "</white> done "
                + "<click:run_command:'/gauntlet'><hover:show_text:'<gray>See them in the menu'><yellow>[SEE]</hover></click>");
        TextUtils.send(player, "<dark_gray>Your own inventory is kept for when you leave, /gauntlet leave.");
        TextUtils.send(player, "");
    }

    // Until their first run, players standing in the lobby are told how to start
    private void hintNewPlayers() {
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            if (!manager.isHere(player) || manager.inRun(player) || manager.isWatching(player) || manager.profile(player).runs > 0) continue;
            if (manager.pvp() != null && manager.pvp().inZone(player)) continue;
            player.sendActionBar(TextUtils.parse(arena.hasStart()
                    ? "<green><b>Walk into the portal</b> <gray>or right click <green>Start a Run</green> to start your first run"
                    : "<green><b>Right click Start a Run</b> <gray>in your hotbar to start your first run"));
        }
    }

    // Walking into the start portal starts a run
    @EventHandler(ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        Location from = event.getFrom(), to = event.getTo();
        if (from.getBlockX() == to.getBlockX() && from.getBlockY() == to.getBlockY() && from.getBlockZ() == to.getBlockZ()) return;
        Player player = event.getPlayer();
        GauntletRuns runs = manager.runs();
        if (!manager.isHere(player) || manager.inRun(player) || !arena.inStart(to) || arena.inStart(from) || runs == null) return;
        String problem = runs.startFlow(player);
        if (problem != null) TextUtils.error(player, problem);
    }

    // The inventory brought in, kept for when they leave. Anything of the Gauntlet in it
    // (left over from a way out that missed giving it back) is not kept
    private void putAway(Player player) {
        UUID uuid = player.getUniqueId();
        ItemStack[] contents = Arrays.stream(player.getInventory().getContents())
                .map(item -> item == null || GauntletSetup.isGauntletItem(item) ? ItemStack.empty() : item).toArray(ItemStack[]::new);
        byte[] items = ItemStack.serializeItemsAsBytes(contents);
        // Saved before the inventory is cleared, a crash in between loses nothing
        store.saveInventory(uuid, items);
        player.getInventory().clear();
    }

    // The inventory brought in, back, and nothing of the Gauntlet's kept, even when no
    // inventory was put away
    private void giveBack(Player player) {
        UUID uuid = player.getUniqueId();
        byte[] items = store.loadInventory(uuid);
        if (items == null) {
            stripGauntlet(player);
            return;
        }
        player.getInventory().clear();
        player.getInventory().setContents(ItemStack.deserializeItemsFromBytes(items));
        stripGauntlet(player);
        db.async(() -> store.removeInventory(uuid));
    }

    // Anything of the Gauntlet out of the inventory, true if there was some
    private static boolean stripGauntlet(Player player) {
        PlayerInventory inventory = player.getInventory();
        boolean found = false;
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            if (!GauntletSetup.isGauntletItem(inventory.getItem(slot))) continue;
            inventory.setItem(slot, null);
            found = true;
        }
        if (GauntletSetup.isGauntletItem(player.getItemOnCursor())) {
            player.setItemOnCursor(null);
            found = true;
        }
        if (found) player.updateInventory();
        return found;
    }

    // Outside the Gauntlet's world (build worlds aside) its gear and kit are taken away
    private void sweepOutside() {
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            if (manager.isHere(player) || build.isBuildWorld(player.getWorld())) continue;
            if (stripGauntlet(player)) TextUtils.send(player, "<gray>Your Gauntlet gear stays in the Gauntlet");
        }
    }

    // ---- Keeping the world apart ----

    // Gear from the main game would carry its stats into the gauntlet (taken out of
    // the Armory here, say), so anything with Aurora stats that is not gauntlet gear
    // is taken away, as are main-game tools, but for the starting kit's own (the grapple)
    private void sweepAll() {
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            if (!manager.isHere(player)) continue;
            PlayerInventory inventory = player.getInventory();
            for (int slot = 0; slot < inventory.getSize(); slot++) {
                ItemStack item = inventory.getItem(slot);
                if (item == null || !item.hasItemMeta() || GauntletGear.gearId(item) != null || GauntletGear.isKit(item)) continue;
                var pdc = item.getItemMeta().getPersistentDataContainer();
                if (pdc.has(DataKeys.ENCHANT_KEY, PersistentDataType.TAG_CONTAINER)
                        || pdc.has(DataKeys.TOOL_KEY, PersistentDataType.STRING)) {
                    inventory.setItem(slot, null);
                }
            }
        }
    }

    // ---- Clear-lag ----

    // Seconds since the ground was last cleared, and whether this round's warning went out
    private int sinceClear;
    private boolean warned;

    // Every clearlag_minutes what lies on the ground outside runs and duels is cleared
    // (GauntletArena.groundClutter), clearlag_warning seconds after a warning to everyone
    // it reaches. With nothing on the ground at the warning, that round clears nothing
    private void clearLag() {
        GauntletData data = manager.data();
        int every = (int) Math.round(data.setting("clearlag_minutes", 5) * 60);
        if (every <= 0) return;
        int left = every - ++sinceClear;
        int warning = (int) Math.round(data.setting("clearlag_warning", 30));
        if (left == warning && !arena.groundClutter().isEmpty()) {
            warned = true;
            tellOutside("<yellow>Items on the ground are cleared in <white>" + warning + " seconds");
        }
        if (left > 0) return;
        sinceClear = 0;
        if (!warned) return;
        warned = false;
        List<Entity> clutter = arena.groundClutter();
        clutter.forEach(Entity::remove);
        if (!clutter.isEmpty()) tellOutside("<gray>Cleared <white>" + clutter.size() + "</white> items from the ground");
    }

    // Everyone in the lobby and the PvP zone, where a clear reaches
    private void tellOutside(String message) {
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            if (manager.isHere(player) && !manager.inRun(player) && !manager.inDuel(player)) TextUtils.send(player, message);
        }
    }

    // The ender chest holds main-game things
    @EventHandler(ignoreCancelled = true)
    public void onOpen(InventoryOpenEvent event) {
        if (event.getPlayer() instanceof Player player && manager.isHere(player)
                && event.getInventory().getType() == InventoryType.ENDER_CHEST) {
            event.setCancelled(true);
            TextUtils.error(player, "Your ender chest can't be opened in the Gauntlet");
        }
    }

    // Players only hurt each other in the PvP zone, both in it and neither in a run
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPvp(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim) || !manager.isHere(victim)) return;
        Player attacker = event.getDamager() instanceof Player p ? p
                : event.getDamager() instanceof Projectile projectile
                && projectile.getShooter() instanceof Player shooter ? shooter : null;
        if (attacker == null) return;
        // Duelists fight each other and nobody else
        GauntletDuels duels = manager.duels();
        if (duels != null && duels.areDueling(victim, attacker)) return;
        String refused = null;
        if (manager.inDuel(victim) || manager.inDuel(attacker)) {
            refused = manager.inDuel(attacker) ? "You can only hit your duel opponent" : victim.getName() + " is in a duel";
        } else if (manager.inRun(victim) || manager.inRun(attacker)) refused = "No PvP during a run";
        else if (!arena.inPvp(attacker.getLocation())) refused = "You can only fight players in the PvP zone";
        else if (!arena.inPvp(victim.getLocation())) refused = victim.getName() + " is outside the PvP zone";
        if (refused == null) return;
        event.setCancelled(true);
        attacker.sendActionBar(TextUtils.parse("<red>" + refused));
    }

    // Dying here costs nothing, the gear is kept, and they come back in the lobby
    @EventHandler(priority = EventPriority.HIGH)
    public void onDeath(PlayerDeathEvent event) {
        Player player = event.getPlayer();
        if (!manager.isHere(player)) return;
        event.setKeepInventory(true);
        event.getDrops().clear();
        event.setKeepLevel(true);
        event.setDroppedExp(0);
        diedHere.add(player.getUniqueId());
    }

    // Back in the lobby with their setup as they left it, whatever the fight used up
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        if (!diedHere.remove(player.getUniqueId())) return;
        Location lobby = arena.lobby();
        if (lobby != null) event.setRespawnLocation(lobby);
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            if (player.isOnline() && manager.isHere(player)) manager.giveKit(player);
        });
    }
}
