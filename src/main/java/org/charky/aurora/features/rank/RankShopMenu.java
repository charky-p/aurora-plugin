package org.charky.aurora.features.rank;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.charky.aurora.common.menu.PagedMenu;
import org.charky.aurora.common.util.TextUtils;
import org.charky.aurora.core.Aurora;
import org.charky.aurora.features.perk.Perk;
import org.charky.aurora.features.perk.PerkManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

// The Essence shop, /shop. Utility perks first, bought once each, then the ranks.
// Clicking a perk you own uses it: Dash turns on or off, a mount or the God Tool is
// handed over again if it was lost
public class RankShopMenu extends PagedMenu {
    private final Aurora plugin;
    private final RankManager ranks;
    private final PerkManager perks;
    private final List<Perk> upgrades = Arrays.stream(Perk.values()).filter(Perk::sold).toList();
    private final List<Rank> forSale = Ranks.ofKind(Rank.Kind.SHOP);

    public RankShopMenu(Aurora plugin) {
        super(plugin.getRegistry(), "Essence Shop");
        this.plugin = plugin;
        this.ranks = plugin.getRankManager();
        this.perks = plugin.getPerkManager();
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
            lore.add(TextUtils.parse("<!italic><gray>" + Character.toUpperCase(perk.effect().charAt(0)) + perk.effect().substring(1) + "."));
            lore.add(Component.empty());
            if (perk.maxLevel() > 1 && level > 0) {
                lore.add(TextUtils.parse("<!italic><gray>Level <white>" + level + "/" + perk.maxLevel()));
            }
            if (maxed) {
                lore.add(TextUtils.parse("<!italic><green>Owned"));
                lore.add(TextUtils.parse("<!italic><yellow>" + useHint(player, perk)));
            } else {
                lore.add(TextUtils.parse("<!italic><gray>Price: <gold>" + String.format("%,d", price) + " Essence"));
                String buy = level > 0 ? "Left click to upgrade" : "Click to buy";
                lore.add(TextUtils.parse(essence >= price ? "<!italic><yellow>" + buy
                        : "<!italic><red>" + String.format("%,d", price - essence) + " more Essence needed"));
                if (level > 0 && perk.mount() != null) lore.add(TextUtils.parse("<!italic><yellow>Right click for its saddle again"));
            }
            meta.lore(lore);

            if (maxed) meta.setEnchantmentGlintOverride(true);
        });
        return item;
    }

    private String useHint(Player player, Perk perk) {
        if (perk == Perk.DASH) return "Click for the Dash Feather again";
        if (perk.mount() != null) return "Click for its saddle again";
        if (perk == Perk.GOD_TOOL) return "Click for the tool again";
        return "Yours for good";
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

    // A mount that is owned but not maxed: left click upgrades it, right click hands
    // its saddle over. Anything maxed is used on any click
    @Override
    protected void onEntryClick(Player player, int index, boolean rightClick) {
        String problem = null;
        if (index < upgrades.size()) {
            Perk perk = upgrades.get(index);
            int level = perks.level(player, perk);
            boolean maxed = level >= perk.maxLevel();
            if (perks.owns(player, perk) && (maxed || rightClick)) use(player, perk);
            else {
                problem = perks.buy(player, perk);
                if (problem == null) bought(player, perk);
            }
        } else {
            int position = index - upgrades.size();
            if (position >= forSale.size()) return;

            Rank rank = forSale.get(position);
            if (ranks.owns(player, rank)) return;
            problem = ranks.buy(player, rank);
        }

        if (problem != null) TextUtils.error(player, problem);
        render(player);
    }

    // Just bought: Dash starts on, a mount's saddle or the God Tool is handed over
    private void bought(Player player, Perk perk) {
        int level = perks.level(player, perk);
        if (perk == Perk.DOUBLE_JUMP) TextUtils.success(player, "Double Jump: press jump again in mid-air");
        // An upgrade, not a first buy: they have the saddle already
        else if (perk.mount() != null && level > 1) {
            TextUtils.success(player, perk.display() + " <green>is now level " + level + ", call it again with its saddle to ride it");
        } else use(player, perk);
    }

    // Owned already: Dash turns on or off, a lost saddle or God Tool is handed over again
    private void use(Player player, Perk perk) {
        if (perk == Perk.DASH) {
            plugin.getDashListener().giveFeather(player);
        } else if (perk.mount() != null) {
            plugin.getMountManager().giveSaddle(player, perk.mount());
        } else if (perk == Perk.GOD_TOOL) {
            plugin.getToolManager().giveGodTool(player);
        }
    }
}
