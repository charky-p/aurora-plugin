package org.charky.aurora.features.mine;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;
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
    // Blast level -> width, length, depth broken, always a flat layer at the height of
    // the block you mine, whichever face you hit. Width runs to your right, length
    // away from you, depth down. Even sizes grow to your right and away. Past the
    // table the last holds. Kept in step with BLAST_SHAPES in scripts/balance/balance_sim.py
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
        // Players never really break blocks, the mine sets them to air itself below,
        // so nothing outside a mine can be broken. Admins break for real to build
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

    // Players can't break anything outside a mine, so a placed block would stay for
    // good, and one placed in a mined out spot would pay out that mine's drop
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        if (!event.getPlayer().hasPermission("aurora.admin")) event.setCancelled(true);
    }

    private void processBlock(Player player, ItemStack pick, Block block) {
        String dropId = manager.getBlockIdAt(block.getLocation());

        if (registry.hasBlock(dropId)) {
            int density = registry.getDensity(dropId);
            // Fortune only counts with the right tool, a pick on a log mines like a hand
            int fortune = rightTool(pick, block.getType()) ? Enchants.level(pick, "fortune") : 0;
            if (fortune + 1 - density <= 0) {
                player.sendActionBar(rightTool(pick, block.getType())
                        ? Component.text("Requires Fortune " + density, NamedTextColor.RED, TextDecoration.BOLD)
                        : Component.text("Wrong tool, Fortune " + density + " " + toolFor(block.getType()) + " needed", NamedTextColor.RED, TextDecoration.BOLD));
                return;
            }

            List<MenuItem> blockDrops = registry.getBlockDrops(dropId, fortune);
            BackpackData backpack = backpackManager.get(player.getUniqueId());
            backpack.addItems(blockDrops);
            if (!blockDrops.isEmpty()) player.sendActionBar(blockDrops.getLast().getItem().displayName().append(Component.text(" " + backpack.getAmount(player, blockDrops.getLast().getId()), NamedTextColor.GRAY, TextDecoration.BOLD)));

            Essence.roll(player, Enchants.level(pick, "luck"), players);

            // Only break if it is a block with a drop. Counted towards the reset only
            // once broken, or swinging at a block you can't mine would reset the mine
            if (!blockDrops.isEmpty()) {
                players.addMined(player);
                block.setType(Material.AIR, false);
                manager.handleBlockBreak(block.getLocation());
            }
        }
    }

    // Does this tool's own rules cover the block. Read from the registry's copy of
    // the item, so tools made before a rule changed follow the current rules
    private boolean rightTool(ItemStack tool, Material block) {
        String id = DataRegistry.itemId(tool);
        ItemStack template = id == null ? null : registry.getAllItems().get(id);
        ItemStack source = template != null ? template : tool;
        if (source == null || !source.hasItemMeta() || !source.getItemMeta().hasTool()) return false;
        return source.getItemMeta().getTool().getRules().stream()
                .anyMatch(rule -> rule.getBlocks().contains(block));
    }

    private static String toolFor(Material block) {
        if (Tag.MINEABLE_AXE.isTagged(block)) return "axe";
        if (Tag.MINEABLE_SHOVEL.isTagged(block)) return "shovel";
        return "pickaxe";
    }

    // Every block blast breaks besides the one mined, a flat layer whatever face
    // was hit. Width runs to your right, length away from you, depth down
    private List<Block> blastArea(Player player, Block center, int level) {
        int[] shape = BLAST_SHAPES[Math.min(level, BLAST_SHAPES.length) - 1];
        BlockFace facing = player.getFacing();
        int[] forward = {facing.getModX(), 0, facing.getModZ()};
        int[] right = {-forward[2], 0, forward[0]};
        int[] length = forward;
        int[] depth = {0, -1, 0};

        List<Block> area = new ArrayList<>();
        for (int w = -(shape[0] - 1) / 2; w <= shape[0] / 2; w++) {
            for (int h = -(shape[1] - 1) / 2; h <= shape[1] / 2; h++) {
                for (int d = 0; d < shape[2]; d++) {
                    if (w == 0 && h == 0 && d == 0) continue;
                    area.add(center.getRelative(
                            w * right[0] + h * length[0] + d * depth[0],
                            w * right[1] + h * length[1] + d * depth[1],
                            w * right[2] + h * length[2] + d * depth[2]));
                }
            }
        }
        return area;
    }
}
