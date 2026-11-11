package org.charky.aurora.common.loot;

import org.jetbrains.annotations.Nullable;

public class ItemDrop {
    private final MenuItem item;
    private final int minAmount;
    private final int maxAmount;
    private final boolean lootable;

    public ItemDrop(MenuItem item, int minAmount, int maxAmount) {
        this(item, minAmount, maxAmount, false);
    }

    // Lootable drops get their range widened by Looting, gear and keys do not
    public ItemDrop(MenuItem item, int minAmount, int maxAmount, boolean lootable) {
        this.item = item;
        this.minAmount = minAmount;
        this.maxAmount = maxAmount;
        this.lootable = lootable;
    }

    public @Nullable MenuItem rollAmt() {
        return rollAmt(0);
    }

    public @Nullable MenuItem rollAmt(int looting) {
        Looting.Range range = lootable
                ? Looting.apply(minAmount, maxAmount, looting)
                : new Looting.Range(minAmount, maxAmount);
        int amount = (int) (Math.random() * (range.max() - range.min() + 1)) + range.min();

        // Menu item to cater to amounts > 64
        MenuItem drop = item.copy();
        drop.setAmount(amount);
        return drop;
    }
}
