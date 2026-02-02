package org.charky.aurora.common.loot;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class InventoryManager {
    public void addToInventory(Player player, List<MenuItem> drops) {
        // TODO: Full inventory handling, message player
        if (player == null || drops == null || drops.isEmpty()) return;

        List<ItemStack> stacks = new ArrayList<>();
        for (MenuItem menuItem : drops) {
            stacks.addAll(menuItem.convertToStacks());
        }

        boolean full = false;
        for (ItemStack item : stacks) {
            if (item == null || item.getType() == Material.AIR) continue;
            if (!player.getInventory().addItem(item).isEmpty()) full = true;
        }

        if (full) sendFullInventoryAlert(player);
    }

    private void sendFullInventoryAlert(Player player) {
        player.sendActionBar(Component.text("INVENTORY FULL", NamedTextColor.RED, TextDecoration.BOLD));
        player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_ALLAY_HURT, 1.0f, 1.0f);
    }
}
