package org.charky.aurora.features.gauntlet;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Container;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.AreaEffectCloud;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ExperienceOrb;
import org.bukkit.entity.Item;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Trident;
import org.bukkit.block.structure.Mirror;
import org.bukkit.block.structure.StructureRotation;
import org.bukkit.structure.Structure;
import org.bukkit.structure.StructureManager;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;
import org.charky.aurora.common.util.Box;
import org.charky.aurora.core.database.Database;
import org.charky.aurora.core.database.GauntletStore;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

// The gauntlet's places, all in one world: the lobby players arrive in, the PvP zone,
// and the arena. The arena is built once, by hand, and saved from the wand as a
// template; every run gets its own copy pasted SLOT_SPACING blocks further along z,
// so players run the tower side by side. The spawns inside it are saved where they
// stand in the template and moved to each copy the same way
public class GauntletArena {
    public static final String LOBBY = "lobby";
    private static final String ARENA_MIN = "arena_min";
    private static final String ARENA_MAX = "arena_max";
    public static final String SPAWN = "spawn";
    public static final String PACKAGE = "package";
    public static final String MOB = "mob_";
    private static final String PVP_A = "pvp_a";
    private static final String PVP_B = "pvp_b";
    private static final String START_A = "start_a";
    private static final String START_B = "start_b";
    private static final String TEMPLATE = "gauntlet_arena.nbt";
    private static final String DUEL_MIN = "duel_min";
    private static final String DUEL_MAX = "duel_max";
    private static final String DUEL_SPAWN = "duel_spawn_";
    private static final String DUEL_TEMPLATE = "gauntlet_duel.nbt";
    static final int SLOT_SPACING = 1000;

    private final Database db;
    private final GauntletStore store;
    private final File templateFile;
    private final Logger logger;
    private final Map<String, Location> places = new ConcurrentHashMap<>();
    private final Set<Integer> usedSlots = new HashSet<>();
    private final Set<Integer> usedDuelSlots = new HashSet<>();
    private final File duelFile;
    private Structure template;
    private Structure duelTemplate;

    public GauntletArena(Database db, GauntletStore store, File dataFolder, Logger logger) {
        this.db = db;
        this.store = store;
        this.templateFile = new File(dataFolder, TEMPLATE);
        this.duelFile = new File(dataFolder, DUEL_TEMPLATE);
        this.logger = logger;
        places.putAll(store.loadPlaces());
        loadTemplate();
    }

    private void loadTemplate() {
        template = load(templateFile);
        duelTemplate = load(duelFile);
    }

    private Structure load(File file) {
        if (!file.exists()) return null;
        try {
            return manager().loadStructure(file);
        } catch (IOException e) {
            logger.severe("Could not read the gauntlet arena " + file.getName() + ": " + e.getMessage());
            return null;
        }
    }

    private static StructureManager manager() {
        return Bukkit.getStructureManager();
    }

    // The world everything is in, the lobby's. Null until the lobby is set
    public World world() {
        Location lobby = places.get(LOBBY);
        return lobby == null ? null : lobby.getWorld();
    }

    public boolean isGauntletWorld(World world) {
        return world != null && world.equals(world());
    }

    public Location lobby() {
        Location lobby = places.get(LOBBY);
        return lobby == null ? null : lobby.clone();
    }

    // Null when everything a run needs is there, otherwise what is missing
    public String missing() {
        if (places.get(LOBBY) == null) return "no lobby, /a gauntlet lobby";
        if (template == null || places.get(ARENA_MIN) == null) return "no arena, /a gauntlet arena";
        if (places.get(SPAWN) == null) return "no player spawn in the arena, /a gauntlet spawn";
        if (mobSpawns().isEmpty()) return "no mob spawns in the arena, /a gauntlet mobspawn";
        return null;
    }

    public void set(String name, Location location) {
        Location place = location.clone();
        places.put(name, place);
        db.async(() -> store.savePlace(name, place));
    }

