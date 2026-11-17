package org.charky.aurora.features.player;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.charky.aurora.core.DataRegistry;

// What a player is handed besides their tools when they start: a stack of bread
public final class StarterKit {
    private static final String FOOD = "bread";
    private static final int FOOD_AMOUNT = 64;

    private StarterKit() {
    }

    public static void give(Player player, DataRegistry registry) {
        ItemStack food = registry.getAllItems().get(FOOD);
        if (food == null) return;

        ItemStack stack = food.clone();
        stack.setAmount(FOOD_AMOUNT);
        player.getInventory().addItem(stack);
    }
}
