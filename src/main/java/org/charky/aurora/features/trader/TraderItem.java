package org.charky.aurora.features.trader;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.charky.aurora.Aurora;
import org.charky.aurora.features.loot.MenuItem;
import org.charky.aurora.interfaces.TraderNode;
import org.charky.aurora.managers.TraderManager;

import java.util.ArrayList;
import java.util.List;

public class TraderItem implements TraderNode {
    public static final String MENU_KEY_NAME = "menu_id";
    public static final NamespacedKey MENU_KEY = new NamespacedKey(JavaPlugin.getPlugin(Aurora.class), MENU_KEY_NAME);
    private final String menuId;
    private final MenuItem item;
    private final List<MenuItem> costs;

    public TraderItem(String menuId, MenuItem item, List<MenuItem> costs) {
        this.menuId = menuId;
        this.item = item;
        this.costs = costs;

        if (item.getAmount() > 64) {
            item.setAmount(64);
        }
    }

    @Override
    public ItemStack getDisplayItem() {
        ItemStack display = item.getItem();
        display.editMeta(meta -> {
            meta.getPersistentDataContainer().set(
                    MENU_KEY,
                    PersistentDataType.STRING,
                    this.menuId
            );

            // Price lore
            List<Component> lore = new ArrayList<>();
            lore.add(Component.text("Cost:").color(NamedTextColor.GRAY));
            for (MenuItem cost : costs) {
                lore.add(Component.text("- " + cost.getAmount() + "x ").append(cost.getItem().displayName()));
            }
            meta.lore(lore);
        });
        return display;
    }

    @Override
    public void onClick(Player player) {
        for (MenuItem cost : costs) {
            if (!this.hasItems(player, cost)) {
                player.sendMessage(Component.text("Missing materials or full inventory!")
                        .color(NamedTextColor.RED));
                return;
            }
        }

        for (MenuItem cost : costs) {
            this.removeItems(player, cost);
        }

        ItemStack purchase = this.item.getItem().clone();
        purchase.editMeta(meta -> {
            meta.getPersistentDataContainer().remove(MENU_KEY);
        });
        player.getInventory().addItem(purchase);
        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);
    }

    public boolean hasItems(Player player, MenuItem cost) {
        int count = 0;
        boolean freeSlot = false;
        for (ItemStack slot : player.getInventory().getStorageContents()) {
            if (slot != null && slot.isSimilar(cost.getItem())) count += slot.getAmount();
            if (slot == null || count <= cost.getAmount()) freeSlot = true;
        }

        return count >= cost.getAmount() && freeSlot;
    }

    private void removeItems(Player player, MenuItem cost) {
        int toRemove = cost.getAmount();
        ItemStack[] contents = player.getInventory().getStorageContents();
        for (int i = 0; i < contents.length; i++) {
            ItemStack slot = contents[i];
            if (slot != null && slot.isSimilar(cost.getItem())) {
                if (slot.getAmount() <= toRemove) {
                    toRemove -= slot.getAmount();
                    contents[i] = null;
                } else {
                    slot.setAmount(slot.getAmount() - toRemove);
                    toRemove = 0;
                }
            }

            if (toRemove <= 0) break;
        }
        player.getInventory().setStorageContents(contents);
    }
}
