package org.charky.aurora.features.portal;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

// A box in a world, anyone who walks into it is sent to the destination.
// The destination is null until it is set
public class Portal {
    private String name;
    private final String world;
    private final int x1, y1, z1, x2, y2, z2;
    private String destWorld;
    private double destX, destY, destZ;
    private float destYaw, destPitch;
    // Only players with this tag get through, null for anyone
    private String requiredTag;

    // Corners in any order, stored sorted
    public Portal(String name, String world, int ax, int ay, int az, int bx, int by, int bz) {
        this.name = name;
        this.world = world;
        this.x1 = Math.min(ax, bx);
        this.y1 = Math.min(ay, by);
        this.z1 = Math.min(az, bz);
        this.x2 = Math.max(ax, bx);
        this.y2 = Math.max(ay, by);
        this.z2 = Math.max(az, bz);
    }

    public boolean contains(String worldName, int x, int y, int z) {
        return world.equals(worldName)
                && x >= x1 && x <= x2 && y >= y1 && y <= y2 && z >= z1 && z <= z2;
    }

    public boolean contains(Location location) {
        return location.getWorld() != null && contains(location.getWorld().getName(),
                location.getBlockX(), location.getBlockY(), location.getBlockZ());
    }

    public long volume() {
        return (long) (x2 - x1 + 1) * (y2 - y1 + 1) * (z2 - z1 + 1);
    }

    public void setDestination(String world, double x, double y, double z, float yaw, float pitch) {
        this.destWorld = world;
        this.destX = x;
        this.destY = y;
        this.destZ = z;
        this.destYaw = yaw;
        this.destPitch = pitch;
    }

    public boolean hasDestination() {
        return destWorld != null;
    }

    // Null when unset or its world is not loaded
    public Location getDestination() {
        if (destWorld == null) return null;
        World bukkitWorld = Bukkit.getWorld(destWorld);
        if (bukkitWorld == null) return null;
        return new Location(bukkitWorld, destX, destY, destZ, destYaw, destPitch);
    }

    // Standing in the middle of the floor, for /aurora portal tp
    public Location getCentre() {
        World bukkitWorld = Bukkit.getWorld(world);
        if (bukkitWorld == null) return null;
        return new Location(bukkitWorld, (x1 + x2 + 1) / 2.0, y1, (z1 + z2 + 1) / 2.0);
    }

    public void setRequiredTag(String tag) {
        this.requiredTag = tag == null || tag.isBlank() ? null : tag.toLowerCase();
    }

    public void rename(String name) {
        this.name = name;
    }

    public String getName() { return name; }
    public String getWorld() { return world; }
    public int getX1() { return x1; }
    public int getY1() { return y1; }
    public int getZ1() { return z1; }
    public int getX2() { return x2; }
    public int getY2() { return y2; }
    public int getZ2() { return z2; }
    public String getDestWorld() { return destWorld; }
    public double getDestX() { return destX; }
    public double getDestY() { return destY; }
    public double getDestZ() { return destZ; }
    public float getDestYaw() { return destYaw; }
    public float getDestPitch() { return destPitch; }
    public String getRequiredTag() { return requiredTag; }
}
