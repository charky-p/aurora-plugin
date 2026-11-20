package org.charky.aurora.features.trader;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.charky.aurora.common.loot.MenuItem;
import org.charky.aurora.common.util.TextUtils;
import org.charky.aurora.features.keys.KeyItems;
import org.charky.aurora.features.player.BackpackData;
import org.charky.aurora.features.player.BackpackManager;

import java.util.ArrayList;
import java.util.List;

// Something a shop sells for resources from the backpack: a key, food, the elytra.
// Armour and a shield go straight on when the slot is free, the rest to the inventory,
// or to the ground when that is full
public class TraderItem implements TraderNode {
    private static final int MAX_STACK = 64;

    private final MenuItem item;
    private final List<MenuItem> costs;
    private final BackpackManager backpackManager;

    public TraderItem(BackpackManager backpackManager, MenuItem item, List<MenuItem> costs) {
        this.item = item;
        this.costs = costs;
        this.backpackManager = backpackManager;
        if (item.getAmount() > MAX_STACK) item.setAmount(MAX_STACK);
    }

    // The item with what it costs and how much of each the player holds
    @Override
    public ItemStack getDisplayItem(Player player) {
        ItemStack display = item.getItem();
        BackpackData backpack = backpackManager.get(player.getUniqueId());
        display.editMeta(meta -> {
            List<Component> lore = meta.hasLore() ? new ArrayList<>(meta.lore()) : new ArrayList<>();
            lore.add(Component.empty());
            lore.add(TextUtils.parse("<!italic><gold>Purchase Requirements"));
            for (MenuItem cost : costs) {
                long have = backpack == null ? 0 : backpack.getAmount(player, cost.getId());
                lore.add(TextUtils.parse("<!italic><gray>» ").append(cost.getItem().effectiveName())
                        .append(TextUtils.parse((have >= cost.getAmount() ? " <green>" : " <red>")
                                + "(" + have + "/" + cost.getAmount() + ")")));
            }
            meta.lore(lore);
        });
        return display;
    }

    @Override
    public void onClick(Player player) {
        BackpackData backpack = backpackManager.get(player.getUniqueId());
        if (backpack == null) {
            TextUtils.error(player, "Not ready yet, try again in a moment");
            return;
        }
        if (!backpack.hasItems(player, costs)) {
            TextUtils.error(player, "Not enough items");
            return;
        }

        backpack.removeItems(player, costs);
        KeyItems.obtained(player, item.getId());
        hand(player, item.getItem());
        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);
        refresh(player);
    }

    private static void hand(Player player, ItemStack purchase) {
        PlayerInventory inventory = player.getInventory();
        EquipmentSlot slot = equipmentSlot(purchase.getType());
        if (slot != null && inventory.getItem(slot).getType().isAir()) {
            inventory.setItem(slot, purchase);
        } else if (inventory.firstEmpty() != -1) {
            inventory.addItem(purchase);
        } else {
            player.getWorld().dropItemNaturally(player.getLocation(), purchase);
            TextUtils.warn(player, "Inventory full - dropped at your feet");
        }
    }

    // The shop still open shows the new amounts the player holds
    private static void refresh(Player player) {
        Inventory open = player.getOpenInventory().getTopInventory();
        if (!(open.getHolder() instanceof TraderMenu menu)) return;
        for (int slot = 0; slot < open.getSize(); slot++) {
            TraderNode node = menu.getNode(slot);
            if (node != null) open.setItem(slot, node.getDisplayItem(player));
        }
    }

    private static EquipmentSlot equipmentSlot(Material material) {
        String name = material.name();
        if (name.endsWith("_HELMET")) return EquipmentSlot.HEAD;
        if (name.endsWith("_CHESTPLATE")) return EquipmentSlot.CHEST;
        if (name.endsWith("_LEGGINGS")) return EquipmentSlot.LEGS;
        if (name.endsWith("_BOOTS")) return EquipmentSlot.FEET;
        if (material == Material.SHIELD) return EquipmentSlot.OFF_HAND;
        return null;
    }
}
