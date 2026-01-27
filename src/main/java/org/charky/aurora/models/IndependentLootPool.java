package org.charky.aurora.models;

import org.bukkit.inventory.ItemStack;

import java.util.*;

public class IndependentLootPool implements LootPool {
    private final List<ItemDrop> drops = new ArrayList<>();

    public void add(ItemDrop drop) {
        drops.add(drop);
    }

    @Override
    public List<MenuItem> roll() {
        return drops.stream()
                    .map(ItemDrop::roll)
                    .filter(Objects::nonNull)
                    .toList();
    }
}
