package org.charky.aurora.features.portal;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.charky.aurora.common.util.Box;

// A box in a world, anyone who walks into it is sent to the destination.
// The destination is null until it is set
public class Portal {
    private String name;
    private final Box box;
    private String destWorld;
    private double destX, destY, destZ;
    private float destYaw, destPitch;
    // Only players with this tag get through, null for anyone
    private String requiredTag;

    // Corners in any order, stored sorted
    public Portal(String name, String world, int ax, int ay, int az, int bx, int by, int bz) {
        this(name, new Box(world, ax, ay, az, bx, by, bz));
    }

    public Portal(String name, Box box) {
        this.name = name;
        this.box = box;
    }

    public boolean contains(String worldName, int x, int y, int z) {
        return box.contains(worldName, x, y, z);
    }

    public boolean contains(Location location) {
        return box.contains(location);
    }

    public Box getBox() {
        return box;
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
        World bukkitWorld = Bukkit.getWorld(box.world());
        if (bukkitWorld == null) return null;
        return new Location(bukkitWorld, (box.x1() + box.x2() + 1) / 2.0, box.y1(), (box.z1() + box.z2() + 1) / 2.0);
    }

    public void setRequiredTag(String tag) {
        this.requiredTag = tag == null || tag.isBlank() ? null : tag.toLowerCase();
    }

    public void rename(String name) {
        this.name = name;
    }

    public String getName() { return name; }
    public String getWorld() { return box.world(); }
    public int getX1() { return box.x1(); }
    public int getY1() { return box.y1(); }
    public int getZ1() { return box.z1(); }
    public int getX2() { return box.x2(); }
    public int getY2() { return box.y2(); }
    public int getZ2() { return box.z2(); }
    public String getDestWorld() { return destWorld; }
    public double getDestX() { return destX; }
    public double getDestY() { return destY; }
    public double getDestZ() { return destZ; }
    public float getDestYaw() { return destYaw; }
    public float getDestPitch() { return destPitch; }
    public String getRequiredTag() { return requiredTag; }
}
