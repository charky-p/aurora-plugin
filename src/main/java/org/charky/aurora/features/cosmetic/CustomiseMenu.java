package org.charky.aurora.features.cosmetic;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.charky.aurora.common.menu.PagedMenu;
import org.charky.aurora.common.util.TextUtils;
import org.charky.aurora.core.DataRegistry;
import org.charky.aurora.features.cosmetic.Cosmetic.Slot;
import org.charky.aurora.features.cosmetic.Cosmetic.Type;
import org.charky.aurora.features.rank.RankManager;

import java.util.ArrayList;
import java.util.List;

// /customise: ranks first, then one entry for every kind of cosmetic
public class CustomiseMenu extends PagedMenu {
    private static final Material[] ICONS = {Material.OAK_HANGING_SIGN, Material.BLUE_DYE, Material.PAINTING,
            Material.OAK_SIGN, Material.BLAZE_POWDER, Material.IRON_SWORD, Material.SKELETON_SKULL,
            Cosmetics.TRIM_ICON, Material.AMETHYST_SHARD};

    private final CosmeticManager cosmetics;
    private final RankManager ranks;
    private final Type[] types = Type.values();

    public CustomiseMenu(CosmeticManager cosmetics, RankManager ranks, DataRegistry registry) {
        super(registry, "Customise");
        this.cosmetics = cosmetics;
        this.ranks = ranks;
    }

    @Override
    protected List<ItemStack> entries(Player player) {
        List<ItemStack> items = new ArrayList<>();

        ItemStack rank = button(Material.NAME_TAG, "<light_purple><b>Ranks");
        rank.editMeta(meta -> meta.lore(List.of(
                TextUtils.parse("<!italic><gray>Shown before your name"),
                Component.empty(),
                TextUtils.parse("<!italic><gray>Wearing: ").append(ranks.worn(player).component()))));
        items.add(rank);

        for (Type type : types) {
            ItemStack item = button(ICONS[type.ordinal()], "<yellow><b>" + type.title());
            Cosmetic worn = cosmetics.equipped(player.getUniqueId(), Slot.of(type));
            item.editMeta(meta -> {
                meta.lore(List.of(
                        TextUtils.parse("<!italic><gray>" + type.description()),
                        Component.empty(),
                        TextUtils.parse("<!italic><gray>Wearing: ").append(
                                worn == null ? TextUtils.parse("<dark_gray>Nothing") : worn.component())));
                meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ATTRIBUTES);
            });
            items.add(item);
        }
        return items;
    }

    @Override
    protected List<Component> info(Player player, List<ItemStack> entries) {
        return List.of(
                TextUtils.parse("<!italic><gray>How you look to everyone else."),
                TextUtils.parse("<!italic><gray>None of it changes how the game plays."));
    }

    @Override
    protected void onEntryClick(Player player, int index) {
        if (index == 0) ranks.openRanks(player);
        else if (index <= types.length) cosmetics.open(player, types[index - 1]);
    }
}
