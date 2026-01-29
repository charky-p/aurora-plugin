package org.charky.aurora.features.mob;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.scheduler.BukkitTask;
import org.charky.aurora.Aurora;

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
    private int activeMobs = 0;
    private BukkitTask task;

    public MobSpawner(Aurora plugin, String name, String world, int x, int y, int z, MobData mob, double radius, int limit) {
        this.plugin = plugin;
        this.name = name;
        this.world = world;
        this.x = x;
        this.y = y;
        this.z = z;
        this.mob = mob;
        this.radius = radius;
        this.limit = limit;

        // Spawn mob every 10 seconds
        this.task = Bukkit.getScheduler().runTaskTimer(plugin, this::spawnMob, 100L, 200L);
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

    public void spawnMob() {
        if (activeMobs >= limit) return;

        Location spawnLocation = this.findSpawnLocation();
        if (spawnLocation == null) return;
        this.plugin.getMobManager().spawnMob(this.mob.id(), spawnLocation, this.name);
        activeMobs++;
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

    public void decrementCount() {
        if (this.activeMobs > 0) this.activeMobs--;
    }

    public void stop() {
        if (task != null) task.cancel();
    }
}
