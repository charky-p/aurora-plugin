package org.charky.aurora.features.player;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.charky.aurora.common.loot.MenuItem;
import org.charky.aurora.common.util.DataKeys;
import org.charky.aurora.core.DataRegistry;

import java.util.*;

public class BackpackData {
    // TODO: Add player to be attached here
    // Linked, so resources keep the order they were first collected in
    private final Map<String, Long> backpack = new LinkedHashMap<>();
    private final DataRegistry registry;

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
                removePhysicalItem(player, cost.getId(), (int) amountNeeded);
            }
        }
    }

    public long getAmount(Player player, String item) {
        long backpackAmount = backpack.getOrDefault(item, 0L);
        long inventoryAmount = 0;

        for (ItemStack slot : player.getInventory().getContents()) {
            if (matches(slot, item)) inventoryAmount += slot.getAmount();
        }

        return backpackAmount + inventoryAmount;
    }

    public boolean hasItems(Player player, List<MenuItem> costs) {
        for (MenuItem cost : costs) {
            if (getAmount(player, cost.getId()) < cost.getAmount()) return false;
        }
        return true;
    }

    public boolean hasPhysicalItem(Player player, MenuItem cost) {
        int count = 0;
        for (ItemStack slot : player.getInventory().getContents()) {
            if (matches(slot, cost.getId())) count += slot.getAmount();
        }

        return count >= cost.getAmount();
    }

    private void removePhysicalItem(Player player, String id, int amountToRemove) {
        ItemStack[] contents = player.getInventory().getContents();
        int remaining = amountToRemove;

        for (int i = 0; i < contents.length; i++) {
            ItemStack slot = contents[i];

            // Check if the item matches requirement
            if (matches(slot, id)) {
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

        player.getInventory().setContents(contents);
    }

    // Matched by registry id, so an upgrade still takes the previous tier after its
    // lore or stats change. Items given out before ids existed fall back to looking
    // the same as the current template
    private boolean matches(ItemStack slot, String id) {
        if (slot == null || slot.getType().isAir()) return false;

        String slotId = DataRegistry.itemId(slot);
        if (slotId != null) return slotId.equals(id);

        ItemStack template = registry.getAllItems().get(id);
        if (template == null) return false;

        ItemStack legacy = template.clone();
        legacy.editMeta(meta -> meta.getPersistentDataContainer().remove(DataKeys.ITEM_KEY));
        return slot.isSimilar(legacy);
    }

    public Map<String, Long> getItems() {
        return backpack;
    }

    public void clear() {
        backpack.clear();
    }
}
