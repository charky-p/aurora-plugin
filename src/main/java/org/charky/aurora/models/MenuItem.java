package org.charky.aurora.models;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

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
}
