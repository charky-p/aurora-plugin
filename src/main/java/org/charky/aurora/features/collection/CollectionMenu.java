package org.charky.aurora.features.collection;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.charky.aurora.common.loot.MenuItem;
import org.charky.aurora.common.menu.PagedMenu;
import org.charky.aurora.common.util.TextUtils;
import org.charky.aurora.core.DataRegistry;

import java.util.ArrayList;
import java.util.List;

// Every resource in progression order with how many the player has ever gathered,
// the tier that total has reached and what the next one needs and pays
public class CollectionMenu extends PagedMenu {
    private final CollectionManager collections;

    public CollectionMenu(CollectionManager collections, DataRegistry registry) {
        super(registry, "Collections");
        this.collections = collections;
    }

    @Override
    protected List<ItemStack> entries(Player player) {
        List<ItemStack> items = new ArrayList<>();
        for (MenuItem resource : registry.getItems()) {
            if (!registry.isLootable(resource.getId())) continue;

            long amount = collections.get(player.getUniqueId(), resource.getId());
            // Something is there, but not what, until the player has found one
            if (amount <= 0) {
                ItemStack unknown = button(Material.GRAY_DYE, "<dark_gray>???");
                unknown.editMeta(meta -> meta.lore(List.of(TextUtils.parse("<!italic><gray>Not found yet"))));
                items.add(unknown);
                continue;
            }
            int base = registry.getCollectionBase(resource.getId());
            int tier = CollectionTiers.tier(base, amount);
            long next = CollectionTiers.nextThreshold(base, amount);

            ItemStack display = resource.getItem();
            display.editMeta(meta -> {
                List<Component> lore = new ArrayList<>();
                lore.add(TextUtils.parse("<!italic><gray>Collected: <white>" + String.format("%,d", amount)));
                lore.add(TextUtils.parse("<!italic><gray>Tier: " + (tier == 0 ? "<dark_gray>None yet"
                        : "<yellow>" + CollectionTiers.numeral(tier) + " <dark_gray>of " + CollectionTiers.numeral(CollectionTiers.count()))));
                lore.add(Component.empty());
                lore.add(TextUtils.parse(next < 0 ? "<!italic><green>Complete"
                        : "<!italic><gray>Next: <white>" + String.format("%,d", next)
                        + " <dark_gray>(<gold>+" + CollectionTiers.nextReward(base, amount) + " Essence<dark_gray>)"));
                meta.lore(lore);
                if (next < 0) meta.setEnchantmentGlintOverride(true);
            });
            items.add(display);
        }
        return items;
    }

    private long found(Player player) {
        return registry.getItems().stream().filter(resource -> registry.isLootable(resource.getId())
                && collections.get(player.getUniqueId(), resource.getId()) > 0).count();
    }

    @Override
    protected List<Component> info(Player player, List<ItemStack> entries) {
        int reached = 0, all = 0;
        for (MenuItem resource : registry.getItems()) {
            if (registry.isLootable(resource.getId())) {
                all++;
                reached += CollectionTiers.tier(registry.getCollectionBase(resource.getId()),
                        collections.get(player.getUniqueId(), resource.getId()));
            }
        }
        return List.of(
                TextUtils.parse("<!italic><gray>Every resource you have ever gathered."),
                TextUtils.parse("<!italic><gray>Found: <white>" + found(player) + "/" + all),
                TextUtils.parse("<!italic><gray>Tiers reached: <white>" + reached + "/" + all * CollectionTiers.count()));
    }
}
