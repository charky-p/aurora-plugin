package org.charky.aurora.common.menu;

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

// A 6 row chest with a glass border, 28 entries a page and a row of controls.
// View only, PagedMenuListener cancels every click and passes it here
public abstract class PagedMenu implements InventoryHolder {
    private static final int SIZE = 54;
    private static final String HUB_ID = "aurora_hub";

    // Bottom row controls, slots 46 and 52 are left free for subclass buttons
    private static final int BACK_SLOT = 45;
    private static final int PREVIOUS_SLOT = 48;
    private static final int INFO_SLOT = 49;
    private static final int NEXT_SLOT = 50;
    private static final int CLOSE_SLOT = 53;

    // Everything inside the glass border, 4 rows of 7
    private static final int[] ENTRY_SLOTS = innerSlots();

    protected final DataRegistry registry;
    private final Inventory inventory;
    private final Component name;
    private int page = 0;

    protected PagedMenu(DataRegistry registry, String name) {
        this.registry = registry;
        this.name = TextUtils.parse("<!italic>" + name);
        this.inventory = Bukkit.createInventory(this, SIZE, TextUtils.parse("<dark_gray>" + name));
    }

    // What fills the pages, built fresh on every draw
    protected abstract List<ItemStack> entries(Player player);

    // Lines on the info item, above the page number
    protected abstract List<Component> info(Player player, List<ItemStack> entries);

    // Index into entries, across all pages
    protected void onEntryClick(Player player, int index) {}

    // Extra buttons in the free slots
    protected void decorate(Player player, Inventory inventory) {}

    protected void onButtonClick(Player player, int slot) {}

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
            default -> {
                int position = entryPosition(slot);
                if (position >= 0) onEntryClick(player, page * ENTRY_SLOTS.length + position);
                else onButtonClick(player, slot);
            }
        }
    }

    // Redrawn in place so turning a page does not reopen the window
    protected void render(Player player) {
        inventory.clear();

        ItemStack glass = button(Material.GRAY_STAINED_GLASS_PANE, null);
        for (int slot = 0; slot < SIZE; slot++) {
            int row = slot / 9, col = slot % 9;
            if (row == 0 || row == 5 || col == 0 || col == 8) inventory.setItem(slot, glass);
        }

        List<ItemStack> entries = entries(player);
        int pages = pageCount(entries.size());
        page = Math.min(page, pages - 1);

        int start = page * ENTRY_SLOTS.length;
        for (int i = 0; i < ENTRY_SLOTS.length && start + i < entries.size(); i++) {
            inventory.setItem(ENTRY_SLOTS[i], entries.get(start + i));
        }

        if (page > 0) inventory.setItem(PREVIOUS_SLOT, button(Material.ARROW, "<yellow>Previous Page"));
        if (page < pages - 1) inventory.setItem(NEXT_SLOT, button(Material.ARROW, "<yellow>Next Page"));

        TraderMenu hub = registry.getGui(HUB_ID);
        if (hub != null) inventory.setItem(BACK_SLOT, hub.getDisplayItem(player));

        List<Component> lore = new ArrayList<>(info(player, entries));
        lore.add(TextUtils.parse("<!italic><gray>Page: <white>" + (page + 1) + "/" + pages));
        ItemStack info = new ItemStack(Material.BOOK);
        info.editMeta(meta -> {
            meta.displayName(name);
            meta.lore(lore);
        });
        inventory.setItem(INFO_SLOT, info);

        inventory.setItem(CLOSE_SLOT, button(Material.BARRIER, "<red>Close"));
        decorate(player, inventory);
    }

    private void turnPage(Player player, int target) {
        if (target < 0 || target >= pageCount(entries(player).size())) return;

        page = target;
        render(player);
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1f);
    }

    private static int pageCount(int entries) {
        return Math.max(1, (entries + ENTRY_SLOTS.length - 1) / ENTRY_SLOTS.length);
    }

    private static int entryPosition(int slot) {
        for (int i = 0; i < ENTRY_SLOTS.length; i++) {
            if (ENTRY_SLOTS[i] == slot) return i;
        }
        return -1;
    }

    // Null name hides the tooltip, for the glass border
    protected static ItemStack button(Material material, String name) {
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
