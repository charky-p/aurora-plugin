package org.charky.aurora.core.data;

import org.bukkit.inventory.ItemStack;
import org.charky.aurora.common.loot.CumulativeLootPool;
import org.charky.aurora.common.loot.IndependentLootPool;
import org.charky.aurora.common.loot.ItemDrop;
import org.charky.aurora.common.loot.LootPool;
import org.charky.aurora.common.loot.MenuItem;
import org.charky.aurora.core.models.BlockModel;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.logging.Logger;

// Turns the drop lists of blocks and mobs into loot pools. Drops of unknown items are
// skipped and logged, so a typo in the data shows up instead of a drop going missing
public class LootPoolBuilder {
    private final Logger logger;
    private final Map<String, ItemStack> items;
    private final Predicate<String> isResource;

    public LootPoolBuilder(Logger logger, Map<String, ItemStack> items, Predicate<String> isResource) {
        this.logger = logger;
        this.items = items;
        this.isResource = isResource;
    }

    // With lootable on, resources can be widened by Looting. Owner is only for the log
    public List<LootPool> build(String owner, List<BlockModel.IndependentPoolModel> independent,
                                BlockModel.CumulativeLootPoolModel choices, boolean lootable) {
        List<LootPool> pools = new ArrayList<>();

        if (independent != null && !independent.isEmpty()) {
            IndependentLootPool pool = new IndependentLootPool();
            for (BlockModel.IndependentPoolModel drop : independent) {
                if (unknown(owner, drop.id)) continue;
                pool.add(itemDrop(drop.id, drop.min, drop.max, lootable), drop.chance);
            }
            pools.add(pool);
        }

        if (choices != null) {
            CumulativeLootPool pool = new CumulativeLootPool(choices.chance);
            for (BlockModel.ChoiceModel choice : choices.options) {
                if (unknown(owner, choice.id)) continue;
                pool.add(itemDrop(choice.id, choice.min, choice.max, lootable), choice.weight);
            }
            pools.add(pool);
        }

        return pools;
    }

    private boolean unknown(String owner, String id) {
        if (items.containsKey(id)) return false;
        logger.warning("'" + owner + "' drops unknown item '" + id + "', skipped");
        return true;
    }

    private ItemDrop itemDrop(String id, int min, int max, boolean lootable) {
        return new ItemDrop(new MenuItem(id, items.get(id)), min, max, lootable && isResource.test(id));
    }
}
