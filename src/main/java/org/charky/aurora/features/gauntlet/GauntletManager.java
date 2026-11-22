package org.charky.aurora.features.gauntlet;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
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
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitTask;
import org.charky.aurora.common.util.DataKeys;
import org.charky.aurora.common.util.TextUtils;
import org.charky.aurora.core.Aurora;
import org.charky.aurora.core.DataRegistry;
import org.charky.aurora.core.database.Database;
import org.charky.aurora.core.database.GauntletStore;
import org.charky.aurora.features.build.BuildManager;
import org.charky.aurora.features.cosmetic.CosmeticManager;
import org.charky.aurora.features.player.PlayerManager;
import org.charky.aurora.features.player.ScoreboardManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

// The gauntlet as a game of its own beside the main one, in its own world. Walking in
// puts the player's inventory away and hands out their gauntlet gear; walking out gives
// it back, so nothing crosses over either way. What carries over is the gauntlet's own
// progress (Shards, Cores, gear levels, best floor) and what milestones pay into the
// main game: Essence and cosmetics. Gear is bought and upgraded in the forge with
// Shards and Cores from runs (GauntletRuns). PvP is off in the world except inside
// its PvP zone, where players fight with their gauntlet gear
public class GauntletManager implements Listener {
    private final Aurora plugin;
    private final Database db;
    private final GauntletStore store;
    private final PlayerManager players;
    private final ScoreboardManager scoreboard;
    private final CosmeticManager cosmetics;
    private final BuildManager build;
    private final GauntletArena arena;
    private final Map<UUID, GauntletProfile> profiles = new ConcurrentHashMap<>();
    // Died in the gauntlet's world, to come back in its lobby
    private final Set<UUID> diedHere = new HashSet<>();
    private GauntletRuns runs;
    private BukkitTask sweep;

    public GauntletManager(Aurora plugin, Database db, GauntletStore store, PlayerManager players,
                           ScoreboardManager scoreboard, CosmeticManager cosmetics, BuildManager build) {
        this.plugin = plugin;
        this.db = db;
        this.store = store;
        this.players = players;
        this.scoreboard = scoreboard;
        this.cosmetics = cosmetics;
        this.build = build;
        this.arena = new GauntletArena(db, store, plugin.getDataFolder(), plugin.getLogger());
        profiles.putAll(store.loadAll());
    }

    public void setRuns(GauntletRuns runs) {
        this.runs = runs;
    }

    public void start() {
        sweep = plugin.getServer().getScheduler().runTaskTimer(plugin, this::sweepAll, 20L, 20L);
        scoreboard.setOverride(this::sidebar);
    }

    // In the Gauntlet's world the sidebar shows its Shards, Cores and best floor, and
    // the run in progress. Elsewhere the main game's
    private List<Component> sidebar(Player player) {
        if (!isHere(player)) return null;
        GauntletProfile profile = profile(player);
        List<Component> lines = new ArrayList<>(List.of(
                TextUtils.parse("<gradient:#FF416C:#FF4B2B><b>GAUNTLET</b></gradient>"),
                TextUtils.parse("<gray>• SHARDS: <aqua>" + profile.shards),
                TextUtils.parse("<gray>• CORES: <light_purple>" + profile.cores),
                TextUtils.parse("<gray>• BEST FLOOR: <white>" + profile.best)));
        if (runs != null && runs.inRun(player)) {
            lines.add(Component.empty());
            lines.add(TextUtils.parse("<gradient:#FF416C:#FF4B2B><b>THIS RUN</b></gradient>"));
            lines.add(TextUtils.parse("<gray>• FLOOR: <white>" + runs.floorOf(player)));
            lines.add(TextUtils.parse("<gray>• EARNED: <aqua>" + runs.shardsOf(player) + " Shards"
                    + (runs.coresOf(player) > 0 ? " <light_purple>" + runs.coresOf(player) + " Cores" : "")));
        } else {
            lines.add(TextUtils.parse("<gray>• RUNS: <white>" + profile.runs));
        }
        return lines;
    }

    public void refreshBoard(Player player) {
        scoreboard.updateBoard(player);
    }

    public void shutdown() {
        if (sweep != null) sweep.cancel();
    }

    public GauntletArena arena() {
        return arena;
    }

