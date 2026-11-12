package org.charky.aurora.features.mine;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.charky.aurora.core.Aurora;
import org.charky.aurora.core.database.MineStore;
import org.charky.aurora.features.mine.commands.MineException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class MineManager {
    private static final int MAX_MINE_VOLUME = 100000;
    private static final List<MineBlock> DEFAULT_BLOCKS =
            List.of(new MineBlock(Material.COBBLESTONE, 100.0, "minecraft:cobblestone"));

    private final Aurora plugin;
    private final MineStore store;
    // Case-insensitive, so commands find a mine however its name is typed.
    // The store keys on the saved casing, so store calls use the mine's own name
    private final Map<String, Mine> mineMap = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
    private final Map<Location, String> blockIdMap = new HashMap<>();

    public MineManager(Aurora plugin, MineStore store) {
        this.plugin = plugin;
        this.store = store;

        Map<String, List<MineBlock>> blockMap = store.loadBlocks();
        List<MineData> mines = store.loadAll();
        for (MineData mineData : mines) {
            // A mine saved without blocks would throw on its first reset
            List<MineBlock> blocks = blockMap.getOrDefault(mineData.getName(), DEFAULT_BLOCKS);
            Mine mine = new Mine(plugin, this, mineData, blocks);
            this.mineMap.put(mineData.getName(), mine);
            mine.resetMine();
            mine.startTimer();
        }
    }

    public void addMine(String name, Location location1, Location location2, double threshold, int delay) throws MineException {
        if (exists(name)) return;
        validateArea(name, location1, location2);

        String world = location1.getWorld().getName();
        int x1 = Math.min(location1.getBlockX(), location2.getBlockX());
        int y1 = Math.min(location1.getBlockY(), location2.getBlockY());
        int z1 = Math.min(location1.getBlockZ(), location2.getBlockZ());
        int x2 = Math.max(location1.getBlockX(), location2.getBlockX());
        int y2 = Math.max(location1.getBlockY(), location2.getBlockY());
        int z2 = Math.max(location1.getBlockZ(), location2.getBlockZ());

        MineData data = new MineData(name, world, x1, y1, z1, x2, y2, z2, threshold, delay, x1 - 0.5, y1 + 1, z1 - 0.5);

        this.store.add(data);
        this.store.setBlocks(name, DEFAULT_BLOCKS);

        Mine mine = new Mine(this.plugin, this, data, DEFAULT_BLOCKS);
        this.mineMap.put(name, mine);
        mine.resetMine();
        mine.startTimer();
    }

    // Shared by create and redefine. Overlapping mines would fight over the same
    // blocks, each resetting and paying out for the other
    private void validateArea(String name, Location location1, Location location2) throws MineException {
        String world = location1.getWorld().getName();
        if (!world.equals(location2.getWorld().getName())) throw new MineException("Positions must be in the same world");

        int x1 = Math.min(location1.getBlockX(), location2.getBlockX());
        int y1 = Math.min(location1.getBlockY(), location2.getBlockY());
        int z1 = Math.min(location1.getBlockZ(), location2.getBlockZ());
        int x2 = Math.max(location1.getBlockX(), location2.getBlockX());
        int y2 = Math.max(location1.getBlockY(), location2.getBlockY());
        int z2 = Math.max(location1.getBlockZ(), location2.getBlockZ());

        if ((long) (x2 - x1 + 1) * (y2 - y1 + 1) * (z2 - z1 + 1) > MAX_MINE_VOLUME) {
            throw new MineException("Mine volume exceeds limit of " + MAX_MINE_VOLUME);
        }

        for (Mine other : mineMap.values()) {
            MineData data = other.getData();
            if (data.getName().equalsIgnoreCase(name)) continue;
            if (data.overlaps(world, x1, y1, z1, x2, y2, z2)) throw new MineException("Overlaps mine '" + data.getName() + "'");
        }
    }

    public void removeMine(String name) {
        Mine mine = this.mineMap.get(name);
        if (mine == null) return;

        mine.stopTimer();
        forgetBlocks(mine);

        this.mineMap.remove(name);
        this.store.remove(mine.getData().getName());
    }

    // Registered blocks outlive their mine, so a removed or moved mine would keep
    // paying out drops where it used to be
    private void forgetBlocks(Mine mine) {
        this.blockIdMap.keySet().removeIf(mine::atLoc);
    }

    public boolean exists(String name) {
        return this.mineMap.containsKey(name);
    }

    public Mine getMine(String name) {
        return mineMap.get(name);
    }

    public List<String> getMines() {
        return new ArrayList<>(this.mineMap.keySet());
    }

    public void setBlocks(String name, List<MineBlock> blocks) {
        Mine mine = this.mineMap.get(name);
        if (mine == null) return;

        mine.setBlocks(blocks);
        this.store.setBlocks(mine.getData().getName(), blocks);
    }

    public void setThreshold(String name, double threshold) {
        Mine mine = this.mineMap.get(name);
        if (mine == null) return;

        mine.setThreshold(threshold);
        this.store.save(mine.getData());
    }

    public void setDelay(String name, int delay) {
        Mine mine = this.mineMap.get(name);
        if (mine == null) return;

        mine.setDelay(delay);
        this.store.save(mine.getData());

        // The running timer still has the old period
        mine.startTimer();
    }

    public void redefineMine(String name, Location location1, Location location2) throws MineException {
        Mine mine = this.mineMap.get(name);
        if (mine == null) return;
        validateArea(name, location1, location2);

        forgetBlocks(mine);
        mine.setLocations(location1, location2);
        this.store.save(mine.getData());
    }

    public void renameMine(String oldName, String newName) {
        Mine mine = this.mineMap.get(oldName);
        if (mine == null) return;

        String savedName = mine.getData().getName();
        mine.rename(newName);
        mineMap.remove(oldName);
        mineMap.put(newName, mine);
        this.store.rename(savedName, newName);
    }

    public void resetMine(String name) {
        Mine mine = this.mineMap.get(name);
        if (mine == null) return;

        mine.resetMine();
        mine.startTimer();
    }

    public void resetAll() {
        for (Mine mine : mineMap.values()) {
            mine.resetMine();
            mine.startTimer();
        }
    }

    public void handleBlockBreak(Player player, Location location) {
        Mine mine = this.getMineAt(location);
        if (mine == null) return;
        mine.handleBlockBreak();
    }

    public Mine getMineAt(Location location) {
        for (Mine mine : this.mineMap.values()) {
            if (mine.atLoc(location)) return mine;
        }
        return null;
    }

    // Only blocks inside a live mine count, whatever is still registered
    public String getBlockIdAt(Location location) {
        if (getMineAt(location) == null) return null;
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
            location.setYaw(player.getYaw());
            location.setPitch(player.getPitch());
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
        store.setTp(mineName, x, y, z);
    }

    public void shutdown() {
        for (Mine mine : this.mineMap.values()) {
            mine.stopTimer();
        }
        this.mineMap.clear();
    }
}
