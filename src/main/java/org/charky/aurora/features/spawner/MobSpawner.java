package org.charky.aurora.features.spawner;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.scheduler.BukkitTask;
import org.charky.aurora.core.Aurora;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class MobSpawner {
    // Nothing spawns unless a player is this close, mobs nobody sees only cost ticks
    private static final double ACTIVATION_RANGE = 64.0;
    // The spawn column and its four neighbours
    private static final int[][] SPAWN_COLUMNS = {{0, 0}, {1, 0}, {-1, 0}, {0, 1}, {0, -1}};

    private final Aurora plugin;
    private String name;
    private String world;
    private int x;
    private int y;
    private int z;
    private MobData mob;
    private double radius;
    private int limit;
    private int delay;
    private boolean active;
    private BukkitTask task;
    private int consecutiveFailures = 0;

    public MobSpawner(Aurora plugin, String name, String world, int x, int y, int z, MobData mob,
                      double radius, int limit, int delay, boolean active) {
        this.plugin = plugin;
        this.name = name;
        this.world = world;
        this.x = x;
        this.y = y;
        this.z = z;
        this.mob = mob;
        this.radius = radius;
        this.limit = limit;
        this.delay = delay;
        this.active = active;

        if (active) start();
    }

    // The first spawn comes one delay after starting, not straight away
    private void start() {
        stop();
        this.task = Bukkit.getScheduler().runTaskTimer(plugin, this::spawnMob, delay, delay);
    }

    public String getName() {
        return this.name;
    }

    public String getWorld() {
        return this.world;
    }

    public int getX() {
        return this.x;
    }

    public int getY() {
        return this.y;
    }

    public int getZ() {
        return this.z;
    }

    public String getMobName() {
        return this.mob.id();
    }

    public double getRadius() {
        return this.radius;
    }

    public int getLimit() {
        return this.limit;
    }

    public int getDelay() {
        return delay;
    }

    public boolean isActive() {
        return active;
    }

    public Location getLocation() {
        return new Location(Bukkit.getWorld(this.world), this.x, this.y, this.z);
    }

    public List<String> getInfo() {
        List<String> info = new ArrayList<>();
        info.add("Name: " + this.name);
        info.add("World: " + this.world);
        info.add("Coordinate: [" + this.x + ", " + this.y + ", " + this.z + "]");
        info.add("Mob: " + this.mob.id());
        info.add("Radius: " + this.radius);
        info.add("Limit: " + this.limit);
        info.add("Delay: " + this.delay);
        info.add("Active: " + this.active);
        return info;
    }

    public void setMob(MobData mob) {
        this.mob = mob;
    }

    public void setRadius(double radius) {
        this.radius = radius;
    }

    public void setLimit(int limit) {
        this.limit = limit;
    }

    // The running task still has the old period, so restart it on the new one
    public void setDelay(int delay) {
        this.delay = delay;
        if (this.active) start();
    }

    public void redefine(int x, int y, int z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public void rename(String newName) {
        this.name = newName;
    }

    public boolean toggle() {
        this.active = !this.active;
        if (this.active) start();
        else stop();

        return this.active;
    }

    public void spawnMob() {
        if (!active) return;
        // A boss waits for its key, see summon()
        if (plugin.getRegistry().isSummoned(mob.id())) return;

        World world = Bukkit.getWorld(this.world);
        if (world == null) return;

        Location origin = getLocation();

        // If nobody near, do not spawn mob to stop lag
        boolean watched = world.getPlayers().stream().anyMatch(p ->
                p.getLocation().distanceSquared(origin) < ACTIVATION_RANGE * ACTIVATION_RANGE);
        if (!watched) return;

        if (!world.isChunkLoaded(this.x >> 4, this.z >> 4)) return;
        if (plugin.getSpawnerManager().countFor(this.name) >= limit) return;

        Location spawnLocation = findSpawnLocation();
        if (spawnLocation == null) {
            if (++consecutiveFailures == 20) {
                plugin.getLogger().warning("Spawner '" + name + "' found no valid spawn location in 20 consecutive cycles");
            }
            return;
        }

        consecutiveFailures = 0;
        plugin.getSpawnerManager().spawnMob(this.mob.id(), spawnLocation, this.name);
    }

    // Wakes this spawner's mob now, for a summon key. Null if it worked, otherwise why not
    public String summon() {
        if (!active) return "This spawner is turned off";

        World world = Bukkit.getWorld(this.world);
        if (world == null || !world.isChunkLoaded(this.x >> 4, this.z >> 4)) return "This spawner is not loaded";
        if (plugin.getSpawnerManager().countFor(this.name) >= limit) return "It is already awake";

        Location spawnLocation = findSpawnLocation();
        if (spawnLocation == null) return "There is no room for it to rise";

        plugin.getSpawnerManager().spawnMob(this.mob.id(), spawnLocation, this.name);
        return null;
    }

    private Location findSpawnLocation() {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        World world = Bukkit.getWorld(this.world);

        // Attempt to find location up to 10 times
        for (int i = 0; i < 10; i++) {
            double x = this.x + (random.nextDouble() * radius * 2 - radius);
            double z = this.z + (random.nextDouble() * radius * 2 - radius);

            // Look for safe spot from 4 blocks below to 4 blocks above spawner
            for (int offset = -4; offset <= 4; offset++) {
                Location floor = new Location(world, x, this.y + offset, z);

                if (isSafeSpot(floor, 2)) {
                    return floor.add(0, 1, 0);
                }
            }
        }

        return null;
    }

    // A solid floor with the spawn column and its four neighbours free to the given
    // height. Passable rather than air, so grass and flowers don't block spawns
    public boolean isSafeSpot(Location floor, int height) {
        if (!floor.getBlock().getType().isSolid()) return false;

        for (int[] column : SPAWN_COLUMNS) {
            for (int y = 1; y <= height; y++) {
                Block block = floor.clone().add(column[0], y, column[1]).getBlock();
                if (!block.isPassable() || block.isLiquid()) return false;
            }
        }

        return true;
    }

    public void stop() {
        if (task != null) task.cancel();
        task = null;
    }
}