    public GauntletData data() {
        return plugin.getRegistry().getGauntlet();
    }

    public DataRegistry registry() {
        return plugin.getRegistry();
    }

    // The gauntlet's world, and not while it is a build world being worked on
    public boolean isHere(Player player) {
        return arena.isGauntletWorld(player.getWorld()) && !build.isBuildWorld(player.getWorld());
    }

    // Made on first use, with the starter gear at level 0
    public GauntletProfile profile(UUID uuid) {
        return profiles.computeIfAbsent(uuid, id -> {
            GauntletProfile profile = new GauntletProfile();
            for (GauntletData.Gear gear : data().gear().values()) {
                if (gear.starter()) profile.gear.put(gear.id(), 0);
            }
            db.async(() -> {
                store.save(id, profile);
                profile.gear.forEach((gear, level) -> store.saveGear(id, gear, level));
            });
            return profile;
        });
    }

    public GauntletProfile profile(Player player) {
        GauntletProfile profile = profile(player.getUniqueId());
        // Starter gear added to the data after a player started is theirs too
        for (GauntletData.Gear gear : data().gear().values()) {
            if (gear.starter() && !profile.owns(gear.id())) setLevel(player.getUniqueId(), profile, gear.id(), 0);
        }
        return profile;
    }

    void save(UUID uuid) {
        GauntletProfile profile = profiles.get(uuid);
        if (profile != null) db.async(() -> store.save(uuid, profile));
    }

    private void setLevel(UUID uuid, GauntletProfile profile, String gear, int level) {
        profile.gear.put(gear, level);
        db.async(() -> store.saveGear(uuid, gear, level));
    }

    // The best floors, highest first
    public List<Map.Entry<UUID, GauntletProfile>> top(int count) {
        return profiles.entrySet().stream().filter(entry -> entry.getValue().best > 0)
                .sorted(Comparator.comparingInt((Map.Entry<UUID, GauntletProfile> entry) -> entry.getValue().best).reversed())
                .limit(count).toList();
    }

    public static String nameOf(UUID uuid) {
        OfflinePlayer player = Bukkit.getOfflinePlayer(uuid);
        return player.getName() == null ? "?" : player.getName();
    }

    // ---- Coming and going ----

    // From anywhere: to the lobby. Null if it worked, otherwise why not
    public String sendToLobby(Player player) {
        Location lobby = arena.lobby();
        if (lobby == null) return "The Gauntlet is not open yet";
        if (!data().ready()) return "The Gauntlet's data did not load";
        player.teleport(lobby);
        return null;
    }

    public void leave(Player player) {
        if (runs != null) runs.end(player, GauntletRuns.End.LEFT);
        Location spawn = plugin.getMainSpawn();
        player.teleport(spawn != null ? spawn : Bukkit.getWorlds().getFirst().getSpawnLocation());
    }

    // Before the loadouts swap (NORMAL), so a loadout put away on the way out holds
    // the player's own inventory, not their gauntlet gear
    @EventHandler(priority = EventPriority.LOWEST)
    public void onLeaveWorld(PlayerChangedWorldEvent event) {
        Player player = event.getPlayer();
        if (!arena.isGauntletWorld(event.getFrom()) || build.isBuildWorld(event.getFrom())) return;
        if (isHere(player)) return;
        if (runs != null) runs.end(player, GauntletRuns.End.LEFT);
        giveBack(player);
        refreshBoard(player);
    }

    // After the loadouts swap, so what is put away is the loadout coming in
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onEnterWorld(PlayerChangedWorldEvent event) {
        Player player = event.getPlayer();
        if (!isHere(player) || (arena.isGauntletWorld(event.getFrom()) && !build.isBuildWorld(event.getFrom()))) return;
        putAway(player);
        giveKit(player);
        refreshBoard(player);
        TextUtils.send(player, "<red><b>THE GAUNTLET</b> <gray>Your own inventory is kept for when you leave. "
                + "<white>/gauntlet</white> to start a run or open the forge.");
    }

