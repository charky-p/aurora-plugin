package org.charky.aurora.features.fishing;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.charky.aurora.common.menu.PagedMenu;
import org.charky.aurora.common.util.TextUtils;
import org.charky.aurora.core.DataRegistry;
import org.charky.aurora.core.models.FishingSpotModel;
import org.charky.aurora.core.models.FishingSpotModel.FishModel;
import org.charky.aurora.features.player.BackpackData;
import org.charky.aurora.features.player.BackpackManager;
import org.charky.aurora.features.player.PlayerManager;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// The Fish Merchant at spawn: hands out the fishing rod and buys fish for Essence,
// each at its value in fishing.yml. Anyone may sell, fishing is apart from the zones
public class FishMerchantMenu extends PagedMenu {
    private static final int SELL_ALL_SLOT = 52;

    private final FishingManager fishing;
    private final BackpackManager backpacks;
    private final PlayerManager players;
    // What each entry after the rod sells
    private List<String> shown = List.of();

    public FishMerchantMenu(DataRegistry registry, FishingManager fishing, BackpackManager backpacks, PlayerManager players) {
        super(registry, "Fish Merchant");
        this.fishing = fishing;
        this.backpacks = backpacks;
        this.players = players;
    }

    // fish id -> Essence for one, every spot's
    private Map<String, Double> values() {
        Map<String, Double> values = new LinkedHashMap<>();
        for (FishingSpotModel spot : fishing.spots().values()) {
            for (FishModel fish : spot.fish) values.put(fish.id, fish.essence);
        }
        return values;
    }

    private static long worth(double value, long amount) {
        return (long) Math.floor(value * amount);
    }

    @Override
    protected List<ItemStack> entries(Player player) {
        List<ItemStack> items = new ArrayList<>();
        ItemStack rod = fishing.rod(player);
        rod.editMeta(meta -> {
            List<Component> lore = meta.hasLore() ? new ArrayList<>(meta.lore()) : new ArrayList<>();
            lore.add(Component.empty());
            lore.add(TextUtils.parse(fishing.carriesRod(player) ? "<!italic><green>You carry it" : "<!italic><yellow>Click to take it"));
            meta.lore(lore);
        });
        items.add(rod);

        List<String> ids = new ArrayList<>();
        BackpackData backpack = backpacks.get(player.getUniqueId());
        for (Map.Entry<String, Double> entry : values().entrySet()) {
            long amount = backpack == null ? 0 : backpack.getAmount(player, entry.getKey());
            long worth = worth(entry.getValue(), amount);
            ItemStack icon = registry.getAllItems().get(entry.getKey());
            if (amount <= 0 || worth <= 0 || icon == null) continue;
            ItemStack item = icon.clone();
            item.setAmount(1);
            item.editMeta(meta -> {
                meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
                meta.lore(List.of(
                        TextUtils.parse("<!italic><gray>In your backpack: <white>" + String.format("%,d", amount)),
                        TextUtils.parse("<!italic><gray>Worth: <gold>" + String.format("%,d", worth) + " Essence"),
                        Component.empty(),
                        TextUtils.parse("<!italic><yellow>Click to sell all of it")));
            });
            ids.add(entry.getKey());
            items.add(item);
        }
        shown = ids;
        return items;
    }

    @Override
    protected List<Component> info(Player player, List<ItemStack> entries) {
        return List.of(
                TextUtils.parse("<!italic><gray>Fish at the spot in each realm, its trader upgrades the rod."),
                TextUtils.parse("<!italic><gray>Rarer fish and later spots pay more."));
    }

    @Override
    protected void decorate(Player player, Inventory inventory) {
        inventory.setItem(SELL_ALL_SLOT, button(Material.GOLD_BLOCK, "<gold><b>Sell every fish"));
    }

    @Override
    protected void onButtonClick(Player player, int slot) {
        if (slot != SELL_ALL_SLOT) return;
        long total = 0;
        for (String id : values().keySet()) total += sell(player, id);
        paid(player, total);
        render(player);
    }

    @Override
    protected void onEntryClick(Player player, int index) {
        if (index == 0) {
            if (fishing.carriesRod(player)) return;
            if (!fishing.giveRod(player)) TextUtils.error(player, "Your inventory is full.");
            render(player);
            return;
        }
        if (index - 1 >= shown.size()) return;
        paid(player, sell(player, shown.get(index - 1)));
        render(player);
    }

    private long sell(Player player, String id) {
        BackpackData backpack = backpacks.get(player.getUniqueId());
        Double value = values().get(id);
        if (backpack == null || value == null) return 0;
        long amount = backpack.getAmount(player, id);
        long worth = worth(value, amount);
        if (worth <= 0) return 0;
        backpack.removeItem(id, amount);
        return worth;
    }

    private void paid(Player player, long total) {
        if (total <= 0) {
            TextUtils.error(player, "No fish worth selling");
            return;
        }
        players.addEssence(player, (int) Math.min(Integer.MAX_VALUE, total));
        TextUtils.success(player, "Sold for " + String.format("%,d", total) + " Essence");
        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1.2f);
    }
}
