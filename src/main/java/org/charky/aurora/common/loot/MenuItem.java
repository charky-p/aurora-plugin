package org.charky.aurora.common.loot;

import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class MenuItem {
    // Wrapper for ItemStack to be used for TraderItem, allows for amount > 64
    private final ItemStack item;
    private int amount;
    private String id;

    public MenuItem(String id, ItemStack item) {
        this(id, item, 1);
    }

    public MenuItem(String id, ItemStack item, int amount) {
        this.item = item;
        this.amount = amount;
        this.id = id;
    }

    public int getAmount() {
        return this.amount;
    }

    public ItemStack getItem() {
        return this.item.clone();
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

    public String getId() {
        return id;
    }

    public MenuItem clone() {
        return new MenuItem(id, item.clone(), amount);
    }
}
