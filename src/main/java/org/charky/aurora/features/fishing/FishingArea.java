package org.charky.aurora.features.fishing;

import org.bukkit.Location;

// A stretch of a fishing spot's water: the hook has to land inside it. Corners are
// stored lowest first
public record FishingArea(String name, String spot, String world, int x1, int y1, int z1, int x2, int y2, int z2) {

    public static FishingArea of(String name, String spot, Location a, Location b) {
        return new FishingArea(name, spot, a.getWorld().getName(),
                Math.min(a.getBlockX(), b.getBlockX()), Math.min(a.getBlockY(), b.getBlockY()), Math.min(a.getBlockZ(), b.getBlockZ()),
                Math.max(a.getBlockX(), b.getBlockX()), Math.max(a.getBlockY(), b.getBlockY()), Math.max(a.getBlockZ(), b.getBlockZ()));
    }

    // A block of slack above the water, where a bobber floats
    public boolean contains(Location at) {
        if (at.getWorld() == null || !at.getWorld().getName().equals(world)) return false;
        return at.getBlockX() >= x1 && at.getBlockX() <= x2
                && at.getBlockY() >= y1 && at.getBlockY() <= y2 + 1
                && at.getBlockZ() >= z1 && at.getBlockZ() <= z2;
    }
}
