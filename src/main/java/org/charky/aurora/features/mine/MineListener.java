package org.charky.aurora.features.mine;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.RayTraceResult;
import org.charky.aurora.common.util.Enchants;
import org.charky.aurora.core.Aurora;
import org.charky.aurora.core.DataRegistry;
import org.charky.aurora.features.player.BackpackData;
import org.charky.aurora.features.player.BackpackManager;
import org.charky.aurora.features.player.Essence;
import org.charky.aurora.features.player.PlayerManager;
import org.charky.aurora.features.player.ScoreboardManager;
import org.charky.aurora.common.loot.MenuItem;

import java.util.ArrayList;
import java.util.List;

public class MineListener implements Listener {
    // Blast level -> width, height, depth broken, laid flat against the face you
    // mine. Even sizes grow to your right and upward. Past the table the last holds.
    // Kept in step with BLAST_SHAPES in scripts/balance/balance_sim.py
    private static final int[][] BLAST_SHAPES = {
            {2, 1, 1},
            {2, 2, 1},
            {3, 3, 1},
    };

    private final MineManager manager;
    private final PlayerManager players;
    private final ScoreboardManager scoreboard;
    private final BackpackManager backpackManager;
    private final DataRegistry registry;

    public MineListener(Aurora plugin) {
        this.manager = plugin.getMineManager();
        this.players = plugin.getPlayerManager();
        this.scoreboard = plugin.getScoreboardManager();
        this.registry = plugin.getRegistry();
        this.backpackManager = plugin.getBackpackManager();
    }

    // HIGH, not MONITOR: this changes the event, and MONITOR is for watching only
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onMine(BlockBreakEvent event) {
        Player player = event.getPlayer();
        event.setDropItems(false);
        // TODO: temp for building
        if (!player.hasPermission("aurora.admin")) event.setCancelled(true);

        ItemStack pick = player.getInventory().getItemInMainHand();
        processBlock(player, pick, event.getBlock());

        int blast = Enchants.level(pick, "blast");
        if (blast > 0) {
            for (Block target : blastArea(player, event.getBlock(), blast)) {
                if (target.getType() != Material.BEDROCK && target.getType() != Material.AIR) {
                    processBlock(player, pick, target);
                }
            }
        }

        scoreboard.updateBoard(player);
    }

    private void processBlock(Player player, ItemStack pick, Block block) {
        manager.handleBlockBreak(player, block.getLocation());

        String dropId = manager.getBlockIdAt(block.getLocation());

        if (registry.hasBlock(dropId)) {
            int density = registry.getDensity(dropId);
            int fortune = Enchants.level(pick, "fortune");
            if (fortune + 1 - density <= 0) {
                player.sendActionBar(Component.text("Requires Fortune " + density, NamedTextColor.RED, TextDecoration.BOLD));
                return;
            }

            List<MenuItem> blockDrops = registry.getBlockDrops(dropId, fortune);
            BackpackData backpack = backpackManager.get(player.getUniqueId());
            backpack.addItems(blockDrops);
            if (!blockDrops.isEmpty()) player.sendActionBar(blockDrops.getLast().getItem().displayName().append(Component.text(" " + backpack.getAmount(player, blockDrops.getLast().getId()), NamedTextColor.GRAY, TextDecoration.BOLD)));

            Essence.roll(player, Enchants.level(pick, "luck"), players);

            // Only break if it is a block with a drop
            if (!blockDrops.isEmpty()) {
                players.addMined(player);
                block.setType(Material.AIR, false);
            }
        }
    }

    // Every block blast breaks besides the one mined. Width runs to your right,
    // height runs up a wall, or away from you across a floor or ceiling, depth
    // runs into the block
    private List<Block> blastArea(Player player, Block center, int level) {
        int[] shape = BLAST_SHAPES[Math.min(level, BLAST_SHAPES.length) - 1];
        BlockFace face = minedFace(player);
        boolean flat = face == BlockFace.UP || face == BlockFace.DOWN;

        int[] depth = {-face.getModX(), -face.getModY(), -face.getModZ()};
        BlockFace facing = player.getFacing();
        int[] forward = flat ? new int[]{facing.getModX(), 0, facing.getModZ()} : depth;
        int[] right = {-forward[2], 0, forward[0]};
        int[] up = flat ? forward : new int[]{0, 1, 0};

        List<Block> area = new ArrayList<>();
        for (int w = -(shape[0] - 1) / 2; w <= shape[0] / 2; w++) {
            for (int h = -(shape[1] - 1) / 2; h <= shape[1] / 2; h++) {
                for (int d = 0; d < shape[2]; d++) {
                    if (w == 0 && h == 0 && d == 0) continue;
                    area.add(center.getRelative(
                            w * right[0] + h * up[0] + d * depth[0],
                            w * right[1] + h * up[1] + d * depth[1],
                            w * right[2] + h * up[2] + d * depth[2]));
                }
            }
        }
        return area;
    }

    // The face the player is looking at, falling back to their pitch if the ray misses
    private static BlockFace minedFace(Player player) {
        RayTraceResult hit = player.rayTraceBlocks(6.0);
        if (hit != null && hit.getHitBlockFace() != null) return hit.getHitBlockFace();

        float pitch = player.getLocation().getPitch();
        if (pitch > 45) return BlockFace.UP;
        if (pitch < -45) return BlockFace.DOWN;
        return player.getFacing().getOppositeFace();
    }
}
