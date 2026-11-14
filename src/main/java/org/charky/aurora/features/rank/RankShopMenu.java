package org.charky.aurora.features.rank;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.charky.aurora.common.menu.PagedMenu;
import org.charky.aurora.common.util.TextUtils;
import org.charky.aurora.core.DataRegistry;

import java.util.ArrayList;
import java.util.List;

// The Essence shop, /shop. Ranks are the first thing Essence buys
public class RankShopMenu extends PagedMenu {
    private final RankManager ranks;
    private final List<Rank> forSale = Ranks.ofKind(Rank.Kind.SHOP);

    public RankShopMenu(RankManager ranks, DataRegistry registry) {
        super(registry, "Essence Shop");
        this.ranks = ranks;
    }

    @Override
    protected List<ItemStack> entries(Player player) {
        int essence = ranks.essenceOf(player);
        List<ItemStack> items = new ArrayList<>();
        for (Rank rank : forSale) {
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
            items.add(item);
        }
        return items;
    }

    @Override
    protected List<Component> info(Player player, List<ItemStack> entries) {
        return List.of(
                TextUtils.parse("<!italic><gray>Your Essence: <gold>" + String.format("%,d", ranks.essenceOf(player))),
                TextUtils.parse("<!italic><dark_gray>From levels, collections and Luck"));
    }

    @Override
    protected void onEntryClick(Player player, int index) {
        if (index >= forSale.size()) return;

        Rank rank = forSale.get(index);
        if (ranks.owns(player, rank)) return;

        String problem = ranks.buy(player, rank);
        if (problem != null) TextUtils.error(player, problem);
        render(player);
    }
}
