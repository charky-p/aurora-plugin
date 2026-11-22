package org.charky.aurora.features.gauntlet;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.structure.Mirror;
import org.bukkit.block.structure.StructureRotation;
import org.bukkit.structure.Structure;
import org.bukkit.structure.StructureManager;
import org.bukkit.util.Vector;
import org.charky.aurora.common.util.Box;
import org.charky.aurora.core.database.Database;
import org.charky.aurora.core.database.GauntletStore;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
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
    private static final String TEMPLATE = "gauntlet_arena.nbt";
    static final int SLOT_SPACING = 1000;

    private final Database db;
    private final GauntletStore store;
    private final File templateFile;
    private final Logger logger;
    private final Map<String, Location> places = new ConcurrentHashMap<>();
    private final Set<Integer> usedSlots = new HashSet<>();
    private Structure template;

    public GauntletArena(Database db, GauntletStore store, File dataFolder, Logger logger) {
        this.db = db;
        this.store = store;
        this.templateFile = new File(dataFolder, TEMPLATE);
        this.logger = logger;
        places.putAll(store.loadPlaces());
        loadTemplate();
    }

    private void loadTemplate() {
        if (!templateFile.exists()) return;
        try {
            template = manager().loadStructure(templateFile);
        } catch (IOException e) {
            logger.severe("Could not read the gauntlet arena " + templateFile.getName() + ": " + e.getMessage());
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
        template.place(origin(slot), false, StructureRotation.NONE, Mirror.NONE, 0, 1f, new Random());
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

    private Location origin(int slot) {
        Location min = places.get(ARENA_MIN);
        return min.clone().add(0, 0, (long) SLOT_SPACING * (slot + 1));
    }
}
