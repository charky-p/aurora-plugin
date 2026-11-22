package org.charky.aurora.features.fishing;

import net.kyori.adventure.text.Component;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.charky.aurora.common.loot.MenuItem;
import org.charky.aurora.common.menu.CostLine;
import org.charky.aurora.common.menu.PagedMenu;
import org.charky.aurora.common.util.TextUtils;
import org.charky.aurora.core.DataRegistry;
import org.charky.aurora.core.models.FishingSpotModel;
import org.charky.aurora.core.models.FishingSpotModel.RodStepModel;
import org.charky.aurora.features.player.BackpackData;
import org.charky.aurora.features.player.BackpackManager;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

// A fishing spot's trader: the rod as the player has it and the next upgrade sold
// here. Clicking buys it, or hands the rod over when it is not carried
public class RodShopMenu extends PagedMenu {
    private final FishingManager fishing;
    private final BackpackManager backpacks;
    private final String spotId;

    public RodShopMenu(DataRegistry registry, FishingManager fishing, BackpackManager backpacks, String spotId) {
        super(registry, FishingManager.plain(registry.getFishingSpots().get(spotId).name) + " Rod");
        this.fishing = fishing;
        this.backpacks = backpacks;
        this.spotId = spotId;
    }

    private FishingSpotModel spot() {
        return fishing.spots().get(spotId);
    }

    @Override
    protected List<ItemStack> entries(Player player) {
        FishingSpotModel spot = spot();
        UUID uuid = player.getUniqueId();
        ItemStack item = fishing.rod(player);
        item.editMeta(meta -> {
            List<Component> lore = meta.hasLore() ? new ArrayList<>(meta.lore()) : new ArrayList<>();
            lore.add(Component.empty());
            long bought = spot.rod.stream().filter(step -> fishing.level(uuid, step.stat) >= step.level).count();
            lore.add(TextUtils.parse("<!italic><gray>Upgrades here: <white>" + bought + "/" + spot.rod.size()));

            RodStepModel next = fishing.next(uuid, spot);
            FishingSpotModel before = fishing.unfinishedBefore(uuid, spot);
            if (!fishing.carriesRod(player)) {
                lore.add(TextUtils.parse("<!italic><yellow>Click to take it"));
            } else if (next == null) {
                lore.add(TextUtils.parse("<!italic><green><b>✔ MAXED</b> <gray>here, the next spot sells more."));
            } else if (before != null) {
                lore.add(TextUtils.parse("<!italic><red>Finish the rod at " + FishingManager.plain(before.name) + " first"));
            } else {
                lore.add(TextUtils.parse("<!italic><gold>Next: <white>" + FishingManager.title(next.stat) + " " + next.level));
                BackpackData backpack = backpacks.get(uuid);
                boolean affordable = true;
                for (MenuItem cost : fishing.costs(next)) {
                    long have = backpack == null ? 0 : backpack.getAmount(player, cost.getId());
                    if (have < cost.getAmount()) affordable = false;
                    lore.add(CostLine.of(cost, have));
                }
                lore.add(TextUtils.parse(affordable ? "<!italic><yellow>Click to upgrade" : "<!italic><red>Not enough fish yet"));
            }
            meta.lore(lore);
        });
        return List.of(item);
    }

    @Override
    protected List<Component> info(Player player, List<ItemStack> entries) {
        return List.of(
                TextUtils.parse("<!italic><gray>Fortune catches more, Luck rarer fish, Lure bites sooner."),
                TextUtils.parse("<!italic><gray>Sell your fish to the Fish Merchant for Essence."));
    }

    @Override
    protected void onEntryClick(Player player, int index) {
        if (index != 0) return;
        if (!fishing.carriesRod(player)) {
            if (!fishing.giveRod(player)) TextUtils.error(player, "Your inventory is full.");
            render(player);
            return;
        }
        String problem = fishing.buy(player, spotId);
        if (problem != null) {
            TextUtils.error(player, problem);
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.5f, 1f);
        }
        render(player);
    }
}
