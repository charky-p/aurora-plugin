package org.charky.aurora.common.loot;

import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CumulativeLootPool implements LootPool {
    // Cumulative loot pool means only 1 of the drops occurs with a given weight
    private final Map<ItemDrop, Double> drops = new HashMap<>();
    private double totalWeight = 0;
    private final double chance;

    public CumulativeLootPool(double chance) {
        this.chance = chance;
    }

    public void add(ItemDrop drop, double weight) {
        drops.put(drop, weight);
        totalWeight += weight;
    }

    @Override
    public @Nullable List<MenuItem> roll(int looting) {
        if (Math.random() * 100 > chance) {
            return Collections.emptyList();
        }

        double rand = Math.random() * totalWeight;
        double current = 0;

        for (Map.Entry<ItemDrop, Double> entry : drops.entrySet()) {
            current += entry.getValue();
            if (rand <= current) {
                MenuItem rolled = entry.getKey().rollAmt(looting);
                if (rolled != null) return List.of(rolled);
            }
        }

        return List.of();
    }
}
