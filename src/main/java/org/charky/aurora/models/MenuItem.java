package org.charky.aurora.models;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.charky.aurora.managers.TraderManager;

import java.util.ArrayList;
import java.util.List;

public class MenuItem implements MenuNode {
    private final TraderManager manager;
    private final String menuId;
    private final ItemStack item;
    private final List<ItemStack> costs;

    public MenuItem(TraderManager manager, String menuId, ItemStack item, List<ItemStack> costs) {
        this.menuId = menuId;
        this.manager = manager;
        this.item = item;
        this.costs = costs;
    }

    @Override
    public ItemStack getDisplayItem() {
        ItemStack display = this.item.clone();
        display.editMeta(meta -> {
            meta.getPersistentDataContainer().set(
                    manager.getMenuKey(),
                    PersistentDataType.STRING,
                    this.menuId
            );

            // Price lore
            List<Component> lore = new ArrayList<>();
            lore.add(Component.text("Cost:").color(NamedTextColor.GRAY));
            for (ItemStack cost : costs) {
                lore.add(Component.text("- " + cost.getAmount() + "x ")
                        .append(Component.translatable(cost.getType().translationKey()))
                        .color(NamedTextColor.YELLOW));
            }
            meta.lore(lore);
        });
        return display;
    }

    @Override
    public void onClick(Player player) {
        for (ItemStack cost : costs) {
            if (!this.hasItems(player, cost)) {
                player.sendMessage(Component.text("Missing materials or full inventory!")
                        .color(NamedTextColor.RED));
                return;
            }
        }

        for (ItemStack cost : costs) {
            this.removeItems(player, cost);
        }

        player.getInventory().addItem(item);
        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);
    }

    public boolean hasItems(Player player, ItemStack cost) {
        int count = 0;
        boolean freeSlot = false;
        for (ItemStack slot : player.getInventory().getStorageContents()) {
            if (slot != null && slot.isSimilar(cost)) count += slot.getAmount();
            if (slot == null || count <= cost.getAmount()) freeSlot = true;
        }

        return count >= cost.getAmount() && freeSlot;
    }

    private void removeItems(Player player, ItemStack cost) {
        int toRemove = cost.getAmount();
        ItemStack[] contents = player.getInventory().getStorageContents();
        for (int i = 0; i < contents.length; i++) {
            ItemStack slot = contents[i];
            if (slot != null && slot.isSimilar(cost)) {
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
