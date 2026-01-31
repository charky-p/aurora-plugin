package org.charky.aurora.features.mining;

import org.bukkit.Location;

public class MineData {
    private String name;
    private String world;
    private int x1;
    private int y1;
    private int z1;
    private int x2;
    private int y2;
    private int z2;
    private double threshold;
    private int delay;
    private double tpX;
    private double tpY;
    private double tpZ;

    public MineData(String name, String world, int x1, int y1, int z1, int x2, int y2, int z2, double threshold, int delay, double tpX, double tpY, double tpZ) {
        this.name = name;
        this.world = world;
        this.x1 = x1;
        this.y1 = y1;
        this.z1 = z1;
        this.x2 = x2;
        this.y2 = y2;
        this.z2 = z2;
        this.threshold = threshold;
        this.delay = delay;
        this.tpX = tpX;
        this.tpY = tpY;
        this.tpZ = tpZ;
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

    public void setLocations(Location location1, Location location2) {
        this.world = location1.getWorld().toString();
        this.x1 = location1.getBlockX();
        this.y1 = location1.getBlockY();
        this.z1 = location1.getBlockZ();
        this.x2 = location2.getBlockX();
        this.y2 = location2.getBlockY();
        this.z2 = location2.getBlockZ();
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
        return world;
    }

    public int getX1() {
        return x1;
    }

    public int getY1() {
        return y1;
    }

    public int getZ1() {
        return z1;
    }

    public int getX2() {
        return x2;
    }

    public int getY2() {
        return y2;
    }

    public int getZ2() {
        return z2;
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