    // Saves the selection as the arena. Null if it worked, otherwise why not
    public String saveArena(Location a, Location b) {
        if (a.getWorld() == null || !a.getWorld().equals(b.getWorld())) return "Both corners have to be in the same world";
        if (!isGauntletWorld(a.getWorld())) return "The arena has to be in the lobby's world, set the lobby first";
        Location min = new Location(a.getWorld(), Math.min(a.getBlockX(), b.getBlockX()),
                Math.min(a.getBlockY(), b.getBlockY()), Math.min(a.getBlockZ(), b.getBlockZ()));
        Location max = new Location(a.getWorld(), Math.max(a.getBlockX(), b.getBlockX()),
                Math.max(a.getBlockY(), b.getBlockY()), Math.max(a.getBlockZ(), b.getBlockZ()));
        if (max.getBlockZ() - min.getBlockZ() >= SLOT_SPACING / 2) return "The arena is too long, keep it under " + SLOT_SPACING / 2 + " blocks";

        Structure structure = manager().createStructure();
        structure.fill(min, max.clone().add(1, 1, 1), false);
        try {
            manager().saveStructure(templateFile, structure);
        } catch (IOException e) {
            return "Could not save the arena: " + e.getMessage();
        }
        template = structure;
        set(ARENA_MIN, min);
        set(ARENA_MAX, max);
        return null;
    }

    public Box arenaBox() {
        Location min = places.get(ARENA_MIN), max = places.get(ARENA_MAX);
        return min == null || max == null ? null : Box.between(min, max);
    }

    public void addMobSpawn(Location location) {
        set(MOB + (mobSpawns().size() + 1), location);
    }

    public void clearMobSpawns() {
        places.keySet().removeIf(name -> name.startsWith(MOB));
        db.async(() -> store.removePlaces(MOB));
    }

    public List<Location> mobSpawns() {
        List<String> names = new ArrayList<>(places.keySet().stream().filter(name -> name.startsWith(MOB)).toList());
        names.sort(Comparator.comparingInt(name -> Integer.parseInt(name.substring(MOB.length()))));
        return names.stream().map(places::get).toList();
    }

    public Location place(String name) {
        Location place = places.get(name);
        return place == null ? null : place.clone();
    }

    public String setPvp(Location a, Location b) {
        if (a.getWorld() == null || !a.getWorld().equals(b.getWorld())) return "Both corners have to be in the same world";
        if (!isGauntletWorld(a.getWorld())) return "The PvP zone has to be in the lobby's world";
        set(PVP_A, a);
        set(PVP_B, b);
        return null;
    }

    // The start portal in the lobby: walking in starts a run
    public String setStart(Location a, Location b) {
        if (a.getWorld() == null || !a.getWorld().equals(b.getWorld())) return "Both corners have to be in the same world";
        if (!isGauntletWorld(a.getWorld())) return "The start portal has to be in the lobby's world";
        set(START_A, a);
        set(START_B, b);
        return null;
    }

    public boolean hasStart() {
        return places.get(START_A) != null && places.get(START_B) != null;
    }

    public boolean inStart(Location location) {
        Location a = places.get(START_A), b = places.get(START_B);
        return a != null && b != null && Box.between(a, b).contains(location);
    }

    public boolean inPvp(Location location) {
        Location a = places.get(PVP_A), b = places.get(PVP_B);
        return a != null && b != null && Box.between(a, b).contains(location);
    }

    // A free copy of the arena, -1 if the arena is not set up
    public int claimSlot() {
        int slot = 0;
        while (usedSlots.contains(slot)) slot++;
        usedSlots.add(slot);
        return slot;
    }

    public void freeSlot(int slot) {
        usedSlots.remove(slot);
    }

    // Pastes a fresh copy of the arena for a slot, so nothing is left from the run before
    public void paste(int slot) {
        if (template == null) return;
        clearSlot(slot, List.of());
        template.place(origin(slot), false, StructureRotation.NONE, Mirror.NONE, 0, 1f, new Random());
    }

