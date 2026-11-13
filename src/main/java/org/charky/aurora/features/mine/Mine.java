package org.charky.aurora.features.mine;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.charky.aurora.core.Aurora;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class Mine {
    // One second, matches the floor MineCreateSub already uses
    private static final long MIN_DELAY_TICKS = 20L;

    private final Aurora plugin;
    private final MineManager manager;
    private final MineData data;
    private List<MineBlock> blocks;
    private BukkitTask task;
    private int blocksBroken;
    private long lastResetTick = -1;

    public Mine(Aurora plugin, MineManager manager, MineData data, List<MineBlock> blocks) {
        this.plugin = plugin;
        this.manager = manager;
        this.data = data;
        this.blocks = blocks;
    }

    public void setBlocks(List<MineBlock> blocks) {
        this.blocks = blocks;
    }

    public void setThreshold(double threshold) {
        data.setThreshold(threshold);
    }

    public void setDelay(int delay) {
        data.setDelay(delay);
    }

    public void rename(String name) {
        data.setName(name);
    }

    public MineData getData() {
        return data;
    }

    public void setLocations(Location location1, Location location2) {
        data.setLocations(location1, location2);
    }

    public void startTimer() {
        stopTimer();
        // A delay of 0 or less would reset the mine every tick
        long delayTicks = Math.max(MIN_DELAY_TICKS, this.data.getDelay());
        this.task = Bukkit.getScheduler().runTaskTimer(this.plugin, this::timedReset, delayTicks, delayTicks);
    }

    public void stopTimer() {
        if (this.task != null) {
            this.task.cancel();
            this.task = null;
        }
    }

    // Filling unloaded chunks would load them on the main thread every delay, for a
    // mine nobody is near. Skipped, the next timer resets it once someone is close
    private void timedReset() {
        World world = Bukkit.getWorld(this.data.getWorld());
        if (world == null || !chunksLoaded(world)) return;
        resetMine();
    }

    private boolean chunksLoaded(World world) {
        for (int cx = this.data.getX1() >> 4; cx <= this.data.getX2() >> 4; cx++) {
            for (int cz = this.data.getZ1() >> 4; cz <= this.data.getZ2() >> 4; cz++) {
                if (!world.isChunkLoaded(cx, cz)) return false;
            }
        }
        return true;
    }

    public void handleBlockBreak() {
        this.blocksBroken++;
        if ((double) blocksBroken / data.getBox().volume() * 100 < data.getThreshold()) return;

        // Ensure we only reset at most once a tick, e.g. blast pickaxe
        if (this.lastResetTick == Bukkit.getCurrentTick()) return;

        this.resetMine();
        this.startTimer();
    }

    public boolean atLoc(Location location) {
        return data.getBox().contains(location);
    }

    public Location getTpLocation() {
        World world = Bukkit.getWorld(this.data.getWorld());
        if (world == null) return null;

        return new Location(world, this.data.getTpX(), this.data.getTpY(), this.data.getTpZ());
    }

    public void setTp(double x, double y, double z) {
        data.updateTeleport(x, y, z);
    }

    public void resetMine() {
        this.blocksBroken = 0;
        this.lastResetTick = Bukkit.getCurrentTick();
        World world = Bukkit.getWorld(this.data.getWorld());
        if (world == null) return;

        for (int x = this.data.getX1(); x <= this.data.getX2(); x++) {
            for (int y = this.data.getY1(); y <= this.data.getY2(); y++) {
                for (int z = this.data.getZ1(); z <= this.data.getZ2(); z++) {
                    MineBlock choice = this.getRandomBlock();
                    world.getBlockAt(x, y, z).setType(choice.material(), false);

                    Location location = new Location(world, x, y, z);
                    manager.registerBlock(location, choice.id());
                }
            }
        }

        for (Player player : world.getPlayers()) {
            if (!isInside(player.getLocation())) continue;

            Location tpLocation = new Location(world, data.getTpX(), data.getTpY(), data.getTpZ(),
                    player.getLocation().getYaw(), player.getLocation().getPitch());
            player.teleport(tpLocation);
        }
    }

    private boolean isInside(Location location) {
        return location.getX() >= data.getX1() && location.getX() <= data.getX2() + 1 &&
                location.getY() >= data.getY1() && location.getY() <= data.getY2() + 1 &&
                location.getZ() >= data.getZ1() && location.getZ() <= data.getZ2() + 1;
    }

    private MineBlock getRandomBlock() {
        double random = ThreadLocalRandom.current().nextDouble(100.0);
        double totalWeight = 0.0;

        for (MineBlock block : this.blocks) {
            totalWeight += block.percentage();

            if (random < totalWeight) {
                return block;
            }
        }

        return blocks.getLast();
    }
}
