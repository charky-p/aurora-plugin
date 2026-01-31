package org.charky.aurora.features.trader;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.charky.aurora.interfaces.TraderNode;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public class TraderMenu implements TraderNode, InventoryHolder {
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

    public void setContents(Map<Integer, TraderNode> contents) {
        this.contents = contents;
    }

    public TraderNode getNode(int slot) {
        return contents.get(slot);
    }

    @Override
    public ItemStack getDisplayItem() {
        if (displayItem == null) return new ItemStack(Material.AIR);
        ItemStack display = this.displayItem.clone();
        display.editMeta(meta -> {
            meta.getPersistentDataContainer().set(
                    TraderNode.MENU_KEY,
                    PersistentDataType.STRING,
                    menuId
            );
        });
        return display;
    }

    @Override
    public void onClick(Player player) {
        Inventory inv = Bukkit.createInventory(this, rows * 9, title);

        contents.forEach((slot, node) -> {
            inv.setItem(slot, node.getDisplayItem());
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
