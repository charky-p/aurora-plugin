package org.charky.aurora.common.loot;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class IndependentLootPool implements LootPool {
    private final Map<ItemDrop, Double> drops = new HashMap<>();

    public void add(ItemDrop drop, double chance) {
        this.drops.put(drop, chance);
    }

    @Override
    public List<MenuItem> roll() {
        return drops.entrySet()
                    .stream()
                    .filter(e -> ThreadLocalRandom.current().nextDouble(100) <= e.getValue())
                    .map(e -> e.getKey().rollAmt())
                    .filter(Objects::nonNull)
                    .toList();
    }
}
