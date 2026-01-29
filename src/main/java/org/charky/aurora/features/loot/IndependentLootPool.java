package org.charky.aurora.features.loot;

import org.charky.aurora.interfaces.LootPool;

import java.util.*;

public class IndependentLootPool implements LootPool {
    private final List<ItemDrop> drops = new ArrayList<>();

    public IndependentLootPool(ItemDrop... drops) {
        this.drops.addAll(Arrays.asList(drops));
    }

    public void add(ItemDrop drop) {
        this.drops.add(drop);
    }

    @Override
    public List<MenuItem> roll() {
        return drops.stream()
                    .map(ItemDrop::roll)
                    .filter(Objects::nonNull)
                    .toList();
    }
}
