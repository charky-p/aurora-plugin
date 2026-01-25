package org.charky.aurora.listeners;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.charky.aurora.MineManager;

public class BlastListener implements Listener {
    private final MineManager manager;

    public BlastListener(MineManager manager) {
        this.manager = manager;
    }

    @EventHandler
    public void onMine(BlockBreakEvent event) {
        ItemStack item = event.getPlayer().getInventory().getItemInMainHand();

        // 1. Safety check: does the item exist and have lore?
        if (item.getType() == Material.AIR || !item.hasItemMeta() || item.lore() == null) return;

        // 2. Check if "Blast" is in the lore (using the modern Paper/Adventure way)
        boolean isBlast = item.lore().stream()
                .anyMatch(line -> PlainTextComponentSerializer.plainText()
                        .serialize(line).contains("Blast"));

        if (isBlast) {
            // Your 3x3x3 loop logic goes here!
            Block center = event.getBlock();
            for (int x = -1; x <= 1; x++) {
                for (int y = -1; y <= 1; y++) {
                    for (int z = -1; z <= 1; z++) {
                        Block target = center.getRelative(x, y, z);
                        if (target.getType() != Material.BEDROCK) {
                            manager.handleBlockBreak(target.getLocation());
                            target.breakNaturally(item);
                        }
                    }
                }
            }
        }
    }
}