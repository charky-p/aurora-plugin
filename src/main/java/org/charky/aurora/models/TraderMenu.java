package org.charky.aurora.models;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.Map;

public class TraderMenu implements TraderNode {
    private String menuId;
    private Component title;
    private int rows;
    private Map<Integer, TraderNode> contents;
    private ItemStack displayItem;

    public TraderMenu(String menuId, Component title, int rows, Map<Integer, TraderNode> contents, ItemStack displayItem) {
        this.menuId = menuId;
        this.title = title;
        this.rows = rows;
        this.contents = contents;
        this.displayItem = displayItem;
    }

    @Override
    public ItemStack getDisplayItem() {
        return this.displayItem;
    }

    @Override
    public void onClick(Player player) {
        Inventory inv = Bukkit.createInventory(null, rows * 9, title);

        contents.forEach((slot, node) -> {
            inv.setItem(slot, node.getDisplayItem());
        });

        player.openInventory(inv);
    }

    public String getId() {
        return menuId;
    }
}
