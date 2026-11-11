package org.charky.aurora.common.loot;

import java.util.List;

public interface LootPool {
    // Looting widens the amount of lootable drops, blocks roll with 0
    List<MenuItem> roll(int looting);

    default List<MenuItem> roll() {
        return roll(0);
    }
}
