package org.charky.aurora.models;

import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

public class ItemDrop {
    private final ItemStack item;
    private final double chance;
    private final int minAmount;
    private final int maxAmount;

    public ItemDrop(ItemStack item, int minAmount, int maxAmount) {
        this(item, 100.0, minAmount, maxAmount);
    }

    public ItemDrop(ItemStack item, double chance, int minAmount, int maxAmount) {
        this.item = item;
        this.chance = chance;
        this.minAmount = minAmount;
        this.maxAmount = maxAmount;
    }

    public @Nullable MenuItem roll() {
        if (Math.random() * 100 > chance) return null;

        int amount = (int) (Math.random() * (maxAmount - minAmount + 1)) + minAmount;

        // Menu item to cater to amounts > 64
        return new MenuItem(item.clone(), amount);
    }
}
