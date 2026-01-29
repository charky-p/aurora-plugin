package org.charky.aurora.managers;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.charky.aurora.Aurora;
import org.charky.aurora.Database;
import org.charky.aurora.features.mining.Mine;
import org.charky.aurora.features.mining.MineBlock;
import org.charky.aurora.features.mining.MineData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MineManager {
    private final Aurora plugin;
    private final Database db;
    private final ScoreboardManager scoreboard;
    private final Map<String, Mine> mineMap = new HashMap<>();
    private final Map<Location, String> blockIdMap = new HashMap<>();

    public MineManager(Aurora plugin, Database db, ScoreboardManager scoreboard) {
        this.plugin = plugin;
        this.db = db;
        this.scoreboard = scoreboard;

        Map<String, List<MineBlock>> blockMap = db.getMineBlocks();
        List<MineData> mines = db.getMines();
        for (MineData mineData : mines) {
            Mine mine = new Mine(plugin, this, mineData, blockMap.get(mineData.getName()));
            this.mineMap.put(mineData.getName(), mine);
            mine.resetMine();
            mine.startTimer();
        }
    }

    public void addMine(String name, String world, int x1, int y1, int z1, int x2, int y2, int z2, double threshold, int delay) {
        if (exists(name)) return;

        MineData data = new MineData(name, world, x1, y1, z1, x2, y2, z2, threshold, delay, x1 - 0.5, y1 + 1, z1 - 0.5);
        List<MineBlock> defaultBlocks = List.of(new MineBlock(Material.COBBLESTONE, 100.0, "minecraft:cobblestone"));

        this.db.addMine(data);
        this.db.setBlocks(name, defaultBlocks);

        Mine mine = new Mine(this.plugin, this, data, defaultBlocks);
        this.mineMap.put(name, mine);
        mine.resetMine();
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

    public void setBlocks(String name, List<MineBlock> blocks) {
        Mine mine = this.mineMap.get(name);
        if (mine == null) return;

        mine.setBlocks(blocks);
        this.db.setBlocks(name, blocks);
    }

    public void resetMine(String name) {
        Mine mine = this.mineMap.get(name);
        if (mine == null) return;

        mine.resetMine();
        mine.startTimer();
    }

    public void handleBlockBreak(Player player, Location location) {
        Mine mine = this.getMineAt(location);
        if (mine == null) return;
        mine.handleBlockBreak();
        scoreboard.incrementMined(player);
    }

    public Mine getMineAt(Location location) {
        for (Mine mine : this.mineMap.values()) {
            if (mine.atLoc(location)) return mine;
        }
        return null;
    }

    public String getBlockIdAt(Location location) {
        if (!blockIdMap.containsKey(location)) return null;
        return blockIdMap.get(location);
    }

    public void registerBlock(Location location, String id) {
        this.blockIdMap.put(location, id);
    }

    public void tpMine(Player player, String mineName) {
        Mine mine = this.mineMap.get(mineName);
        if (mine == null) return;

        Location location = mine.getTpLocation();
        if (location != null && location.getWorld() != null) {
            player.teleport(location);
        } else {
            player.sendMessage(Component.text("ERROR: Mine location was invalid")
                    .color(NamedTextColor.RED));
        }
    }

    public void setTp(String mineName, double x, double y, double z) {
        Mine mine = mineMap.get(mineName);
        if (mine == null) return;

        mine.setTp(x, y, z);
        db.setTp(mineName, x, y, z);
    }

    public void shutdown() {
        for (Mine mine : this.mineMap.values()) {
            mine.stopTimer();
        }
        this.mineMap.clear();
    }
}
