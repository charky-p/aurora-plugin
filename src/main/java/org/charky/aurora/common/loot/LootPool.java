package org.charky.aurora.common.loot;

import java.util.List;

public interface LootPool {
    // Looting widens the amount of lootable drops, Luck makes gear drop more often.
    // Blocks roll with neither
    List<MenuItem> roll(int looting, int luck);

    default List<MenuItem> roll() {
        return roll(0, 0);
    }
}
