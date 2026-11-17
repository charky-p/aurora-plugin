package org.charky.aurora.common.loot;

import java.util.concurrent.ThreadLocalRandom;

// One entry of a loot pool: an item and how many of it a roll gives
public class ItemDrop {
    private final MenuItem item;
    private final int minAmount;
    private final int maxAmount;
    private final boolean lootable;
    private final boolean lucky;

    // Lootable drops are multiplied by Looting, gear and keys are not.
    // Lucky drops, a mob's gear, come more often with Luck
    public ItemDrop(MenuItem item, int minAmount, int maxAmount, boolean lootable, boolean lucky) {
        this.lucky = lucky;
        this.item = item;
        this.minAmount = minAmount;
        this.maxAmount = maxAmount;
        this.lootable = lootable;
    }

    public boolean isLucky() {
        return lucky;
    }

    // A fresh copy every time, so callers can change its amount. Rare is whether the
    // pool it came from drops less than every time, which Looting multiplies less
    public MenuItem rollAmt(int looting, boolean rare) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        int amount = random.nextInt(minAmount, Math.max(minAmount, maxAmount) + 1);
        if (lootable) amount = Looting.apply(amount, looting, rare, random);

        // Menu item to cater to amounts > 64
        MenuItem drop = item.copy();
        drop.setAmount(amount);
        return drop;
    }
}
