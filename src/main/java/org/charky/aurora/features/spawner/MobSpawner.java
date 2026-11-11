package org.charky.aurora.features.spawner;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.scheduler.BukkitTask;
import org.charky.aurora.core.Aurora;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class MobSpawner {
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
    private boolean active = true;
    private BukkitTask task;

    private static final double ACTIVATION_RANGE = 64.0;
    private int consecutiveFailures = 0;

    public MobSpawner(Aurora plugin, String name, String world, int x, int y, int z, MobData mob, double radius, int limit, int delay) {
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

        // Spawn mob with delay offset
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
        if (!this.active) return;

        if (this.task != null) this.task.cancel();
        this.task = Bukkit.getScheduler().runTaskTimer(plugin, this::spawnMob, delay, delay);
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
        if (this.active) {
            this.task = Bukkit.getScheduler().runTaskTimer(plugin, this::spawnMob, delay, delay);
        } else if (this.task != null) {
            this.task.cancel();
        }

        return this.active;
    }

    public void spawnMob() {
        if (!active) return;

        World world = Bukkit.getWorld(this.world);
        if (world == null) return;

        Location origin = getLocation();

        // If nobody near, do not spawn mob to stop lag
        boolean watched = world.getPlayers().stream().anyMatch(p ->
                p.getLocation().distanceSquared(origin) < ACTIVATION_RANGE * ACTIVATION_RANGE);
        if (!watched) return;

        if (!world.isChunkLoaded(this.x >> 4, this.z >> 4)) return;
        if (plugin.getMobManager().countFor(this.name) >= limit) return;

        Location spawnLocation = findSpawnLocation();
        if (spawnLocation == null) {
            if (++consecutiveFailures == 20) {
                plugin.getLogger().warning("Spawner '" + name + "' found no valid spawn location in 20 consecutive cycles");
            }
            return;
        }

        consecutiveFailures = 0;
        plugin.getMobManager().spawnMob(this.mob.id(), spawnLocation, this.name);
    }

    private Location findSpawnLocation() {
        Random random = new Random();
        World world = Bukkit.getWorld(this.world);

        // Attempt to find location up to 10 times
        for (int i = 0; i < 10; i++) {
            double x = this.x + (random.nextDouble() * radius * 2 - radius);
            double z = this.z + (random.nextDouble() * radius * 2 - radius);

            // Look for safe spot from 4 blocks below to 4 blocks above spawner
            for (int offset = -4; offset <= 4; offset++) {
                Location floor = new Location(world, x, this.y + offset, z);

                // Check if box of size 3 x 3 and height 2 is empty for spawn
                // space enough to fit a ravager
                // TODO: just check that adjacent blocks are free
                if (isSafeBox(floor, 3, 2)) {
                    return floor.add(0, 1, 0);
                }
            }
        }

        return null;
    }

    public boolean isSafeBox(Location floor, int width, int height) {
        if (floor.getBlock().getType().isAir()) return false;

        for (int x = - width / 2; x <= width / 2; x++) {
            for (int z = - width / 2; z <= width / 2; z++) {
                for (int y = 1; y <= height; y++) {
                    Block block = floor.clone().add(x, y, z).getBlock();
                    if (!block.getType().isAir()) return false;
                }
            }
        }

        return true;
    }

    public void stop() {
        if (task != null) task.cancel();
    }
}
