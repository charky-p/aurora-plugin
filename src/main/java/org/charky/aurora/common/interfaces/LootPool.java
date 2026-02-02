package org.charky.aurora.common.interfaces;

import org.charky.aurora.features.spawner.loot.MenuItem;

import java.util.List;

public interface LootPool {
    List<MenuItem> roll();
}
