package org.charky.aurora.features.gauntlet;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.charky.aurora.common.menu.PagedMenu;
import org.charky.aurora.common.util.TextUtils;
import org.charky.aurora.features.cosmetic.Cosmetic;
import org.charky.aurora.features.cosmetic.Cosmetics;

import java.util.ArrayList;
import java.util.List;

// Cosmetics bought with Shards (shardshop.csv), Gauntlet only, worn in /customise
public class ShardShopMenu extends PagedMenu {
    private final GauntletManager manager;
    private final List<GauntletData.ShopItem> items;

    public ShardShopMenu(GauntletManager manager) {
        super(manager.registry(), "Shard Shop");
        this.manager = manager;
        this.items = manager.data().shop().stream().filter(item -> Cosmetics.get(item.cosmetic()) != null).toList();
    }

    @Override
    protected List<ItemStack> entries(Player player) {
        int shards = manager.profile(player).shards;
        List<ItemStack> shown = new ArrayList<>();
        for (GauntletData.ShopItem shopItem : items) {
            Cosmetic cosmetic = Cosmetics.get(shopItem.cosmetic());
            boolean owned = manager.ownsCosmetic(player, cosmetic);
            ItemStack item = new ItemStack(cosmetic.icon() == null ? Material.NAME_TAG : cosmetic.icon());
            item.editMeta(meta -> {
                meta.displayName(TextUtils.parse("<!italic>" + cosmetic.display()));
                meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
                List<Component> lore = new ArrayList<>();
                lore.add(TextUtils.parse("<!italic><gray>" + cosmetic.type().name().charAt(0) + cosmetic.type().name().substring(1).toLowerCase().replace('_', ' ')
                        + " <dark_gray>- worn in /customise"));
                lore.add(Component.empty());
                if (owned) {
                    lore.add(TextUtils.parse("<!italic><green>Owned"));
                } else {
                    lore.add(TextUtils.parse("<!italic><gray>Price: <aqua>" + String.format("%,d", shopItem.shards()) + " Shards"));
                    lore.add(TextUtils.parse(shards >= shopItem.shards() ? "<!italic><yellow>Click to buy"
                            : "<!italic><red>" + String.format("%,d", shopItem.shards() - shards) + " more Shards needed"));
                }
                meta.lore(lore);
                if (owned) meta.setEnchantmentGlintOverride(true);
            });
            shown.add(item);
        }
        return shown;
    }

    @Override
    protected List<Component> info(Player player, List<ItemStack> entries) {
        return List.of(TextUtils.parse("<!italic><gray>Cosmetics only the Gauntlet sells."),
                TextUtils.parse("<!italic><gray>Shards: <aqua>" + manager.profile(player).shards));
    }

    @Override
    protected void onEntryClick(Player player, int index) {
        if (index >= items.size()) return;
        String problem = manager.buyCosmetic(player, items.get(index));
        if (problem != null) TextUtils.error(player, problem);
        render(player);
    }

    @Override
    protected void back(Player player) {
        manager.openMenu(player);
    }
}
