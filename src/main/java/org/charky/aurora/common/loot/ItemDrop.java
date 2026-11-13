package org.charky.aurora.common.loot;

import java.util.concurrent.ThreadLocalRandom;

// One entry of a loot pool: an item and how many of it a roll gives
public class ItemDrop {
    private final MenuItem item;
    private final int minAmount;
    private final int maxAmount;
    private final boolean lootable;

    // Lootable drops get their range widened by Looting, gear and keys do not
    public ItemDrop(MenuItem item, int minAmount, int maxAmount, boolean lootable) {
        this.item = item;
        this.minAmount = minAmount;
        this.maxAmount = maxAmount;
        this.lootable = lootable;
    }

    // A fresh copy every time, so callers can change its amount
    public MenuItem rollAmt(int looting) {
        Looting.Range range = lootable
                ? Looting.apply(minAmount, maxAmount, looting)
                : new Looting.Range(minAmount, maxAmount);
        int amount = ThreadLocalRandom.current().nextInt(range.min(), Math.max(range.min(), range.max()) + 1);

        // Menu item to cater to amounts > 64
        MenuItem drop = item.copy();
        drop.setAmount(amount);
        return drop;
    }
}