    // A player who logged out here comes back in the lobby, never inside an arena copy.
    // One who is somewhere else with an inventory still put away gets it back
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (!player.isOnline()) return;
            if (isHere(player)) {
                Location lobby = arena.lobby();
                if (lobby != null) player.teleport(lobby);
                giveKit(player);
            } else if (!build.isBuildWorld(player.getWorld())) {
                giveBack(player);
            }
        }, 5L);
    }

    private void putAway(Player player) {
        UUID uuid = player.getUniqueId();
        ItemStack[] contents = Arrays.stream(player.getInventory().getContents())
                .map(item -> item == null ? ItemStack.empty() : item).toArray(ItemStack[]::new);
        byte[] items = ItemStack.serializeItemsAsBytes(contents);
        // Saved before the inventory is cleared, a crash in between loses nothing
        store.saveInventory(uuid, items);
        player.getInventory().clear();
    }

    private void giveBack(Player player) {
        UUID uuid = player.getUniqueId();
        byte[] items = store.loadInventory(uuid);
        if (items == null) return;
        player.getInventory().clear();
        player.getInventory().setContents(ItemStack.deserializeItemsFromBytes(items));
        player.updateInventory();
        db.async(() -> store.removeInventory(uuid));
    }

    // ---- Gear ----

    // Every piece of gear the player owns, armour worn, the shield in the off hand,
    // weapons in the hotbar in the forge's order. Anything a run handed out stays
    public void giveKit(Player player) {
        PlayerInventory inventory = player.getInventory();
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            ItemStack item = inventory.getItem(slot);
            if (item != null && !GauntletGear.isRunItem(item)) inventory.setItem(slot, null);
        }
        GauntletProfile profile = profile(player);
        for (GauntletData.Gear gear : data().gear().values()) {
            GauntletData.GearLevel level = data().level(profile.level(gear.id()));
            if (!profile.owns(gear.id()) || level == null) continue;
            ItemStack item = GauntletGear.build(registry(), gear, level);
            EquipmentSlot slot = item.getType().getEquipmentSlot();
            if (slot.isArmor()) inventory.setItem(slot, item);
            else if (item.getType() == Material.SHIELD) inventory.setItemInOffHand(item);
            else inventory.addItem(item);
        }
        player.updateInventory();
    }

    // Null if it worked, otherwise why not
    public String unlock(Player player, String gearId) {
        GauntletData.Gear gear = data().gear().get(gearId);
        GauntletProfile profile = profile(player);
        if (gear == null) return "There is no such gear";
        if (profile.owns(gearId)) return "You already have it";
        if (profile.best < gear.unlockFloor()) return "Reach floor " + gear.unlockFloor() + " first";
        if (profile.shards < gear.unlockShards()) return "You need " + (gear.unlockShards() - profile.shards) + " more Shards";
        profile.shards -= gear.unlockShards();
        setLevel(player.getUniqueId(), profile, gearId, 0);
        save(player.getUniqueId());
        afterPurchase(player, "<green>Unlocked the " + gear.name());
        return null;
    }

    public String upgrade(Player player, String gearId) {
        GauntletData.Gear gear = data().gear().get(gearId);
        GauntletProfile profile = profile(player);
        if (gear == null || !profile.owns(gearId)) return "Unlock it first";
        GauntletData.GearLevel next = data().level(profile.level(gearId) + 1);
        if (next == null) return "It is fully upgraded";
        if (profile.shards < next.shards()) return "You need " + (next.shards() - profile.shards) + " more Shards";
        if (profile.cores < next.cores()) return "You need " + (next.cores() - profile.cores) + " more Cores";
        profile.shards -= next.shards();
        profile.cores -= next.cores();
        setLevel(player.getUniqueId(), profile, gearId, next.level());
        save(player.getUniqueId());
        afterPurchase(player, "<green>" + gear.name() + " upgraded to <white>+" + next.level());
        return null;
    }

    public String upgradeCare(Player player) {
        GauntletProfile profile = profile(player);
        if (profile.care >= data().maxCareLevel()) return "Your care package is fully upgraded";
        GauntletData.CareLevel next = data().careLevel(profile.care + 1);
        if (profile.shards < next.shards()) return "You need " + (next.shards() - profile.shards) + " more Shards";
        if (profile.cores < next.cores()) return "You need " + (next.cores() - profile.cores) + " more Cores";
        profile.shards -= next.shards();
        profile.cores -= next.cores();
        profile.care = next.level();
        save(player.getUniqueId());
        afterPurchase(player, "<green>Care package upgraded to level <white>" + next.level());
        return null;
    }

    private void afterPurchase(Player player, String message) {
        player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_USE, 0.6f, 1.2f);
        TextUtils.send(player, message);
        if (isHere(player) && (runs == null || !runs.inRun(player))) giveKit(player);
        refreshBoard(player);
    }

    // ---- After a run ----

    // What a run earned, and the best floor cleared in it
    void bank(Player player, int shards, int cores, int cleared) {
        UUID uuid = player.getUniqueId();
        GauntletProfile profile = profile(player);
        profile.shards += shards;
        profile.cores += cores;
        profile.runs++;
        boolean record = cleared > profile.best;
        if (record) profile.best = cleared;
        save(uuid);
        if (record) milestones(player, profile);
        refreshBoard(player);
    }

    private void milestones(Player player, GauntletProfile profile) {
        for (GauntletData.Milestone milestone : data().milestones()) {
            if (profile.best < milestone.floor() || profile.milestones.contains(milestone.floor())) continue;
            profile.milestones.add(milestone.floor());
            db.async(() -> store.saveMilestone(player.getUniqueId(), milestone.floor()));

            if (milestone.essence() > 0) players.addEssence(player, milestone.essence());
            if (!milestone.cosmetic().isBlank()) cosmetics.give(player.getUniqueId(), milestone.cosmetic());
            scoreboard.updateBoard(player);
            player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.7f, 1f);
            TextUtils.send(player, "<gold><b>GAUNTLET MILESTONE</b> <white>Floor " + milestone.floor()
                    + (milestone.essence() > 0 ? " <aqua>+" + milestone.essence() + " Essence" : ""));
        }
    }

    // ---- Keeping the world apart ----

    // Gear from the main game would carry its stats into the gauntlet (taken out of
    // the Armory here, say), so anything with Aurora stats that is not gauntlet gear
    // is taken away, as are main-game tools
    private void sweepAll() {
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            if (!isHere(player)) continue;
            PlayerInventory inventory = player.getInventory();
            for (int slot = 0; slot < inventory.getSize(); slot++) {
                ItemStack item = inventory.getItem(slot);
                if (item == null || !item.hasItemMeta() || GauntletGear.gearId(item) != null) continue;
                var pdc = item.getItemMeta().getPersistentDataContainer();
                if (pdc.has(DataKeys.ENCHANT_KEY, PersistentDataType.TAG_CONTAINER)
                        || pdc.has(DataKeys.TOOL_KEY, PersistentDataType.STRING)) {
                    inventory.setItem(slot, null);
                }
            }
        }
    }

    // The ender chest holds main-game things
    @EventHandler(ignoreCancelled = true)
    public void onOpen(InventoryOpenEvent event) {
        if (event.getPlayer() instanceof Player player && isHere(player)
                && event.getInventory().getType() == InventoryType.ENDER_CHEST) {
            event.setCancelled(true);
            TextUtils.error(player, "Your ender chest can't be opened in the Gauntlet");
        }
    }

    // Players only hurt each other in the PvP zone, both in it and neither in a run
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPvp(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim) || !isHere(victim)) return;
        Player attacker = event.getDamager() instanceof Player p ? p
                : event.getDamager() instanceof Projectile projectile
                && projectile.getShooter() instanceof Player shooter ? shooter : null;
        if (attacker == null) return;
        boolean allowed = arena.inPvp(victim.getLocation()) && arena.inPvp(attacker.getLocation())
                && (runs == null || (!runs.inRun(victim) && !runs.inRun(attacker)));
        if (!allowed) event.setCancelled(true);
    }

    // Dying here costs nothing, the gear is kept, and they come back in the lobby
    @EventHandler(priority = EventPriority.HIGH)
    public void onDeath(PlayerDeathEvent event) {
        Player player = event.getPlayer();
        if (!isHere(player)) return;
        event.setKeepInventory(true);
        event.getDrops().clear();
        event.setKeepLevel(true);
        event.setDroppedExp(0);
        diedHere.add(player.getUniqueId());
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onRespawn(PlayerRespawnEvent event) {
        if (!diedHere.remove(event.getPlayer().getUniqueId())) return;
        Location lobby = arena.lobby();
        if (lobby != null) event.setRespawnLocation(lobby);
    }
}
