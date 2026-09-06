package org.charky.aurora.features.trader;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.function.Consumer;

public class TraderAction implements TraderNode {
    private final ItemStack icon;
    private final Consumer<Player> action;

    public TraderAction(ItemStack icon, Consumer<Player> action) {
        this.icon = icon;
        this.action = action;
    }

    @Override
    public ItemStack getDisplayItem(Player player) {
        return icon == null ? new ItemStack(Material.AIR) : icon.clone();
    }

    @Override
    public void onClick(Player player) {
        if (action != null) action.accept(player);
    }
}