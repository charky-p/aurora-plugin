package org.charky.aurora.features.world;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.charky.aurora.core.Aurora;
import org.charky.aurora.core.database.WorldStore;
import org.charky.aurora.features.system.VoidGenerator;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

// Extra void worlds like Gormak's. Created by command, then loaded on every startup
// so their portals, mines and spawners have somewhere to be
public class WorldManager {
    // Vanilla rejects other characters in a world folder name
    private static final Pattern VALID_NAME = Pattern.compile("[a-z0-9_]{1,32}");

    private final Aurora plugin;
    private final WorldStore store;
    private final List<String> managed = new ArrayList<>();

    public WorldManager(Aurora plugin, WorldStore store) {
        this.plugin = plugin;
        this.store = store;
    }

    // Before anything that looks worlds up by name, spawns and portals included
    public void loadAll() {
        for (String name : store.loadAll()) {
            if (load(name) == null) plugin.getLogger().warning("World '" + name + "' could not be loaded");
            else managed.add(name);
        }
    }

    // Vanilla's own dimensions, taken by the main world, its nether and end whatever their folders are called
    private static final List<String> RESERVED = List.of("overworld", "the_nether", "the_end");

    public static boolean isValidName(String name) {
        return VALID_NAME.matcher(name).matches();
    }

    // Bukkit refuses a world whose key another world already has, and the main world's
    // key is minecraft:overworld although its folder is called world
    public static boolean isTaken(String name) {
        if (RESERVED.contains(name)) return true;
        return Bukkit.getWorlds().stream()
                .anyMatch(world -> !world.getName().equals(name) && world.getKey().getKey().equals(name));
    }

    // Loads it if its folder exists, generates an empty one if not
    public World create(String name) {
        World world = load(name);
        if (world == null) return null;

        store.add(name);
        if (!managed.contains(name)) managed.add(name);
        return world;
    }

    // Stops it loading on startup, the files stay so it can be added back
    public boolean remove(String name) {
        if (!managed.remove(name)) return false;
        store.remove(name);
        return true;
    }

    public boolean isManaged(String name) {
        return managed.contains(name);
    }

    public List<String> getManaged() {
        return new ArrayList<>(managed);
    }

    private static World load(String name) {
        World loaded = Bukkit.getWorld(name);
        if (loaded != null) return loaded;

        WorldCreator creator = new WorldCreator(name);
        creator.generator(new VoidGenerator());
        creator.generateStructures(false);
        try {
            return Bukkit.createWorld(creator);
        } catch (IllegalArgumentException e) {
            Bukkit.getLogger().warning("[Aurora] World '" + name + "': " + e.getMessage());
            return null;
        }
    }
}
