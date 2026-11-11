package org.charky.aurora.features.armory;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.charky.aurora.common.menu.PagedMenu;
import org.charky.aurora.common.util.TextUtils;
import org.charky.aurora.core.DataRegistry;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

// Every mob gear drop in progression order, locked ones hint at the mob that drops them
public class ArmoryMenu extends PagedMenu {
    private final ArmoryManager armory;
    private List<String> ids = List.of();

    public ArmoryMenu(ArmoryManager armory, DataRegistry registry) {
        super(registry, "Armory");
        this.armory = armory;
    }

    @Override
    protected List<ItemStack> entries(Player player) {
        ids = new ArrayList<>(registry.getArmory().keySet());

        List<ItemStack> items = new ArrayList<>();
        for (Map.Entry<String, Component> entry : registry.getArmory().entrySet()) {
            String id = entry.getKey();
            if (!armory.isUnlocked(player.getUniqueId(), id)) {
                items.add(locked(entry.getValue()));
                continue;
            }

            ItemStack display = registry.getAllItems().get(id).clone();
            String hint = armory.carries(player, id) ? "<gray>You have this one" : "<yellow>Click to take one";
            display.editMeta(meta -> {
                List<Component> lore = meta.hasLore() ? new ArrayList<>(meta.lore()) : new ArrayList<>();
                lore.add(Component.empty());
                lore.add(TextUtils.parse("<!italic>" + hint));
                meta.lore(lore);
            });
            items.add(display);
        }
        return items;
    }

    private static ItemStack locked(Component mob) {
        ItemStack item = button(Material.GRAY_DYE, "<dark_gray>???");
        item.editMeta(meta -> meta.lore(List.of(
                TextUtils.parse("<!italic><gray>Dropped by ").append(mob))));
        return item;
    }

    @Override
    protected List<Component> info(Player player, List<ItemStack> entries) {
        return List.of(TextUtils.parse("<!italic><gray>Unlocked: <white>"
                + armory.unlockedCount(player.getUniqueId()) + "/" + entries.size()));
    }

    @Override
    protected void onEntryClick(Player player, int index) {
        if (index >= ids.size()) return;
        armory.retrieve(player, ids.get(index));
        render(player);
    }
}
