package org.charky.aurora.common.loot;

import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

public class ItemDrop {
    private final ItemStack item;
    private final int minAmount;
    private final int maxAmount;

    public ItemDrop(ItemStack item, int minAmount, int maxAmount) {
        this.item = item;
        this.minAmount = minAmount;
        this.maxAmount = maxAmount;
    }

    public @Nullable MenuItem rollAmt() {
        int amount = (int) (Math.random() * (maxAmount - minAmount + 1)) + minAmount;

        // Menu item to cater to amounts > 64
        return new MenuItem(item.clone(), amount);
    }
}
