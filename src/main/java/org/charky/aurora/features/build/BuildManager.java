package org.charky.aurora.features.build;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.BlockInventoryHolder;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.charky.aurora.common.util.TextUtils;
import org.charky.aurora.core.database.BuildStore;
import org.charky.aurora.core.database.Database;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

// Build worlds: only builders (and admins) may go there, and they build in creative
// without OP. Each builder has two inventories, the survival one is put away on the
// way in and given back on the way out, so nothing made in creative reaches the game
public class BuildManager implements Listener {
    public static final String ADMIN = "aurora.admin";
    private static final String SURVIVAL = "survival";
    private static final String BUILD = "build";

    private final Database db;
    private final BuildStore store;
    private final Set<String> worlds = ConcurrentHashMap.newKeySet();
    private final Map<UUID, String> builders = new ConcurrentHashMap<>();
    private final Map<UUID, Map<String, byte[]>> inventories = new ConcurrentHashMap<>();
    // Where someone sent out of a build world goes, the main spawn
    private final Supplier<Location> exit;

    public BuildManager(Database db, BuildStore store, Supplier<Location> exit) {
        this.db = db;
        this.store = store;
        this.exit = exit;
        worlds.addAll(store.loadWorlds());
        builders.putAll(store.loadBuilders());
        store.loadInventories().forEach((uuid, kinds) -> inventories.put(uuid, new ConcurrentHashMap<>(kinds)));
    }

    public boolean isBuildWorld(World world) {
        return world != null && worlds.contains(world.getName());
    }

    public Set<String> getWorlds() {
        return new TreeSet<>(worlds);
    }

    public boolean isBuilder(UUID uuid) {
        return builders.containsKey(uuid);
    }

    public Map<UUID, String> getBuilders() {
        return new HashMap<>(builders);
    }

    // May go to build worlds at all
    public boolean canEnter(Player player) {
        return isBuilder(player.getUniqueId()) || player.hasPermission(ADMIN);
    }

    // May place and break blocks here: only in a build world, only builders and admins
    public boolean canBuild(Player player, World world) {
        return isBuildWorld(world) && canEnter(player);
    }

    // Players already in the world change over now, not on their next trip
    public boolean setBuildWorld(World world, boolean build) {
        String name = world.getName();
        if (build == worlds.contains(name)) return false;

        if (build) {
            worlds.add(name);
            db.async(() -> store.addWorld(name));
            for (Player player : world.getPlayers()) {
                if (canEnter(player)) enter(player);
                else sendAway(player);
            }
        } else {
            for (Player player : world.getPlayers()) leave(player);
            worlds.remove(name);
            db.async(() -> store.removeWorld(name));
        }
        return true;
    }

    public boolean addBuilder(UUID uuid, String name) {
        if (builders.put(uuid, name) != null) return false;
        db.async(() -> store.addBuilder(uuid, name));
        return true;
    }

    // Someone taken off the list while building is sent back to the game
    public boolean removeBuilder(UUID uuid) {
        if (builders.remove(uuid) == null) return false;
        db.async(() -> store.removeBuilder(uuid));
        Player player = Bukkit.getPlayer(uuid);
        if (player != null && isBuildWorld(player.getWorld()) && !player.hasPermission(ADMIN)) sendAway(player);
        return true;
    }

    // Anyone not allowed in is turned back at the door, portals and /tp included
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onTeleport(PlayerTeleportEvent event) {
        World to = event.getTo().getWorld();
        if (to == event.getFrom().getWorld() || !isBuildWorld(to) || canEnter(event.getPlayer())) return;
        event.setCancelled(true);
        TextUtils.error(event.getPlayer(), "Only builders can go to " + to.getName());
    }

    @EventHandler
    public void onChangedWorld(PlayerChangedWorldEvent event) {
        Player player = event.getPlayer();
        boolean from = isBuildWorld(event.getFrom());
        boolean to = isBuildWorld(player.getWorld());
        if (to && !from) enter(player);
        else if (from && !to) leave(player);
    }

    // Menus of the game (backpack, shops, trash) and the ender chest would let creative
    // items or copies out of a build world, so only real containers open there
    @EventHandler(ignoreCancelled = true)
    public void onOpen(InventoryOpenEvent event) {
        if (!(event.getPlayer() instanceof Player player) || !isBuildWorld(player.getWorld())) return;
        InventoryType type = event.getInventory().getType();
        InventoryHolder holder = event.getInventory().getHolder(false);
        boolean container = holder instanceof BlockInventoryHolder || holder instanceof Entity;
        if (type == InventoryType.ENDER_CHEST || type == InventoryType.MERCHANT || !container) {
            event.setCancelled(true);
            TextUtils.error(player, "That can't be opened in a build world");
        }
    }

    private void enter(Player player) {
        swap(player, SURVIVAL, BUILD);
        player.setGameMode(GameMode.CREATIVE);
        TextUtils.success(player, "Build world: creative, with your build inventory. Your own inventory is kept for when you leave");
    }

    private void leave(Player player) {
        swap(player, BUILD, SURVIVAL);
        player.setGameMode(Bukkit.getDefaultGameMode());
    }

    // Puts what they hold away as one kind and hands them the other
    private void swap(Player player, String putAway, String takeOut) {
        UUID uuid = player.getUniqueId();
        byte[] held = ItemStack.serializeItemsAsBytes(Arrays.stream(player.getInventory().getContents())
                .map(item -> item == null ? ItemStack.empty() : item).toArray(ItemStack[]::new));
        Map<String, byte[]> kinds = inventories.computeIfAbsent(uuid, k -> new ConcurrentHashMap<>());
        kinds.put(putAway, held);
        db.async(() -> store.saveInventory(uuid, putAway, held));

        byte[] stored = kinds.get(takeOut);
        player.getInventory().clear();
        if (stored != null) player.getInventory().setContents(ItemStack.deserializeItemsFromBytes(stored));
        player.updateInventory();
    }

    private void sendAway(Player player) {
        Location spawn = exit.get();
        player.teleport(spawn != null && !isBuildWorld(spawn.getWorld()) ? spawn : Bukkit.getWorlds().getFirst().getSpawnLocation());
        TextUtils.error(player, "You are not a builder, so you were sent out of the build world");
    }
}
