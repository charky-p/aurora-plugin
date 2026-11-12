package org.charky.aurora.features.trader;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

// A saved place a trader stands, the id is its row in the traders table
public record TraderSpot(int id, String traderId, String world, double x, double y, double z, float yaw) {
    // Null when the world is not loaded
    public Location toLocation() {
        World bukkitWorld = Bukkit.getWorld(world);
        if (bukkitWorld == null) return null;
        return new Location(bukkitWorld, x, y, z, yaw, 0);
    }

    public boolean isChunkLoaded() {
        World bukkitWorld = Bukkit.getWorld(world);
        return bukkitWorld != null && bukkitWorld.isChunkLoaded((int) Math.floor(x) >> 4, (int) Math.floor(z) >> 4);
    }

    public String describe() {
        return "#" + id + " " + traderId + " (" + world + " " + (int) Math.floor(x) + ", " + (int) Math.floor(y) + ", " + (int) Math.floor(z) + ")";
    }
}
