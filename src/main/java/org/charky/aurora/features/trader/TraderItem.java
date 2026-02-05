package org.charky.aurora.features.trader;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.charky.aurora.common.loot.MenuItem;
import org.charky.aurora.common.util.DataKeys;
import org.charky.aurora.common.util.TextUtils;
import org.charky.aurora.features.player.BackpackData;
import org.charky.aurora.features.player.BackpackManager;

import java.util.ArrayList;
import java.util.List;

public class TraderItem implements TraderNode {
    private final String menuId;
    private final MenuItem item;
    private final List<MenuItem> costs;
    private final BackpackManager backpackManager;

    public TraderItem(BackpackManager backpackManager, String menuId, MenuItem item, List<MenuItem> costs) {
        this.menuId = menuId;
        this.item = item;
        this.costs = costs;
        this.backpackManager = backpackManager;

        if (item.getAmount() > 64) {
            item.setAmount(64);
        }
    }

    @Override
    public ItemStack getDisplayItem(Player player) {
        ItemStack display = item.getItem();
        display.editMeta(meta -> {
            meta.getPersistentDataContainer().set(
                    DataKeys.MENU_KEY,
                    PersistentDataType.STRING,
                    this.menuId
            );

            // Price lore
            List<Component> lore = new ArrayList<>();
            if (display.lore() != null) lore.addAll(display.lore());
            lore.add(Component.empty());
            lore.add(Component.text("Purchase Requirements").color(NamedTextColor.GOLD).decoration(TextDecoration.ITALIC, false));

            BackpackData backpack = backpackManager.get(player.getUniqueId());
            for (MenuItem cost : costs) {
                long current = (backpack != null) ? backpack.getAmount(player, cost.getId()) : 0;
                int required = cost.getAmount();

                NamedTextColor statusColor = (current >= required) ? NamedTextColor.GREEN : NamedTextColor.RED;

                Component bullet = Component.text("» ").color(NamedTextColor.GRAY);
                Component itemName = cost.getItem().effectiveName().colorIfAbsent(NamedTextColor.WHITE);
                Component fraction = Component.text(" (" + current + "/" + required + ")")
                        .color(statusColor);
                lore.add(bullet.append(itemName).append(fraction).decoration(TextDecoration.ITALIC, false));
            }
            meta.lore(lore);
        });
        return display;
    }

    @Override
    public void onClick(Player player) {
        // Legacy inventory system
//        for (MenuItem cost : costs) {
//            if (!this.hasItems(player, cost)) {
//                player.sendMessage(Component.text("Missing materials or full inventory!")
//                        .color(NamedTextColor.RED));
//                return;
//            }
//        }
//
//        for (MenuItem cost : costs) {
//            this.removeItems(player, cost);
//        }

        // New backpack system
        if (player.getInventory().firstEmpty() == -1) {
            TextUtils.error(player, "Full inventory");
            return;
        }

        BackpackData backpack = backpackManager.get(player.getUniqueId());
        if (!backpack.hasItems(player, costs)) {
            TextUtils.error(player, "Not enough items");
            return;
        }

        backpack.removeItems(player, costs);

        ItemStack purchase = this.item.getItem();
        purchase.editMeta(meta -> {
            meta.getPersistentDataContainer().remove(DataKeys.MENU_KEY);
        });
        player.getInventory().addItem(purchase);
        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);

        Inventory inventory = player.getOpenInventory().getTopInventory();
        if (inventory.getHolder() instanceof TraderMenu menu) {
            for (int i = 0; i < inventory.getSize(); i++) {
                TraderNode node = menu.getNode(i);
                if (node != null) {
                    inventory.setItem(i, node.getDisplayItem(player));
                }
            }
        }
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
