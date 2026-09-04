package org.charky.aurora.features.player;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.charky.aurora.common.loot.MenuItem;
import org.charky.aurora.core.DataRegistry;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class BackpackData implements InventoryHolder {
    // TODO: Add player to be attached here
    private final Map<String, Long> backpack = new HashMap<>();
    private final DataRegistry registry;
    private Inventory inventory;

    public BackpackData(DataRegistry registry) {
        this.registry = registry;
    }

    public void addItem(String item, long amount) {
        backpack.put(item, backpack.getOrDefault(item, 0L) + amount);
    }

    public void removeItem(String item, long amount) {
        backpack.put(item, Math.max(backpack.getOrDefault(item, 0L) - amount, 0));
    }

    public void addItems(List<MenuItem> drops) {
        for (MenuItem item : drops) {
            addItem(item.getId(), item.getAmount());
        }
    }

    public void removeItems(Player player, List<MenuItem> costs) {
        for (MenuItem cost : costs) {
            long amountNeeded = cost.getAmount();
            long inBackpack = backpack.getOrDefault(cost.getId(), 0L);

            if (inBackpack > 0) {
                long takenFromBackpack = Math.min(inBackpack, amountNeeded);
                removeItem(cost.getId(), (int) takenFromBackpack);
                amountNeeded -= takenFromBackpack;
            }

            // Take remaining from inventory
            if (amountNeeded > 0) {
                removePhysicalItem(player, cost.getItem(), (int) amountNeeded);
            }
        }
    }

    public long getAmount(Player player, String item) {
        long backpackAmount = backpack.getOrDefault(item, 0L);
        long inventoryAmount = 0;

        MenuItem template = registry.getItem(item);

        if (template != null) {
            ItemStack target = template.getItem();
            for (ItemStack slot : player.getInventory().getStorageContents()) {
                if (slot != null && slot.isSimilar(target)) {
                    inventoryAmount += slot.getAmount();
                }
            }
        }

        return backpackAmount + inventoryAmount;
    }

    public boolean hasItems(Player player, List<MenuItem> costs) {
        for (MenuItem cost : costs) {
            if (backpack.getOrDefault(cost.getId(), 0L) >= cost.getAmount() ||
                hasPhysicalItem(player, cost)) continue;
            return false;
        }
        return true;
    }

    public boolean hasPhysicalItem(Player player, MenuItem cost) {
        int count = 0;
        for (ItemStack slot : player.getInventory().getStorageContents()) {
            if (slot != null && slot.isSimilar(cost.getItem())) count += slot.getAmount();
        }

        return count >= cost.getAmount();
    }

    private void removePhysicalItem(Player player, ItemStack itemToMatch, int amountToRemove) {
        ItemStack[] contents = player.getInventory().getStorageContents();
        int remaining = amountToRemove;

        for (int i = 0; i < contents.length; i++) {
            ItemStack slot = contents[i];

            // Check if the item matches requirement
            if (slot != null && slot.isSimilar(itemToMatch)) {
                if (slot.getAmount() <= remaining) {
                    remaining -= slot.getAmount();
                    contents[i] = null;
                } else {
                    slot.setAmount(slot.getAmount() - remaining);
                    remaining = 0;
                }
            }
            if (remaining <= 0) break;
        }

        player.getInventory().setStorageContents(contents);
    }

    public Map<String, Long> getItems() {
        return backpack;
    }

    public void openInventory(Player player) {
        this.inventory = Bukkit.createInventory(this, 54, "§8Your Backpack");

        // TODO: preserve order of resources yaml file
        for (MenuItem item : registry.getItems()) {
            ItemStack displayItem = item.getItem();
            displayItem.editMeta(meta -> {
                List<Component> lore = meta.hasLore() ? meta.lore() : new ArrayList<>();
                lore.add(Component.empty());
                lore.add(Component.text("Amount: " + backpack.getOrDefault(item.getId(), 0L)));
                meta.lore(lore);
            });

            this.inventory.addItem(displayItem);
        }

        player.openInventory(this.inventory);
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }

    public void clear() {
        backpack.clear();
    }
}
