package org.charky.aurora.common.loot;

import java.util.List;

public interface LootPool {
    // Looting widens the amount of lootable drops, Luck makes gear drop more often.
    // Blocks roll with neither
    List<MenuItem> roll(int looting, int luck);

    default List<MenuItem> roll() {
        return roll(0, 0);
    }

    // Drops that come less than every time. Fortune multiplies these far less
    default boolean isRare() {
        return false;
    }
}
