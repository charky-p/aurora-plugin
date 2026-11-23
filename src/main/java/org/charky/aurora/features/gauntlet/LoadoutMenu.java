package org.charky.aurora.features.gauntlet;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.charky.aurora.common.util.TextUtils;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;

// The Loadout: where the setup goes in a run, a duel and the PvP zone, in a chest laid
// out like the inventory: its three rows on top, the hotbar under them and the off hand
// below the hotbar's first slot. Armour is worn. Things are moved about one whole stack
// at a time and closing it saves the layout; Reset lays it all out afresh (GauntletSetup)
final class LoadoutMenu implements InventoryHolder {
    static final int SIZE = 45;
    static final int RESET_SLOT = 44;
    // The chest's rows: the inventory's rows (its slots 9-35), then the hotbar (0-8)
    private static final int HOTBAR_ROW = 27;
    private static final int OFF_HAND_SLOT = 36;
    private static final int OFF_HAND_LABEL = 37;
    private static final int INFO_SLOT = 40;

    private final Inventory inventory;

    // Laid out by inventory slot
    LoadoutMenu(Map<Integer, ItemStack> placed) {
        inventory = Bukkit.createInventory(this, SIZE, TextUtils.parse("<dark_gray>Loadout <gray>- close to save"));
        draw(placed);
    }

    void draw(Map<Integer, ItemStack> placed) {
        inventory.clear();
        placed.forEach((slot, item) -> {
            int at = chestSlot(slot);
            if (at >= 0) inventory.setItem(at, item);
        });
        ItemStack pane = icon(Material.GRAY_STAINED_GLASS_PANE, " ", List.of());
        for (int slot = OFF_HAND_SLOT + 1; slot < SIZE; slot++) inventory.setItem(slot, pane);
        inventory.setItem(OFF_HAND_LABEL, icon(Material.LIGHT_GRAY_STAINED_GLASS_PANE, "<gray>← Off hand", List.of()));
        inventory.setItem(INFO_SLOT, icon(Material.BOOK, "<gold><b>Your Loadout", List.of(
                "<gray>Where your gear and kit go in a",
                "<gray>run, a duel and the PvP zone:",
                "<gray>the top three rows are your",
                "<gray>inventory, the row under them",
                "<gray>your hotbar. Armour is worn.",
                "",
                "<yellow>Close to save")));
        inventory.setItem(RESET_SLOT, icon(Material.BARRIER, "<red><b>Reset", List.of("<gray>Lay it all out afresh")));
    }

    // The chest slot showing an inventory slot, -1 for armour (worn, not shown)
    static int chestSlot(int inventorySlot) {
        if (inventorySlot >= 9 && inventorySlot < GauntletSetup.STORAGE) return inventorySlot - 9;
        if (inventorySlot >= 0 && inventorySlot < 9) return HOTBAR_ROW + inventorySlot;
        return inventorySlot == GauntletSetup.OFF_HAND ? OFF_HAND_SLOT : -1;
    }

    // The inventory slot a chest slot shows, -1 for the chest's own
    static int inventorySlot(int chestSlot) {
        if (chestSlot >= 0 && chestSlot < HOTBAR_ROW) return chestSlot + 9;
        if (chestSlot >= HOTBAR_ROW && chestSlot < OFF_HAND_SLOT) return chestSlot - HOTBAR_ROW;
        return chestSlot == OFF_HAND_SLOT ? GauntletSetup.OFF_HAND : -1;
    }

    private static ItemStack icon(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        item.editMeta(meta -> {
            meta.displayName(TextUtils.parse("<!italic>" + name));
            meta.lore(lore.stream().map(line -> TextUtils.parse("<!italic>" + line)).toList());
            meta.addItemFlags(ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
        });
        return item;
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }
}
