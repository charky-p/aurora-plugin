package org.charky.aurora.features.mining;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.scheduler.BukkitTask;
import org.charky.aurora.Aurora;
import org.charky.aurora.managers.MineManager;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class Mine {
    private final Aurora plugin;
    private final MineManager manager;
    private final MineData data;
    private List<MineBlock> blocks;
    private BukkitTask task;
    private int blocksBroken;
    private final int volume;
    private long lastResetTick = -1;

    public Mine(Aurora plugin, MineManager manager, MineData data, List<MineBlock> blocks) {
        this.plugin = plugin;
        this.manager = manager;
        this.data = data;
        this.blocks = blocks;
        this.volume = (Math.abs(data.getX1() - data.getX2()) + 1) *
                      (Math.abs(data.getY1() - data.getY2()) + 1) *
                      (Math.abs(data.getZ1() - data.getZ2()) + 1);
    }

    public void setBlocks(List<MineBlock> blocks) {
        this.blocks = blocks;
    }

    public void startTimer() {
        stopTimer();
        long delayTicks = (long) this.data.getDelay();
        this.task = Bukkit.getScheduler().runTaskTimer(this.plugin, this::resetMine, delayTicks, delayTicks);
    }

    public void stopTimer() {
        if (this.task != null) {
            this.task.cancel();
            this.task = null;
        }
    }

    public void handleBlockBreak() {
        this.blocksBroken++;
        if (((double) blocksBroken) / volume * 100 < data.getThreshold()) return;

        // Ensure we only reset at most once a tick, e.g. blast pickaxe
        if (this.lastResetTick == Bukkit.getCurrentTick()) return;

        this.resetMine();
        this.startTimer();
    }

    public boolean atLoc(Location location) {
        if (!this.data.getWorld().equals(location.getWorld().getName())) return false;
        int x = location.getBlockX();
        int y = location.getBlockY();
        int z = location.getBlockZ();

        if (x < this.data.getX1() || x > this.data.getX2()) return false;
        if (y < this.data.getY1() || y > this.data.getY2()) return false;
        return z >= this.data.getZ1() && z <= this.data.getZ2();
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
