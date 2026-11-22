package org.charky.aurora.features.gauntlet;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.charky.aurora.common.menu.PagedMenu;
import org.charky.aurora.common.util.TextUtils;

import java.util.ArrayList;
import java.util.List;

// Talents (talents.csv): permanent upgrades for runs, bought a level at a time with
// Shards and some with Cores: more hearts, more Shards, likelier care package loot,
// boon rerolls, an extra boon to pick from, a Second Wind. They never count in PvP
public class TalentsMenu extends PagedMenu {
    private final GauntletManager manager;
    private final List<GauntletData.Talent> talents;

    public TalentsMenu(GauntletManager manager) {
        super(manager.registry(), "Talents");
        this.manager = manager;
        this.talents = List.copyOf(manager.data().talents().values());
    }

    @Override
    protected List<ItemStack> entries(Player player) {
        GauntletProfile profile = manager.profile(player);
        List<ItemStack> items = new ArrayList<>();
        for (GauntletData.Talent talent : talents) {
            int level = profile.talent(talent.id());
            Material icon = Material.matchMaterial(talent.icon());
            ItemStack item = new ItemStack(icon == null || !icon.isItem() ? Material.BOOK : icon);
            item.editMeta(meta -> {
                meta.displayName(TextUtils.parse("<!italic><green><b>" + talent.name()
                        + (talent.max() > 1 ? " <gray>" + level + "/" + talent.max() : "")));
                meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
                List<Component> lore = new ArrayList<>();
                lore.add(TextUtils.parse("<!italic><gray>" + talent.description()));
                lore.add(Component.empty());
                if (level >= talent.max()) {
                    lore.add(TextUtils.parse("<!italic><green>Fully learned"));
                } else {
                    int next = level + 1;
                    lore.add(TextUtils.parse("<!italic><gray>" + (level == 0 ? "Learn" : "Next") + ": <aqua>" + talent.shardsFor(next) + " Shards"
                            + (talent.coresFor(next) > 0 ? " <gray>+ <light_purple>" + talent.coresFor(next) + " Cores" : "")));
                    lore.add(TextUtils.parse("<!italic><yellow>Click to " + (level == 0 ? "learn" : "level up")));
                }
                meta.lore(lore);
                if (level > 0) meta.setEnchantmentGlintOverride(true);
            });
            items.add(item);
        }
        return items;
    }

    @Override
    protected List<Component> info(Player player, List<ItemStack> entries) {
        GauntletProfile profile = manager.profile(player);
        return List.of(TextUtils.parse("<!italic><gray>Permanent upgrades for runs,"),
                TextUtils.parse("<!italic><gray>never counted in PvP."),
                TextUtils.parse("<!italic><gray>Shards: <aqua>" + profile.shards + " <gray>Cores: <light_purple>" + profile.cores));
    }

    @Override
    protected void onEntryClick(Player player, int index) {
        if (index >= talents.size()) return;
        String problem = manager.buyTalent(player, talents.get(index).id());
        if (problem != null) TextUtils.error(player, problem);
        render(player);
    }

    @Override
    protected void back(Player player) {
        manager.openMenu(player);
    }
}
