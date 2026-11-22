package org.charky.aurora.features.gauntlet;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.charky.aurora.common.menu.PagedMenu;
import org.charky.aurora.common.util.TextUtils;
import org.charky.aurora.core.DataRegistry;

import java.util.ArrayList;
import java.util.List;

// The three boons offered every 5 floors, one to pick. They last for this run only
public class BoonMenu extends PagedMenu {
    private final GauntletRuns runs;
    private final List<GauntletData.Boon> offered;

    public BoonMenu(DataRegistry registry, GauntletRuns runs, Player player) {
        super(registry, "Pick a Boon");
        this.runs = runs;
        this.offered = runs.offered(player);
    }

    @Override
    protected List<ItemStack> entries(Player player) {
        List<ItemStack> items = new ArrayList<>();
        for (GauntletData.Boon boon : offered) {
            Material icon = Material.matchMaterial(boon.icon());
            ItemStack item = new ItemStack(icon == null || !icon.isItem() ? Material.NETHER_STAR : icon);
            int stacks = runs.stacks(player, boon.id());
            item.editMeta(meta -> {
                meta.displayName(TextUtils.parse("<!italic><gold><b>" + boon.name()));
                meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
                List<Component> lore = new ArrayList<>();
                lore.add(TextUtils.parse("<!italic><gray>" + boon.description()));
                lore.add(Component.empty());
                lore.add(TextUtils.parse("<!italic><gray>Taken: <white>" + stacks + "/" + boon.max()));
                lore.add(TextUtils.parse("<!italic><yellow>Click to take it"));
                meta.lore(lore);
            });
            items.add(item);
        }
        return items;
    }

    @Override
    protected List<Component> info(Player player, List<ItemStack> entries) {
        return List.of(TextUtils.parse("<!italic><gray>Upgrades for this run only."),
                TextUtils.parse("<!italic><gray>The next floor starts once you pick."));
    }

    @Override
    protected void onEntryClick(Player player, int index) {
        if (index < offered.size()) runs.choose(player, offered.get(index).id());
    }

    // Nothing above it to go back to
    @Override
    protected void back(Player player) {
        player.closeInventory();
    }
}
