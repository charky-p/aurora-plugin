package org.charky.aurora.features.mine;

import org.bukkit.Location;
import org.charky.aurora.common.util.Box;

public class MineData {
    private String name;
    private Box box;
    private double threshold;
    private int delay;
    private double tpX;
    private double tpY;
    private double tpZ;

    public MineData(String name, String world, int x1, int y1, int z1, int x2, int y2, int z2, double threshold, int delay, double tpX, double tpY, double tpZ) {
        this.name = name;
        this.box = new Box(world, x1, y1, z1, x2, y2, z2);
        this.threshold = threshold;
        this.delay = delay;
        this.tpX = tpX;
        this.tpY = tpY;
        this.tpZ = tpZ;
    }

    // A new mine between two corners given in any order. It teleports to just
    // outside its lowest corner until /aurora mine settp says otherwise
    public static MineData between(String name, Location location1, Location location2, double threshold, int delay) {
        Box box = Box.between(location1, location2);
        return new MineData(name, box.world(), box.x1(), box.y1(), box.z1(), box.x2(), box.y2(), box.z2(),
                threshold, delay, box.x1() - 0.5, box.y1() + 1, box.z1() - 0.5);
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setThreshold(double threshold) {
        this.threshold = threshold;
    }

    public void setDelay(int delay) {
        this.delay = delay;
    }

    // Corners in any order, the box sorts them
    public void setLocations(Location location1, Location location2) {
        this.box = Box.between(location1, location2);
    }

    public boolean overlaps(String world, int x1, int y1, int z1, int x2, int y2, int z2) {
        return box.overlaps(new Box(world, x1, y1, z1, x2, y2, z2));
    }

    public Box getBox() {
        return box;
    }

    public void updateTeleport(double tpX, double tpY, double tpZ) {
        this.tpX = tpX;
        this.tpY = tpY;
        this.tpZ = tpZ;
    }

    public String getName() {
        return name;
    }

    public String getWorld() {
        return box.world();
    }

    public int getX1() {
        return box.x1();
    }

    public int getY1() {
        return box.y1();
    }

    public int getZ1() {
        return box.z1();
    }

    public int getX2() {
        return box.x2();
    }

    public int getY2() {
        return box.y2();
    }

    public int getZ2() {
        return box.z2();
    }

    public double getThreshold() {
        return threshold;
    }

    public int getDelay() {
        return delay;
    }

    public double getTpX() {
        return tpX;
    }

    public double getTpY() {
        return tpY;
    }

    public double getTpZ() {
        return tpZ;
    }
}
