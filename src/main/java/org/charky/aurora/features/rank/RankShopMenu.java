package org.charky.aurora.features.rank;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.charky.aurora.common.menu.PagedMenu;
import org.charky.aurora.common.util.TextUtils;
import org.charky.aurora.core.DataRegistry;
import org.charky.aurora.features.perk.Perk;
import org.charky.aurora.features.perk.PerkManager;

import java.util.ArrayList;
import java.util.List;

// The Essence shop, /shop. Perks first, a level at a time, then the ranks
public class RankShopMenu extends PagedMenu {
    private final RankManager ranks;
    private final PerkManager perks;
    private final Perk[] upgrades = Perk.values();
    private final List<Rank> forSale = Ranks.ofKind(Rank.Kind.SHOP);

    public RankShopMenu(RankManager ranks, PerkManager perks, DataRegistry registry) {
        super(registry, "Essence Shop");
        this.ranks = ranks;
        this.perks = perks;
    }

    @Override
    protected List<ItemStack> entries(Player player) {
        int essence = ranks.essenceOf(player);
        List<ItemStack> items = new ArrayList<>();
        for (Perk perk : upgrades) items.add(perkItem(player, perk, essence));
        for (Rank rank : forSale) items.add(rankItem(player, rank, essence));
        return items;
    }

    private ItemStack perkItem(Player player, Perk perk, int essence) {
        int level = perks.level(player, perk);
        boolean maxed = level >= perk.maxLevel();
        int price = maxed ? 0 : perk.cost(level + 1);

        ItemStack item = new ItemStack(perk.icon());
        item.editMeta(meta -> {
            meta.displayName(TextUtils.parse("<!italic>" + perk.display()));
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);

            List<Component> lore = new ArrayList<>();
            lore.add(TextUtils.parse("<!italic><gray>A permanent upgrade: " + perk.effect() + "."));
            lore.add(Component.empty());
            lore.add(TextUtils.parse("<!italic><gray>Level: <white>" + level + "<dark_gray>/<gray>" + perk.maxLevel()));
            if (maxed) lore.add(TextUtils.parse("<!italic><green>Complete"));
            else {
                lore.add(TextUtils.parse("<!italic><gray>Next level: <gold>" + String.format("%,d", price) + " Essence"));
                lore.add(TextUtils.parse(essence >= price ? "<!italic><yellow>Click to buy"
                        : "<!italic><red>" + String.format("%,d", price - essence) + " more Essence needed"));
            }
            meta.lore(lore);

            if (maxed) meta.setEnchantmentGlintOverride(true);
        });
        item.setAmount(Math.clamp(level, 1, 64));
        return item;
    }

    private ItemStack rankItem(Player player, Rank rank, int essence) {
        boolean owned = ranks.owns(player, rank);
        boolean affordable = essence >= rank.requirement();

        ItemStack item = new ItemStack(Material.NAME_TAG);
        item.editMeta(meta -> {
            meta.displayName(TextUtils.parse("<!italic>").append(rank.component()));

            List<Component> lore = new ArrayList<>();
            lore.add(TextUtils.parse("<!italic><gray>A rank shown by your name in chat,"));
            lore.add(TextUtils.parse("<!italic><gray>the tab list and the sidebar."));
            lore.add(Component.empty());
            lore.add(TextUtils.parse("<!italic><gray>Price: <gold>" + String.format("%,d", rank.requirement()) + " Essence"));
            if (owned) lore.add(TextUtils.parse("<!italic><green>Owned - wear it with /ranks"));
            else if (affordable) lore.add(TextUtils.parse("<!italic><yellow>Click to buy"));
            else lore.add(TextUtils.parse("<!italic><red>" + String.format("%,d", rank.requirement() - essence) + " more Essence needed"));
            meta.lore(lore);

            if (owned) meta.setEnchantmentGlintOverride(true);
        });
        return item;
    }

    @Override
    protected List<Component> info(Player player, List<ItemStack> entries) {
        return List.of(
                TextUtils.parse("<!italic><gray>Your Essence: <gold>" + String.format("%,d", ranks.essenceOf(player))),
                TextUtils.parse("<!italic><dark_gray>From levels, collections and Luck"));
    }

    @Override
    protected void onEntryClick(Player player, int index) {
        String problem;
        if (index < upgrades.length) {
            problem = perks.buy(player, upgrades[index]);
        } else {
            int position = index - upgrades.length;
            if (position >= forSale.size()) return;

            Rank rank = forSale.get(position);
            if (ranks.owns(player, rank)) return;
            problem = ranks.buy(player, rank);
        }

        if (problem != null) TextUtils.error(player, problem);
        render(player);
    }
}
