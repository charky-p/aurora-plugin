package org.charky.aurora.features.system.commands;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.charky.aurora.common.util.TextUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * A building helper: the top of every column around the player, the blocks that see
 * the sky, is replaced with a block a few layers deep. Paint an island's top red wool,
 * then swap the wool for a palette. One step of undo per player
 */
public class SkyLayerSub implements AuroraSubCommand {
    private static final int DEFAULT_DEPTH = 1;
    private static final int DEFAULT_RADIUS = 50;
    private static final int MAX_RADIUS = 200;
    private static final int MAX_DEPTH = 20;

    private record Change(Location at, BlockData before) {
    }

    private final Map<UUID, List<Change>> lastChange = new HashMap<>();

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            TextUtils.error(sender, "Stand by the island in game");
            return;
        }
        if (args.length == 1 && args[0].equalsIgnoreCase("undo")) {
            undo(player);
            return;
        }
        if (args.length < 1) {
            TextUtils.usage(sender, getFullUsage());
            return;
        }

        Material material = Material.matchMaterial(args[0]);
        if (material == null || !material.isBlock()) {
            TextUtils.error(sender, "No block called " + args[0]);
            return;
        }
        int depth, radius;
        try {
            depth = args.length > 1 ? Integer.parseInt(args[1]) : DEFAULT_DEPTH;
            radius = args.length > 2 ? Integer.parseInt(args[2]) : DEFAULT_RADIUS;
        } catch (NumberFormatException e) {
            TextUtils.usage(sender, getFullUsage());
            return;
        }
        if (depth < 1 || depth > MAX_DEPTH || radius < 1 || radius > MAX_RADIUS) {
            TextUtils.error(sender, "Depth is 1 to " + MAX_DEPTH + ", radius 1 to " + MAX_RADIUS);
            return;
        }

        World world = player.getWorld();
        Location centre = player.getLocation();
        BlockData placed = material.createBlockData();
        List<Change> changes = new ArrayList<>();
        for (int x = centre.getBlockX() - radius; x <= centre.getBlockX() + radius; x++) {
            for (int z = centre.getBlockZ() - radius; z <= centre.getBlockZ() + radius; z++) {
                // The highest block of the column is the one that sees the sky, then
                // the solid blocks straight under it
                Block block = world.getHighestBlockAt(x, z);
                // Past grass, carpet or a button sitting on top, to the ground under it
                while (!block.getType().isSolid() && !block.getType().isAir() && block.getY() > world.getMinHeight()) {
                    block = block.getRelative(0, -1, 0);
                }
                for (int layer = 0; layer < depth && block.getY() > world.getMinHeight(); layer++) {
                    if (!block.getType().isSolid()) break;
                    if (block.getType() != material) {
                        changes.add(new Change(block.getLocation(), block.getBlockData()));
                        block.setBlockData(placed, false);
                    }
                    block = block.getRelative(0, -1, 0);
                }
            }
        }
        lastChange.put(player.getUniqueId(), changes);
        TextUtils.success(sender, changes.size() + " blocks are now " + material.getKey().getKey()
                + ", " + depth + " deep within " + radius + " blocks. /a skylayer undo puts them back");
    }

    private void undo(Player player) {
        List<Change> changes = lastChange.remove(player.getUniqueId());
        if (changes == null || changes.isEmpty()) {
            TextUtils.error(player, "Nothing to undo");
            return;
        }
        for (Change change : changes) change.at().getBlock().setBlockData(change.before(), false);
        TextUtils.success(player, changes.size() + " blocks put back");
    }

    @Override
    public List<String> tabComplete(Player player, String[] args) {
        if (args.length == 1) {
            List<String> options = new ArrayList<>(List.of("undo"));
            Arrays.stream(Material.values()).filter(m -> m.isBlock() && !m.isLegacy())
                    .map(m -> m.getKey().getKey()).forEach(options::add);
            return TextUtils.getCompletions(args[0], options);
        }
        if (args.length == 2) return TextUtils.getCompletions(args[1], List.of("1", "2", "3"));
        if (args.length == 3) return TextUtils.getCompletions(args[2], List.of("25", "50", "100"));
        return List.of();
    }

    @Override
    public String getUsage() {
        return "<block|undo> [depth] [radius]";
    }

    @Override
    public String getName() {
        return "skylayer";
    }

    @Override
    public String getDescription() {
        return "Replace the top blocks that see the sky around you, a few layers deep";
    }
}
