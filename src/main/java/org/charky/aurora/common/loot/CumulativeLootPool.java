package org.charky.aurora.common.loot;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

// At most one of the drops, picked by weight, and only chance% of the time
public class CumulativeLootPool implements LootPool {
    private final Map<ItemDrop, Double> drops = new HashMap<>();
    private final double chance;
    private double totalWeight = 0;

    public CumulativeLootPool(double chance) {
        this.chance = chance;
    }

    public void add(ItemDrop drop, double weight) {
        drops.put(drop, weight);
        totalWeight += weight;
    }

    @Override
    public List<MenuItem> roll(int looting) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        if (random.nextDouble() * 100 > chance) return List.of();

        double rand = random.nextDouble() * totalWeight;
        double current = 0;

        for (Map.Entry<ItemDrop, Double> entry : drops.entrySet()) {
            current += entry.getValue();
            if (rand <= current) return List.of(entry.getKey().rollAmt(looting));
        }

        return List.of();
    }
}
