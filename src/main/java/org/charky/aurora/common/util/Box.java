package org.charky.aurora.common.util;

import org.bukkit.Location;

// A box of blocks in one world, both corners included. Corners can be given in any
// order, they are stored sorted so x1 <= x2 and so on. Mines and portals are both one
public record Box(String world, int x1, int y1, int z1, int x2, int y2, int z2) {
    public Box {
        int minX = Math.min(x1, x2), minY = Math.min(y1, y2), minZ = Math.min(z1, z2);
        int maxX = Math.max(x1, x2), maxY = Math.max(y1, y2), maxZ = Math.max(z1, z2);
        x1 = minX;
        y1 = minY;
        z1 = minZ;
        x2 = maxX;
        y2 = maxY;
        z2 = maxZ;
    }

    // In the first corner's world, callers check both corners share one
    public static Box between(Location a, Location b) {
        return new Box(a.getWorld().getName(),
                a.getBlockX(), a.getBlockY(), a.getBlockZ(), b.getBlockX(), b.getBlockY(), b.getBlockZ());
    }

    public boolean contains(String worldName, int x, int y, int z) {
        return world.equals(worldName)
                && x >= x1 && x <= x2 && y >= y1 && y <= y2 && z >= z1 && z <= z2;
    }

    // The block the location is in
    public boolean contains(Location location) {
        return location.getWorld() != null && contains(location.getWorld().getName(),
                location.getBlockX(), location.getBlockY(), location.getBlockZ());
    }

    // The boxes share at least one block, touching faces count since that block is in both
    public boolean overlaps(Box other) {
        return world.equals(other.world)
                && other.x1 <= x2 && other.x2 >= x1
                && other.y1 <= y2 && other.y2 >= y1
                && other.z1 <= z2 && other.z2 >= z1;
    }

    // Blocks in the box
    public long volume() {
        return (long) (x2 - x1 + 1) * (y2 - y1 + 1) * (z2 - z1 + 1);
    }
}