    // A run's copy, done with: its care package chests emptied and gone (pasting over a
    // full chest would spill it onto the floor of the next run) and everything left lying
    // in it removed: dropped items, arrows, orbs, potion clouds, markers. The chests at
    // the places given and at the package spot
    public void clearSlot(int slot, Collection<Location> chests) {
        Location min = places.get(ARENA_MIN), max = places.get(ARENA_MAX);
        if (min == null || max == null) return;
        Location origin = origin(slot);
        List<Location> all = new ArrayList<>(chests);
        Location spot = inSlot(slot, places.get(PACKAGE));
        if (spot != null) all.add(spot);
        clear(origin, origin.clone().add(max.toVector().subtract(min.toVector())), all);
    }

    private static void clear(Location from, Location to, Collection<Location> chests) {
        World world = from.getWorld();
        if (world == null) return;
        for (Location at : chests) {
            if (!(at.getBlock().getState() instanceof Container container)) continue;
            container.getInventory().clear();
            at.getBlock().setType(Material.AIR, false);
        }
        BoundingBox box = BoundingBox.of(from.getBlock(), to.getBlock()).expand(0, 0, 0, 0, 8, 0);
        for (Entity entity : world.getNearbyEntities(box)) {
            if (entity instanceof Item || entity instanceof Projectile || entity instanceof ExperienceOrb
                    || entity instanceof AreaEffectCloud || entity instanceof Display) entity.remove();
        }
    }

    // What lies on the ground in the Gauntlet's world, for its clear-lag: dropped items
    // (PvP loot among them), arrows stuck in blocks and experience orbs. Not in the copies
    // of runs and duels still going (they are cleared when they end), nor items kept on
    // show for good
    public List<Entity> groundClutter() {
        World world = world();
        if (world == null) return List.of();
        List<Entity> clutter = new ArrayList<>();
        for (Entity entity : world.getEntities()) {
            boolean lying = entity instanceof Item item ? !item.isUnlimitedLifetime()
                    : entity instanceof ExperienceOrb
                    || entity instanceof AbstractArrow arrow && !(arrow instanceof Trident) && arrow.isInBlock();
            if (lying && !inUse(entity.getLocation())) clutter.add(entity);
        }
        return clutter;
    }

    // In the copy of a run or duel still going
    private boolean inUse(Location location) {
        for (int slot : usedSlots) if (isOverSlot(slot, location)) return true;
        for (int slot : usedDuelSlots) if (isInDuelSlot(slot, location)) return true;
        return false;
    }

    // A place saved in the template, moved to a slot's copy
    public Location inSlot(int slot, Location place) {
        Location min = places.get(ARENA_MIN);
        if (min == null || place == null) return null;
        Vector offset = place.toVector().subtract(min.toVector());
        Location moved = origin(slot).add(offset);
        moved.setYaw(place.getYaw());
        moved.setPitch(place.getPitch());
        return moved;
    }

    public boolean isInSlot(int slot, Location location) {
        Location min = places.get(ARENA_MIN), max = places.get(ARENA_MAX);
        if (min == null || max == null) return false;
        Location origin = origin(slot);
        Location far = origin.clone().add(max.toVector().subtract(min.toVector()));
        return Box.between(origin, far).contains(location);
    }

    // Over or under a slot's copy at any height, as seen from above
    public boolean isOverSlot(int slot, Location location) {
        Location min = places.get(ARENA_MIN), max = places.get(ARENA_MAX);
        if (min == null || max == null) return false;
        Location origin = origin(slot);
        Location far = origin.clone().add(max.toVector().subtract(min.toVector()));
        Location level = location.clone();
        level.setY(origin.getY());
        return Box.between(origin, far).contains(level);
    }

    private Location origin(int slot) {
        Location min = places.get(ARENA_MIN);
        return min.clone().add(0, 0, (long) SLOT_SPACING * (slot + 1));
    }

    // ---- The duel arena ----
    // Built once and saved like the run arena; every duel gets its own copy, pasted
    // SLOT_SPACING blocks further along x, so duels never meet the runs (along z)

