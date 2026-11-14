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
import org.charky.aurora.features.player.BackpackManager;
import org.charky.aurora.features.collection.CollectionManager;
import org.charky.aurora.features.player.Essence;
import org.charky.aurora.features.player.LevelManager;
import org.charky.aurora.features.player.PlayerManager;
import org.charky.aurora.features.player.ScoreboardManager;
import org.charky.aurora.common.loot.MenuItem;

import java.util.ArrayList;
import java.util.List;

public class MineListener implements Listener {
    // How far the block a player is mining can be from them
    private static final int REACH = 6;

    private final MineManager manager;
    private final PlayerManager players;
    private final ScoreboardManager scoreboard;
    private final BackpackManager backpackManager;
    private final DataRegistry registry;
    private final LevelManager levels;
    private final CollectionManager collections;

    public MineListener(Aurora plugin) {
        this.manager = plugin.getMineManager();
        this.players = plugin.getPlayerManager();
        this.scoreboard = plugin.getScoreboardManager();
        this.registry = plugin.getRegistry();
        this.backpackManager = plugin.getBackpackManager();
        this.levels = plugin.getLevelManager();
        this.collections = plugin.getCollectionManager();
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
        if (!registry.hasBlock(dropId)) return;

        int density = registry.getDensity(dropId);
        // Fortune only counts with the right tool, a pick on a log mines like a hand
        boolean rightTool = rightTool(pick, block.getType());
        int fortune = rightTool ? Enchants.level(pick, "fortune") : 0;
        if (fortune + 1 - density <= 0) {
            String needed = rightTool
                    ? "Requires Fortune " + density
                    : "Wrong tool, Fortune " + density + " " + toolFor(block.getType()) + " needed";
            player.sendActionBar(Component.text(needed, NamedTextColor.RED, TextDecoration.BOLD));
            return;
        }

        List<MenuItem> blockDrops = registry.getBlockDrops(dropId, fortune);
        backpackManager.get(player.getUniqueId()).collect(player, blockDrops);

        Essence.roll(player, Enchants.level(pick, "luck"), players);

        // Only break if it is a block with a drop. Counted towards the reset only
        // once broken, or swinging at a block you can't mine would reset the mine
        if (blockDrops.isEmpty()) return;

        players.addMined(player);
        levels.addXp(player, registry.getBlockXp(dropId));
        collections.add(player, blockDrops);
        block.setType(Material.AIR, false);
        manager.handleBlockBreak(block.getLocation());
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

    // Every block blast breaks besides the one mined, a patch on the face that was
    // hit, see BlastShape
    private List<Block> blastArea(Player player, Block center, int level) {
        BlockFace face = hitFace(player);
        BlockFace facing = player.getFacing();

        List<Block> area = new ArrayList<>();
        for (int[] offset : BlastShape.offsets(level, face.getModX(), face.getModY(), face.getModZ(),
                facing.getModX(), facing.getModZ())) {
            area.add(center.getRelative(offset[0], offset[1], offset[2]));
        }
        return area;
    }

    // The side of the block the player is looking at. If that can't be traced it is
    // worked out from where they look: steeply down is the top of a block, steeply up
    // the bottom, otherwise the side facing them
    private static BlockFace hitFace(Player player) {
        BlockFace traced = player.getTargetBlockFace(REACH);
        if (traced != null && traced.isCartesian()) return traced;

        float pitch = player.getLocation().getPitch();
        if (pitch > 45) return BlockFace.UP;
        if (pitch < -45) return BlockFace.DOWN;
        return player.getFacing().getOppositeFace();
    }
}
