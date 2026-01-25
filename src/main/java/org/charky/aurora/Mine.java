package org.charky.aurora;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.scheduler.BukkitTask;
import org.charky.aurora.models.MineBlock;
import org.charky.aurora.models.MineData;

import java.util.List;

public class Mine {
    private final Aurora plugin;
    private final MineData data;
    private List<MineBlock> composition;
    private BukkitTask task;
    private int blocksBroken;

    public Mine(Aurora plugin, MineData data, List<MineBlock> composition) {
        this.plugin = plugin;
        this.data = data;
        this.composition = composition;
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

    public void resetMine() {
        World world = Bukkit.getWorld(this.data.world());
        if (world == null) return;

        for (int x = this.data.x1(); x <= this.data.x2(); x++) {
            for (int y = this.data.y1(); y <= this.data.y2(); y++) {
                for (int z = this.data.z1(); z <= this.data.x2(); z++) {
                    world.getBlockAt(x, y, z).setType(Material.COBBLESTONE, false);
                }
            }
        }

        plugin.getLogger().info(this.data.name() + " Reset Successfully!");
    }
}
