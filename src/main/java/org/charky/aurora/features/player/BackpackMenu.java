package org.charky.aurora.features.player;

import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.charky.aurora.common.loot.MenuItem;
import org.charky.aurora.common.menu.PagedMenu;
import org.charky.aurora.common.util.TextUtils;
import org.charky.aurora.core.DataRegistry;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

// View only, the backpack holds resources so they don't clutter the inventory.
// We may later add moving items between the backpack and the inventory
public class BackpackMenu extends PagedMenu {
    private static final int ARMORY_SLOT = 52;

    private final BackpackData backpack;

    public BackpackMenu(BackpackData backpack, DataRegistry registry) {
        super(registry, "Backpack");
        this.backpack = backpack;
    }

    // Held resources in progression order, the order of resources.csv
    @Override
    protected List<ItemStack> entries(Player player) {
        Map<String, Long> held = backpack.getItems();

        List<ItemStack> items = new ArrayList<>();
        for (MenuItem resource : registry.getItems()) {
            long amount = held.getOrDefault(resource.getId(), 0L);
            if (amount <= 0) continue;

            ItemStack display = resource.getItem();
            display.editMeta(meta -> {
                List<Component> lore = meta.hasLore() ? new ArrayList<>(meta.lore()) : new ArrayList<>();
                lore.add(Component.empty());
                lore.add(TextUtils.parse("<!italic><gray>Amount: <white>" + amount));
                meta.lore(lore);
            });
            items.add(display);
        }
        return items;
    }

    @Override
    protected List<Component> info(Player player, List<ItemStack> entries) {
        return List.of(TextUtils.parse("<!italic><gray>Resources: <white>" + entries.size()));
    }

    @Override
    protected void decorate(Player player, Inventory inventory) {
        ItemStack icon = registry.getAllIcons().get("icon_armory");
        if (icon != null) inventory.setItem(ARMORY_SLOT, icon.clone());
    }

    @Override
    protected void onButtonClick(Player player, int slot) {
        if (slot == ARMORY_SLOT) registry.runAction("armory", player);
    }
}
