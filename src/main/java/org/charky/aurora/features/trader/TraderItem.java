package org.charky.aurora.features.trader;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
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
        BackpackData backpack = backpackManager.get(player.getUniqueId());
        if (!backpack.hasItems(player, costs)) {
            TextUtils.error(player, "Not enough items");
            return;
        }

        backpack.removeItems(player, costs);

        ItemStack purchase = this.item.getItem();
        purchase.editMeta(meta -> meta.getPersistentDataContainer().remove(DataKeys.MENU_KEY));

        PlayerInventory playerInv = player.getInventory();
        EquipmentSlot target = equipmentSlot(purchase.getType());
        ItemStack occupant = (target == null) ? null : playerInv.getItem(target);

        if (target != null && occupant.getType().isAir()) {
            playerInv.setItem(target, purchase);
        } else if (playerInv.firstEmpty() != -1) {
            playerInv.addItem(purchase);
        } else {
            player.getWorld().dropItemNaturally(player.getLocation(), purchase);
            TextUtils.warn(player, "Inventory full - dropped at your feet");
        }

        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);

        Inventory open = player.getOpenInventory().getTopInventory();
        if (open.getHolder() instanceof TraderMenu menu) {
            for (int i = 0; i < open.getSize(); i++) {
                TraderNode node = menu.getNode(i);
                if (node != null) {
                    open.setItem(i, node.getDisplayItem(player));
                }
            }
        }
    }

    private static EquipmentSlot equipmentSlot(Material material) {
        String name = material.name();
        if (name.endsWith("_HELMET"))     return EquipmentSlot.HEAD;
        if (name.endsWith("_CHESTPLATE")) return EquipmentSlot.CHEST;
        if (name.endsWith("_LEGGINGS"))   return EquipmentSlot.LEGS;
        if (name.endsWith("_BOOTS"))      return EquipmentSlot.FEET;
        if (material == Material.SHIELD)  return EquipmentSlot.OFF_HAND;
        return null;
    }
}
