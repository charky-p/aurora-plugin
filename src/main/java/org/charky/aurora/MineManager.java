package org.charky.aurora;

import org.bukkit.Material;
import org.charky.aurora.models.MineBlock;
import org.charky.aurora.models.MineData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MineManager {
    private final Aurora plugin;
    private final Database db;
    private final Map<String, Mine> mineMap = new HashMap<>();

    public MineManager(Aurora plugin, Database db) {
        this.plugin = plugin;
        this.db = db;

        Map<String, List<MineBlock>> blockMap = db.getMineBlocks();
        List<MineData> mines = db.getMines();
        for (MineData mine : mines) {
            this.mineMap.put(mine.name(), new Mine(plugin, mine, blockMap.get(mine.name())));
        }
    }

    public void addMine(String name, String world, int x1, int y1, int z1, int x2, int y2, int z2, double threshold, int delay) {
        if (exists(name)) return;

        MineData data = new MineData(name, world, x1, y1, z1, x2, y2, z2, threshold, delay);
        List<MineBlock> defaultBlocks = List.of(new MineBlock(Material.COBBLESTONE, 100.0));

        this.db.addMine(data);
        this.db.setBlocks(name, defaultBlocks);

        Mine mine = new Mine(this.plugin, data, defaultBlocks);
        this.mineMap.put(name, mine);
        mine.startTimer();
    }

    public void removeMine(String name) {
        Mine mine = this.mineMap.get(name);
        if (mine == null) return;

        mine.stopTimer();

        this.mineMap.remove(name);
        this.db.removeMine(name);
    }

    public boolean exists(String name) {
        return this.mineMap.containsKey(name);
    }

    public List<String> getMines() {
        return new ArrayList<>(this.mineMap.keySet());
    }

    public void shutdown() {
        for (Mine mine : this.mineMap.values()) {
            mine.stopTimer();
        }
        this.mineMap.clear();
    }
}
