package org.charky.aurora.features.tool;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.charky.aurora.common.loot.MenuItem;
import org.charky.aurora.common.menu.PagedMenu;
import org.charky.aurora.common.util.TextUtils;
import org.charky.aurora.core.DataRegistry;
import org.charky.aurora.features.player.BackpackData;
import org.charky.aurora.features.tool.UpgradeZone.Status;
import org.charky.aurora.features.tool.UpgradeZone.Step;

import java.util.ArrayList;
import java.util.List;

// A zone's upgrades: one entry for every tool the zone improves, drawn as the
// player has it. Clicking a tool buys the next step of its chain, and a tool whose
// chain is finished here says so
public class UpgradeMenu extends PagedMenu {
    private final UpgradeShop shop;
    private final ToolManager tools;
    private final UpgradeZone zone;
    private final List<ToolType> types = new ArrayList<>();

    public UpgradeMenu(UpgradeShop shop, DataRegistry registry, UpgradeZone zone) {
        super(registry, zone.name() + " Upgrades");
        this.shop = shop;
        this.tools = shop.tools();
        this.zone = zone;
        for (String id : zone.tools()) {
            ToolType type = registry.getToolType(id);
            if (type != null) types.add(type);
        }
    }

    @Override
    protected List<ItemStack> entries(Player player) {
        BackpackData backpack = shop.backpackOf(player);
        List<ItemStack> items = new ArrayList<>();
        for (ToolType type : types) {
            Step next = shop.next(player, zone, type.id());
            Status status = shop.status(player, zone, type.id());
            boolean owned = tools.owns(player.getUniqueId(), type.id());

            ItemStack item = tools.build(player, type);
            item.editMeta(meta -> {
                List<Component> lore = meta.hasLore() ? new ArrayList<>(meta.lore()) : new ArrayList<>();
                lore.add(Component.empty());
                lore.add(TextUtils.parse("<!italic><gray>Upgrades here: <white>" + shop.bought(player, zone, type.id())
                        + "/" + zone.chain(type.id()).size()));

                if (owned && !tools.carries(player, type)) {
                    lore.add(TextUtils.parse("<!italic><yellow>Click to take it"));
                } else if (!owned && next != null && !next.stat().equals(ToolLevels.UNLOCK)) {
                    // Sold for the first time in another zone, never handed out here
                    lore.add(TextUtils.parse("<!italic><red>You need to buy the " + type.name() + " first"));
                } else if (status == Status.MAXED) {
                    Component name = meta.displayName();
                    meta.displayName(TextUtils.parse("<!italic><green><b>✔ MAXED</b> <dark_gray>| ")
                            .append(name == null ? Component.empty() : name));
                    lore.add(TextUtils.parse("<!italic><green>Nothing left to buy for it here."));
                    lore.add(TextUtils.parse("<!italic><gray>The next zone sells more."));
                } else if (status == Status.BEHIND) {
                    lore.add(TextUtils.parse("<!italic><red>" + shop.behindReason(player, zone, type)));
                } else {
                    lore.add(TextUtils.parse("<!italic><gold>Next: <white>").append(shop.describe(player, zone, type, next)));
                    boolean affordable = true;
                    for (MenuItem cost : next.costs()) {
                        long have = backpack == null ? 0 : backpack.getAmount(player, cost.getId());
                        if (have < cost.getAmount()) affordable = false;
                        lore.add(TextUtils.parse("<!italic><gray>» ").append(cost.getItem().effectiveName())
                                .append(TextUtils.parse((have >= cost.getAmount() ? " <green>" : " <red>")
                                        + "(" + have + "/" + cost.getAmount() + ")")));
                    }
                    String click = next.stat().equals(ToolLevels.UNLOCK) ? "Click to buy it" : "Click to upgrade";
                    lore.add(TextUtils.parse(affordable ? "<!italic><yellow>" + click : "<!italic><red>Not enough yet"));
                }
                meta.lore(lore);
            });
            // A finished tool is shown as an emerald block wearing the tool's name and
            // stats, so a maxed shop reads at a glance
            items.add(status == Status.MAXED && owned && tools.carries(player, type) ? item.withType(Material.EMERALD_BLOCK) : item);
        }
        return items;
    }

    @Override
    protected List<Component> info(Player player, List<ItemStack> entries) {
        long maxed = types.stream().filter(type -> shop.status(player, zone, type.id()) == Status.MAXED).count();
        return List.of(
                TextUtils.parse("<!italic><gray>Your tools get better, they are never replaced."),
                TextUtils.parse("<!italic><gray>Maxed here: <white>" + maxed + "/" + types.size()));
    }

    @Override
    protected void onEntryClick(Player player, int index) {
        if (index >= types.size()) return;

        ToolType type = types.get(index);
        // A tool of theirs they do not carry is handed back, one they never had is bought
        if (tools.owns(player.getUniqueId(), type.id()) && !tools.carries(player, type)) {
            if (!tools.give(player, type)) TextUtils.error(player, "Your inventory is full.");
            render(player);
            return;
        }

        String problem = shop.buy(player, zone, type.id());
        if (problem != null) {
            TextUtils.error(player, problem);
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.5f, 1f);
        }
        render(player);
    }
}
