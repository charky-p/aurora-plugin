package org.charky.aurora.features.rank;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.charky.aurora.common.menu.PagedMenu;
import org.charky.aurora.common.util.TextUtils;
import org.charky.aurora.core.DataRegistry;

import java.util.ArrayList;
import java.util.List;

// Every rank, /ranks. Click one you own to wear it. The ones you don't say how to
// get them: hours played, or the price in /shop
public class RankMenu extends PagedMenu {
    private final RankManager ranks;
    private final List<Rank> all = Ranks.all();

    public RankMenu(RankManager ranks, DataRegistry registry) {
        super(registry, "Ranks");
        this.ranks = ranks;
    }

    @Override
    protected List<ItemStack> entries(Player player) {
        Rank worn = ranks.worn(player);
        List<ItemStack> items = new ArrayList<>();
        for (Rank rank : all) {
            boolean owned = ranks.owns(player, rank);
            ItemStack item = new ItemStack(owned ? Material.NAME_TAG : Material.GRAY_DYE);
            item.editMeta(meta -> {
                meta.displayName(TextUtils.parse("<!italic>").append(rank.component()));

                List<Component> lore = new ArrayList<>();
                lore.add(TextUtils.parse("<!italic><dark_gray>" + source(rank)));
                lore.add(Component.empty());
                if (rank == worn) lore.add(TextUtils.parse("<!italic><green>You are wearing this"));
                else if (owned) lore.add(TextUtils.parse("<!italic><yellow>Click to wear"));
                else lore.add(TextUtils.parse("<!italic><red>" + locked(player, rank)));
                meta.lore(lore);

                if (rank == worn) meta.setEnchantmentGlintOverride(true);
            });
            items.add(item);
        }
        return items;
    }

    private static String source(Rank rank) {
        return switch (rank.kind()) {
            case DEFAULT -> "Everyone starts with this";
            case PLAYTIME -> "Earned by playing for " + hours(rank.requirement()) + "h";
            case SHOP -> "Bought for " + String.format("%,d", rank.requirement()) + " Essence in /shop";
        };
    }

    private String locked(Player player, Rank rank) {
        if (rank.kind() == Rank.Kind.SHOP) return "Buy it in /shop";

        long left = rank.requirement() - ranks.playtimeOf(player);
        return "Play " + (left >= 3600 ? hours(left) + " more hours" : Math.max(1, left / 60) + " more minutes");
    }

    private static long hours(long seconds) {
        return seconds / 3600;
    }

    @Override
    protected List<Component> info(Player player, List<ItemStack> entries) {
        long owned = all.stream().filter(rank -> ranks.owns(player, rank)).count();
        long played = ranks.playtimeOf(player);
        return List.of(
                TextUtils.parse("<!italic><gray>Wearing: ").append(ranks.worn(player).component()),
                TextUtils.parse("<!italic><gray>Unlocked: <white>" + owned + "/" + all.size()),
                TextUtils.parse("<!italic><gray>Played: <white>" + played / 3600 + "h " + played % 3600 / 60 + "m"));
    }

    @Override
    protected void onEntryClick(Player player, int index) {
        if (index >= all.size()) return;

        Rank rank = all.get(index);
        if (!ranks.owns(player, rank) || rank == ranks.worn(player)) return;

        String problem = ranks.wear(player, rank);
        if (problem != null) {
            TextUtils.error(player, problem);
            return;
        }
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1f);
        render(player);
    }
}
