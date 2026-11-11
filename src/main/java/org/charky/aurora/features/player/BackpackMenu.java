package org.charky.aurora.features.player;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.charky.aurora.common.util.TextUtils;
import org.charky.aurora.core.DataRegistry;
import org.charky.aurora.features.trader.TraderMenu;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

// View only, the backpack holds resources so they don't clutter the inventory.
// We may later add moving items between the backpack and the inventory
public class BackpackMenu implements InventoryHolder {
    private static final int SIZE = 54;
    private static final String HUB_ID = "aurora_hub";

    // Bottom row controls
    private static final int BACK_SLOT = 45;
    private static final int PREVIOUS_SLOT = 48;
    private static final int INFO_SLOT = 49;
    private static final int NEXT_SLOT = 50;
    private static final int CLOSE_SLOT = 53;

    // Everything inside the glass border, 4 rows of 7
    private static final int[] ITEM_SLOTS = innerSlots();

    private final BackpackData backpack;
    private final DataRegistry registry;
    private final Inventory inventory;
    private int page = 0;

    public BackpackMenu(BackpackData backpack, DataRegistry registry) {
        this.backpack = backpack;
        this.registry = registry;
        this.inventory = Bukkit.createInventory(this, SIZE, TextUtils.parse("<dark_gray>Backpack"));
    }

    public void open(Player player) {
        render(player);
        player.openInventory(inventory);
    }

    public void handleClick(Player player, int slot) {
        switch (slot) {
            case PREVIOUS_SLOT -> turnPage(player, page - 1);
            case NEXT_SLOT -> turnPage(player, page + 1);
            case CLOSE_SLOT -> player.closeInventory();
            case BACK_SLOT -> {
                TraderMenu hub = registry.getGui(HUB_ID);
                if (hub != null) hub.onClick(player);
            }
            default -> {}
        }
    }

    private void turnPage(Player player, int target) {
        if (target < 0 || target >= pageCount()) return;

        page = target;
        render(player);
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1f);
    }

    // Redrawn in place so turning a page does not reopen the window
    private void render(Player player) {
        inventory.clear();

        ItemStack glass = button(Material.GRAY_STAINED_GLASS_PANE, null);
        for (int slot = 0; slot < SIZE; slot++) {
            int row = slot / 9, col = slot % 9;
            if (row == 0 || row == 5 || col == 0 || col == 8) inventory.setItem(slot, glass);
        }

        List<ItemStack> items = displayItems();
        int start = page * ITEM_SLOTS.length;
        for (int i = 0; i < ITEM_SLOTS.length && start + i < items.size(); i++) {
            inventory.setItem(ITEM_SLOTS[i], items.get(start + i));
        }

        int pages = pageCount();
        if (page > 0) inventory.setItem(PREVIOUS_SLOT, button(Material.ARROW, "<yellow>Previous Page"));
        if (page < pages - 1) inventory.setItem(NEXT_SLOT, button(Material.ARROW, "<yellow>Next Page"));

        TraderMenu hub = registry.getGui(HUB_ID);
        if (hub != null) inventory.setItem(BACK_SLOT, hub.getDisplayItem(player));

        ItemStack info = button(Material.CHEST, "<gold><b>Backpack");
        info.editMeta(meta -> meta.lore(List.of(
                TextUtils.parse("<!italic><gray>Resources: <white>" + items.size()),
                TextUtils.parse("<!italic><gray>Page: <white>" + (page + 1) + "/" + pages))));
        inventory.setItem(INFO_SLOT, info);

        inventory.setItem(CLOSE_SLOT, button(Material.BARRIER, "<red>Close"));
    }

    // Resources in the order they were first collected, each showing how many are held
    private List<ItemStack> displayItems() {
        List<ItemStack> items = new ArrayList<>();
        for (Map.Entry<String, Long> entry : backpack.getItems().entrySet()) {
            ItemStack template = registry.getAllItems().get(entry.getKey());
            if (template == null || entry.getValue() <= 0) continue;

            ItemStack display = template.clone();
            display.editMeta(meta -> {
                List<Component> lore = meta.hasLore() ? new ArrayList<>(meta.lore()) : new ArrayList<>();
                lore.add(Component.empty());
                lore.add(TextUtils.parse("<!italic><gray>Amount: <white>" + entry.getValue()));
                meta.lore(lore);
            });
            items.add(display);
        }
        return items;
    }

    private int pageCount() {
        int count = (int) backpack.getItems().entrySet().stream()
                .filter(entry -> entry.getValue() > 0 && registry.getAllItems().containsKey(entry.getKey()))
                .count();
        return Math.max(1, (count + ITEM_SLOTS.length - 1) / ITEM_SLOTS.length);
    }

    // Null name hides the tooltip, for the glass border
    private static ItemStack button(Material material, String name) {
        ItemStack item = new ItemStack(material);
        item.editMeta(meta -> {
            if (name == null) meta.setHideTooltip(true);
            else meta.displayName(TextUtils.parse("<!italic>" + name));
        });
        return item;
    }

    private static int[] innerSlots() {
        int[] slots = new int[28];
        int i = 0;
        for (int row = 1; row <= 4; row++) {
            for (int col = 1; col <= 7; col++) slots[i++] = row * 9 + col;
        }
        return slots;
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }
}
