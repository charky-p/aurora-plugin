package org.charky.aurora.features.tool;

import net.kyori.adventure.text.Component;
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
    private final ToolManager tools;
    private final UpgradeZone zone;
    private final List<ToolType> types = new ArrayList<>();

    public UpgradeMenu(ToolManager tools, DataRegistry registry, UpgradeZone zone) {
        super(registry, zone.name() + " Upgrades");
        this.tools = tools;
        this.zone = zone;
        for (String id : zone.tools()) {
            ToolType type = registry.getToolType(id);
            if (type != null) types.add(type);
        }
    }

    @Override
    protected List<ItemStack> entries(Player player) {
        BackpackData backpack = tools.backpackOf(player);
        List<ItemStack> items = new ArrayList<>();
        for (ToolType type : types) {
            Step next = tools.next(player, zone, type.id());
            Status status = tools.status(player, zone, type.id());

            ItemStack item = tools.build(player, type);
            item.editMeta(meta -> {
                List<Component> lore = meta.hasLore() ? new ArrayList<>(meta.lore()) : new ArrayList<>();
                lore.add(Component.empty());

                if (!tools.carries(player, type)) {
                    lore.add(TextUtils.parse("<!italic><yellow>Click to take it"));
                } else if (status == Status.MAXED) {
                    lore.add(TextUtils.parse("<!italic><green><b>MAXED</b> <green>here, the next zone has more"));
                } else if (status == Status.BEHIND) {
                    lore.add(TextUtils.parse("<!italic><red>Finish this tool's upgrades in the zone before first"));
                } else {
                    lore.add(TextUtils.parse("<!italic><gold>Next: <white>").append(tools.describe(type, next)));
                    boolean affordable = true;
                    for (MenuItem cost : next.costs()) {
                        long have = backpack == null ? 0 : backpack.getAmount(player, cost.getId());
                        if (have < cost.getAmount()) affordable = false;
                        lore.add(TextUtils.parse("<!italic><gray>» ").append(cost.getItem().effectiveName())
                                .append(TextUtils.parse((have >= cost.getAmount() ? " <green>" : " <red>")
                                        + "(" + have + "/" + cost.getAmount() + ")")));
                    }
                    lore.add(TextUtils.parse(affordable ? "<!italic><yellow>Click to upgrade" : "<!italic><red>Not enough yet"));
                }
                meta.lore(lore);
            });
            items.add(item);
        }
        return items;
    }

    @Override
    protected List<Component> info(Player player, List<ItemStack> entries) {
        long maxed = types.stream().filter(type -> tools.status(player, zone, type.id()) == Status.MAXED).count();
        return List.of(
                TextUtils.parse("<!italic><gray>Your tools get better, they are never replaced."),
                TextUtils.parse("<!italic><gray>Maxed here: <white>" + maxed + "/" + types.size()));
    }

    @Override
    protected void onEntryClick(Player player, int index) {
        if (index >= types.size()) return;

        ToolType type = types.get(index);
        if (!tools.carries(player, type)) {
            if (!tools.give(player, type)) TextUtils.error(player, "Your inventory is full.");
            render(player);
            return;
        }

        String problem = tools.buy(player, zone, type.id());
        if (problem != null) {
            TextUtils.error(player, problem);
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.5f, 1f);
        }
        render(player);
    }
}
