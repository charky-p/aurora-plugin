package org.charky.aurora.features.gauntlet;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.charky.aurora.common.menu.PagedMenu;
import org.charky.aurora.common.util.TextUtils;

import java.util.ArrayList;
import java.util.List;

// The starting kit (setup.csv): things bought for good, a level at a time, that every
// run starts with on top of the care packages. They join the lobby setup at once
public class SetupMenu extends PagedMenu {
    private final GauntletManager manager;
    private final GauntletRuns runs;
    private final List<GauntletData.SetupItem> items;

    public SetupMenu(GauntletManager manager, GauntletRuns runs) {
        super(manager.registry(), "Starting Kit");
        this.manager = manager;
        this.runs = runs;
        this.items = List.copyOf(manager.data().setup().values());
    }

    @Override
    protected List<ItemStack> entries(Player player) {
        GauntletProfile profile = manager.profile(player);
        List<ItemStack> shown = new ArrayList<>();
        for (GauntletData.SetupItem setup : items) {
            int level = profile.setup.getOrDefault(setup.id(), 0);
            ItemStack item = manager.setup().kitItem(setup, Math.max(level, 1));
            if (item == null) item = new ItemStack(Material.BARRIER);
            item.setAmount(1);
            item.editMeta(meta -> {
                meta.displayName(TextUtils.parse("<!italic><gold><b>" + setup.name()
                        + (setup.max() > 1 ? " <gray>" + level + "/" + setup.max() : "")));
                meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
                List<Component> lore = new ArrayList<>();
                lore.add(TextUtils.parse("<!italic><gray>" + setup.description()));
                lore.add(Component.empty());
                lore.add(TextUtils.parse("<!italic><gray>You start with: <white>" + setup.startsWith(level)));
                if (level >= setup.max()) {
                    lore.add(TextUtils.parse("<!italic><green>Fully bought"));
                } else {
                    int next = level + 1;
                    lore.add(TextUtils.parse("<!italic><gray>" + (level == 0 ? "Buy" : "Next") + ": <aqua>" + setup.shardsFor(next) + " Shards"
                            + (setup.coresFor(next) > 0 ? " <gray>+ <light_purple>" + setup.coresFor(next) + " Cores" : "")));
                    lore.add(TextUtils.parse("<!italic><yellow>Click to " + (level == 0 ? "buy" : "add more")));
                }
                meta.lore(lore);
                if (level > 0) meta.setEnchantmentGlintOverride(true);
            });
            shown.add(item);
        }
        return shown;
    }

    @Override
    protected List<Component> info(Player player, List<ItemStack> entries) {
        GauntletProfile profile = manager.profile(player);
        return List.of(TextUtils.parse("<!italic><gray>Every run starts with these,"),
                TextUtils.parse("<!italic><gray>on top of the care packages."),
                TextUtils.parse("<!italic><gray>Shards: <aqua>" + profile.shards + " <gray>Cores: <light_purple>" + profile.cores));
    }

    @Override
    protected void onEntryClick(Player player, int index) {
        if (index >= items.size()) return;
        String problem = manager.setup().buy(player, items.get(index).id());
        if (problem != null) TextUtils.error(player, problem);
        render(player);
    }

    @Override
    protected void back(Player player) {
        manager.openMenu(player);
    }
}
