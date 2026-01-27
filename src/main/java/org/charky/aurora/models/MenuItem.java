package org.charky.aurora.models;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class MenuItem {
    // Wrapper for ItemStack to be used for TraderItem, allows for amount > 64
    private final ItemStack item;
    private int amount;

    public MenuItem(ItemStack item) {
        this(item, 1);
    }

    public MenuItem(ItemStack item, int amount) {
        this.item = item;
        this.amount = amount;
    }

    public int getAmount() {
        return this.amount;
    }

    public ItemStack getItem() {
        return this.item;
    }

    public void setAmount(int amount) {
        this.amount = amount;
    }

    public List<ItemStack> convertToStacks() {
        List<ItemStack> stacks = new ArrayList<>();
        int remaining = amount;
        int maxStack = item.getMaxStackSize();

        while (remaining > 0) {
            int currentStackSize = Math.min(remaining, maxStack);
            ItemStack stack = item.clone();
            stack.setAmount(currentStackSize);
            stacks.add(stack);
            remaining -= currentStackSize;
        }
        return stacks;
    }
}
