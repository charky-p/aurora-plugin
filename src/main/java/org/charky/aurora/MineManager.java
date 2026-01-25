package org.charky.aurora;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.scheduler.BukkitTask;
import org.charky.aurora.models.MineBlock;
import org.charky.aurora.models.MineData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MineManager {
    private final Aurora plugin;
    private final Database db;
//    private final Map<String, Mine> mineMap = new HashMap<>();
    private final Map<String, List<MineBlock>> blockMap = new HashMap<>();
    private final Map<String, BukkitTask> activeTasks = new HashMap<>();

    public MineManager(Aurora plugin, Database db) {
        this.plugin = plugin;
        this.db = db;
        // Create blockMap with db
        this.blockMap.putAll(db.getMineBlocks());

        // Initialise mine reset delays
        List<MineData> mines = db.getMines();
        for (MineData mine : mines) {
            startMineTimer(mine.name(), mine.world(), mine.x1(), mine.y1(), mine.z1(),
                    mine.x2(), mine.y2(), mine.z2(), mine.delay());
        }
    }

    public void addMine(String name, String world, int x1, int y1, int z1, int x2, int y2, int z2, double threshold, int delay) {
        if (exists(name)) return;

        db.addMine(name, world, x1, y1, z1, x2, y2, z2, threshold, delay);
        List<MineBlock> blocks = List.of(new MineBlock(Material.COBBLESTONE, 100.0));
        db.setBlocks(name, blocks);

        blockMap.put(name, blocks);

        startMineTimer(name, world, x1, y1, z1, x2, y2, z2, delay);
    }

    public boolean exists(String name) {
        return blockMap.containsKey(name);
    }

    public void startMineTimer(String name, String world, int x1, int y1, int z1, int x2, int y2, int z2, int delay) {
        stopMineTimer(name);

        BukkitTask task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            resetMine(world, x1, y1, z1, x2, y2, z2);
        }, 0L, (long) delay);

        activeTasks.put(name, task);
    }

    public void stopMineTimer(String name) {
        if (activeTasks.containsKey(name)) {
            activeTasks.get(name).cancel();
            activeTasks.remove(name);
        }
    }

    public List<String> getMines() {
        return new ArrayList<>(blockMap.keySet());
    }

    public void resetMine(String worldName, int x1, int y1, int z1, int x2, int y2, int z2) {
        World world = Bukkit.getWorld(worldName);
        if (world == null) return;

        for (int x = x1; x <= x2; x++) {
            for (int y = y1; y <= y2; y++) {
                for (int z = z1; z <= x2; z++) {
                    world.getBlockAt(x, y, z).setType(Material.COBBLESTONE, false);
                }
            }
        }
    }
}
