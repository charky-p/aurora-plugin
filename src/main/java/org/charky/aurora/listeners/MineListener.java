package org.charky.aurora.listeners;

import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.charky.aurora.Aurora;
import org.charky.aurora.managers.InventoryManager;
import org.charky.aurora.managers.MineManager;
import org.charky.aurora.managers.ScoreboardManager;
import org.charky.aurora.features.mining.BlockPresets;
import org.charky.aurora.features.loot.MenuItem;

import java.util.ArrayList;
import java.util.List;

public class MineListener implements Listener {
    private final MineManager manager;
    private final ScoreboardManager scoreboard;
    private final InventoryManager inventoryManager;
    private final BlockPresets blockPresets;

    public MineListener(Aurora plugin) {
        this.manager = plugin.getMineManager();
        this.scoreboard = plugin.getScoreboardManager();
        this.inventoryManager = plugin.getInventoryManager();
        this.blockPresets = plugin.getBlockPresets();
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMine(BlockBreakEvent event) {
        Player player = event.getPlayer();
        event.setDropItems(false);
        event.setCancelled(true);

        List<MenuItem> drops = new ArrayList<>();
        processBlock(player, event.getBlock(), drops);

        // Blast mining
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item.getType() != Material.AIR && item.hasItemMeta() && item.lore() != null)  {
            boolean isBlast = item.lore().stream()
                    .anyMatch(line -> PlainTextComponentSerializer.plainText()
                            .serialize(line).contains("Blast"));

            // 3x3x3 mining
            if (isBlast) {
                Block center = event.getBlock();
                for (int x = -1; x <= 1; x++) {
                    for (int y = -1; y <= 1; y++) {
                        for (int z = -1; z <= 1; z++) {
                            if (x == 0 && y == 0 && z == 0) continue;
                            Block target = center.getRelative(x, y, z);
                            if (target.getType() != Material.BEDROCK && target.getType() != Material.AIR) {
                                processBlock(player, target, drops);
                            }
                        }
                    }
                }
            }
        }

        scoreboard.updateBoard(player);
        inventoryManager.addToInventory(player, drops);
    }

    private void processBlock(Player player, Block block, List<MenuItem> drops) {
        manager.handleBlockBreak(player, block.getLocation());

        String customId = manager.getBlockIdAt(block.getLocation());
        String dropId = customId == null ? block.getType().getKey().toString() : customId;

        if (blockPresets.hasBlock(dropId)) {
            drops.addAll(blockPresets.getDrops(dropId));
        }

        // Only break if it is a block with a drop
        if (!blockPresets.getDrops(dropId).isEmpty()) {
            scoreboard.incrementMined(player);
            block.setType(Material.AIR, false);
        }
    }
}
