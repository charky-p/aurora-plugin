package org.charky.aurora.features.portal;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

// A block players step on to unlock a teleport to it from spawn. Stepping on it opens
// the access id below for good, and a portal at spawn locked to that id leads here
public record TeleportPad(String name, String world, int x, int y, int z) {
    private static final String ACCESS_PREFIX = "pad_";

    public String access() {
        return ACCESS_PREFIX + name;
    }

    // Standing on top of it, null while its world is not loaded
    public Location top() {
        World w = Bukkit.getWorld(world);
        return w == null ? null : new Location(w, x + 0.5, y + 1, z + 0.5);
    }

    public String key() {
        return key(world, x, y, z);
    }

    static String key(String world, int x, int y, int z) {
        return world + ":" + x + ":" + y + ":" + z;
    }
}
