package org.charky.aurora.features.trader;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;

public class TraderMenu implements TraderNode, InventoryHolder {
    private final String menuId;
    private final Component title;
    private final int rows;
    private final ItemStack displayItem;
    // Filled after every menu exists, so menus can link to each other
    private Map<Integer, TraderNode> contents;

    public TraderMenu(String menuId, Component title, int rows, Map<Integer, TraderNode> contents, ItemStack displayItem) {
        this.menuId = menuId;
        this.title = title;
        this.rows = rows;
        this.contents = contents;
        this.displayItem = displayItem;
    }

    public void setContents(Map<Integer, TraderNode> contents) {
        this.contents = contents;
    }

    public TraderNode getNode(int slot) {
        return contents.get(slot);
    }

    // Everything it holds, in slot order
    public List<TraderNode> nodes() {
        return contents.entrySet().stream().sorted(Map.Entry.comparingByKey()).map(Map.Entry::getValue).toList();
    }

    @Override
    public ItemStack getDisplayItem(Player player) {
        if (displayItem == null) return new ItemStack(Material.AIR);
        return displayItem.clone();
    }

    @Override
    public void onClick(Player player) {
        Inventory inv = Bukkit.createInventory(this, rows * 9, title);

        contents.forEach((slot, node) -> {
            inv.setItem(slot, node.getDisplayItem(player));
        });

        player.openInventory(inv);
    }

    @Override
    public @NotNull Inventory getInventory() {
        return null;
    }

    public String getId() {
        return menuId;
    }
}
