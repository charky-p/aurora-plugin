package org.charky.aurora.listeners;

import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.charky.aurora.MineManager;

public class MineListener implements Listener {
    private final MineManager manager;
    private final ScoreboardListener scoreboard;

    public MineListener(MineManager manager, ScoreboardListener scoreboard) {
        this.manager = manager;
        this.scoreboard = scoreboard;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMine(BlockBreakEvent event) {
        manager.handleBlockBreak(event.getPlayer(), event.getBlock().getLocation());

        // Blast mining
        ItemStack item = event.getPlayer().getInventory().getItemInMainHand();
        if (item.getType() == Material.AIR || !item.hasItemMeta() || item.lore() == null) return;
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
                        if (target.getType() != Material.BEDROCK) {
                            manager.handleBlockBreak(event.getPlayer(), target.getLocation());
                            target.breakNaturally(item);
                        }
                    }
                }
            }
        }
        scoreboard.updateBoard(event.getPlayer());
    }
}
