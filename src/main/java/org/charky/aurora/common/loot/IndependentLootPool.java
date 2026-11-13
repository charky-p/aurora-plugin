package org.charky.aurora.common.loot;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

// Every drop rolls its own chance, so one roll can give several of them or none
public class IndependentLootPool implements LootPool {
    private final Map<ItemDrop, Double> drops = new HashMap<>();

    public void add(ItemDrop drop, double chance) {
        this.drops.put(drop, chance);
    }

    @Override
    public List<MenuItem> roll(int looting) {
        return drops.entrySet()
                    .stream()
                    .filter(e -> ThreadLocalRandom.current().nextDouble(100) <= e.getValue())
                    .map(e -> e.getKey().rollAmt(looting))
                    .toList();
    }
}
