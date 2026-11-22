package org.charky.aurora.features.gauntlet;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.charky.aurora.common.menu.PagedMenu;
import org.charky.aurora.common.util.TextUtils;
import org.charky.aurora.core.DataRegistry;

import java.util.ArrayList;
import java.util.List;

// The boons offered every few floors, one to pick, coloured by rarity. They last for
// this run only. With rerolls left (the Reroll talent) a fresh pick can be drawn
public class BoonMenu extends PagedMenu {
    private static final int REROLL_SLOT = 52;

    private final GauntletRuns runs;

    public BoonMenu(DataRegistry registry, GauntletRuns runs, Player player) {
        super(registry, "Pick a Boon");
        this.runs = runs;
    }

    @Override
    protected List<ItemStack> entries(Player player) {
        List<ItemStack> items = new ArrayList<>();
        for (GauntletData.Boon boon : runs.offered(player)) {
            Material icon = Material.matchMaterial(boon.icon());
            ItemStack item = new ItemStack(icon == null || !icon.isItem() ? Material.NETHER_STAR : icon);
            int stacks = runs.stacks(player, boon.id());
            item.editMeta(meta -> {
                meta.displayName(TextUtils.parse("<!italic>" + boon.colour() + "<b>" + boon.name()));
                meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
                List<Component> lore = new ArrayList<>();
                lore.add(TextUtils.parse("<!italic>" + boon.colour() + Character.toUpperCase(boon.rarity().charAt(0)) + boon.rarity().substring(1)));
                lore.add(TextUtils.parse("<!italic><gray>" + boon.description()));
                lore.add(Component.empty());
                lore.add(TextUtils.parse("<!italic><gray>Taken: <white>" + stacks + "/" + boon.max()));
                lore.add(TextUtils.parse("<!italic><yellow>Click to take it"));
                meta.lore(lore);
                if (!boon.rarity().equals("common")) meta.setEnchantmentGlintOverride(true);
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
    protected void decorate(Player player, Inventory inventory) {
        int rerolls = runs.rerolls(player);
        if (rerolls <= 0) return;
        ItemStack reroll = new ItemStack(Material.ENDER_EYE);
        reroll.editMeta(meta -> {
            meta.displayName(TextUtils.parse("<!italic><aqua><b>Reroll"));
            meta.lore(List.of(TextUtils.parse("<!italic><gray>A fresh pick of boons"),
                    TextUtils.parse("<!italic><gray>Rerolls left this run: <white>" + rerolls)));
        });
        inventory.setItem(REROLL_SLOT, reroll);
    }

    @Override
    protected void onButtonClick(Player player, int slot) {
        if (slot != REROLL_SLOT) return;
        String problem = runs.reroll(player);
        if (problem != null) TextUtils.error(player, problem);
        render(player);
    }

    @Override
    protected void onEntryClick(Player player, int index) {
        List<GauntletData.Boon> offered = runs.offered(player);
        if (index < offered.size()) runs.choose(player, offered.get(index).id());
    }

    // Nothing above it to go back to
    @Override
    protected void back(Player player) {
        player.closeInventory();
    }
}
