package org.charky.aurora;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.scheduler.BukkitTask;
import org.charky.aurora.models.MineBlock;
import org.charky.aurora.models.MineData;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class Mine {
    private final Aurora plugin;
    private final MineData data;
    private List<MineBlock> blocks;
    private BukkitTask task;
    private int blocksBroken;
    private final int volume;

    public Mine(Aurora plugin, MineData data, List<MineBlock> blocks) {
        this.plugin = plugin;
        this.data = data;
        this.blocks = blocks;
        this.volume = (Math.abs(data.x1() - data.x2()) + 1) *
                      (Math.abs(data.y1() - data.y2()) + 1) *
                      (Math.abs(data.z1() - data.z2()) + 1);
    }

    public void setBlocks(List<MineBlock> blocks) {
        this.blocks = blocks;
    }

    public void startTimer() {
        stopTimer();
        this.task = Bukkit.getScheduler().runTaskTimer(this.plugin, this::resetMine, 0L, (long) this.data.delay());
    }

    public void stopTimer() {
        if (this.task != null) {
            this.task.cancel();
            this.task = null;
        }
    }

    public void handleBlockBreak() {
        this.blocksBroken++;
        if (((double) blocksBroken) / volume >= data.threshold()) {
            this.resetMine();
            this.blocksBroken = 0;
            this.startTimer();
        }
    }

    public boolean atLoc(Location location) {
        if (!this.data.world().equals(location.getWorld().getName())) return false;
        int x = location.getBlockX();
        int y = location.getBlockY();
        int z = location.getBlockZ();

        if (x < this.data.x1() || x > this.data.x2()) return false;
        if (y < this.data.y1() || y > this.data.y2()) return false;
        if (z < this.data.z1() || z > this.data.z2()) return false;
        return true;
    }

    public void resetMine() {
        World world = Bukkit.getWorld(this.data.world());
        if (world == null) return;

        for (int x = this.data.x1(); x <= this.data.x2(); x++) {
            for (int y = this.data.y1(); y <= this.data.y2(); y++) {
                for (int z = this.data.z1(); z <= this.data.z2(); z++) {
                    world.getBlockAt(x, y, z).setType(this.getRandomMaterial(), false);
                }
            }
        }

        plugin.getLogger().info(this.data.name() + " Reset Successfully!");
    }

    private Material getRandomMaterial() {
        double random = ThreadLocalRandom.current().nextDouble(100.0);
        double totalWeight = 0.0;

        for (MineBlock block : this.blocks) {
            totalWeight += block.percentage();

            if (random < totalWeight) {
                return block.material();
            }
        }

        return blocks.getLast().material();
    }
}