    // Null if it worked, otherwise why not
    public String saveDuelArena(Location a, Location b) {
        if (a.getWorld() == null || !a.getWorld().equals(b.getWorld())) return "Both corners have to be in the same world";
        if (!isGauntletWorld(a.getWorld())) return "The duel arena has to be in the lobby's world, set the lobby first";
        Location min = new Location(a.getWorld(), Math.min(a.getBlockX(), b.getBlockX()),
                Math.min(a.getBlockY(), b.getBlockY()), Math.min(a.getBlockZ(), b.getBlockZ()));
        Location max = new Location(a.getWorld(), Math.max(a.getBlockX(), b.getBlockX()),
                Math.max(a.getBlockY(), b.getBlockY()), Math.max(a.getBlockZ(), b.getBlockZ()));
        if (max.getBlockX() - min.getBlockX() >= SLOT_SPACING / 2) return "The duel arena is too wide, keep it under " + SLOT_SPACING / 2 + " blocks";
        Structure structure = manager().createStructure();
        structure.fill(min, max.clone().add(1, 1, 1), false);
        try {
            manager().saveStructure(duelFile, structure);
        } catch (IOException e) {
            return "Could not save the duel arena: " + e.getMessage();
        }
        duelTemplate = structure;
        set(DUEL_MIN, min);
        set(DUEL_MAX, max);
        return null;
    }

    public Box duelBox() {
        Location min = places.get(DUEL_MIN), max = places.get(DUEL_MAX);
        return min == null || max == null ? null : Box.between(min, max);
    }

    public void setDuelSpawn(int which, Location location) {
        set(DUEL_SPAWN + which, location);
    }

    // Null when duels can be fought, otherwise what is missing
    public String duelMissing() {
        if (places.get(LOBBY) == null) return "no lobby, /a gauntlet lobby";
        if (duelTemplate == null || places.get(DUEL_MIN) == null) return "no duel arena, /a gauntlet duelarena";
        if (places.get(DUEL_SPAWN + 1) == null || places.get(DUEL_SPAWN + 2) == null) return "no duel spawns, /a gauntlet duelspawn 1 and 2";
        return null;
    }

    public int claimDuelSlot() {
        int slot = 0;
        while (usedDuelSlots.contains(slot)) slot++;
        usedDuelSlots.add(slot);
        return slot;
    }

    // A duel's copy, done with: whatever was dropped or shot in it removed
    public void clearDuelSlot(int slot) {
        Location min = places.get(DUEL_MIN), max = places.get(DUEL_MAX);
        if (min == null || max == null) return;
        Location origin = duelOrigin(slot);
        clear(origin, origin.clone().add(max.toVector().subtract(min.toVector())), List.of());
    }

    public void freeDuelSlot(int slot) {
        usedDuelSlots.remove(slot);
    }

    public void pasteDuel(int slot) {
        if (duelTemplate == null) return;
        clearDuelSlot(slot);
        duelTemplate.place(duelOrigin(slot), false, StructureRotation.NONE, Mirror.NONE, 0, 1f, new Random());
    }

    // Where duelist 1 or 2 starts in a slot's copy
    public Location duelSpawn(int slot, int which) {
        Location min = places.get(DUEL_MIN), place = places.get(DUEL_SPAWN + which);
        if (min == null || place == null) return null;
        Location moved = duelOrigin(slot).add(place.toVector().subtract(min.toVector()));
        moved.setYaw(place.getYaw());
        moved.setPitch(place.getPitch());
        return moved;
    }

    public boolean isInDuelSlot(int slot, Location location) {
        Location min = places.get(DUEL_MIN), max = places.get(DUEL_MAX);
        if (min == null || max == null) return false;
        Location origin = duelOrigin(slot);
        return Box.between(origin, origin.clone().add(max.toVector().subtract(min.toVector()))).contains(location);
    }

    private Location duelOrigin(int slot) {
        return places.get(DUEL_MIN).clone().add((long) SLOT_SPACING * (slot + 1), 0, 0);
    }
}
