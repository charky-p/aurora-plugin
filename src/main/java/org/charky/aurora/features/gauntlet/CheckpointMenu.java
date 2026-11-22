package org.charky.aurora.features.gauntlet;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.charky.aurora.common.menu.PagedMenu;
import org.charky.aurora.common.util.TextUtils;

import java.util.ArrayList;
import java.util.List;

// Where a run starts, for players past a boss floor: floor 1, or the floor after any
// boss floor they have cleared. Starting later skips the floors before (no Shards
// for them) and hands out their boons at random and a care package. Once unlocked,
// the heat button sets how hard the run is, and how much more it pays
public class CheckpointMenu extends PagedMenu {
    private static final int HEAT_SLOT = 46;
    private final GauntletManager manager;
    private final GauntletRuns runs;

    public CheckpointMenu(GauntletManager manager, GauntletRuns runs) {
        super(manager.registry(), "Start a Run");
        this.manager = manager;
        this.runs = runs;
    }

    @Override
    protected List<ItemStack> entries(Player player) {
        int every = Math.max(1, (int) manager.data().setting("boon_every", 5));
        List<ItemStack> items = new ArrayList<>();
        for (int floor : runs.checkpoints(player)) {
            ItemStack item = new ItemStack(floor == 1 ? Material.IRON_SWORD : Material.LODESTONE);
            int boons = (floor - 1) / every;
            item.editMeta(meta -> {
                meta.displayName(TextUtils.parse("<!italic><red><b>Floor " + floor));
                meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
                List<Component> lore = new ArrayList<>();
                if (floor == 1) {
                    lore.add(TextUtils.parse("<!italic><gray>From the bottom of the tower"));
                } else {
                    lore.add(TextUtils.parse("<!italic><gray>Skips floors 1-" + (floor - 1) + ", no Shards for them"));
                    lore.add(TextUtils.parse("<!italic><gray>Starts with <white>" + boons + "</white> random boons"));
                    lore.add(TextUtils.parse("<!italic><gray>and a care package"));
                }
                lore.add(Component.empty());
                lore.add(TextUtils.parse("<!italic><yellow>Click to start here"));
                meta.lore(lore);
            });
            items.add(item);
        }
        return items;
    }

    @Override
    protected List<Component> info(Player player, List<ItemStack> entries) {
        return List.of(TextUtils.parse("<!italic><gray>Clear a boss floor to start"),
                TextUtils.parse("<!italic><gray>after it next time."));
    }

    @Override
    protected void decorate(Player player, Inventory inventory) {
        int unlocked = runs.heatUnlocked(player);
        GauntletData data = manager.data();
        int heat = runs.heat(player);
        ItemStack button = new ItemStack(heat > 0 ? Material.BLAZE_POWDER : Material.GUNPOWDER);
        button.editMeta(meta -> {
            meta.displayName(TextUtils.parse("<!italic><gold><b>Heat " + heat + (unlocked > 0 ? " <gray>/ " + unlocked : "")));
            List<Component> lore = new ArrayList<>();
            if (unlocked == 0) {
                lore.add(TextUtils.parse("<!italic><gray>Harder runs that pay more."));
                lore.add(TextUtils.parse("<!italic><gray>Unlocks at best floor <white>" + (int) data.setting("heat_first", 15)));
            } else {
                lore.add(TextUtils.parse("<!italic><gray>Mobs: <white>+" + Math.round(heat * data.setting("heat_health", 25))
                        + "% health, +" + Math.round(heat * data.setting("heat_damage", 15)) + "% hits"));
                lore.add(TextUtils.parse("<!italic><gray>Shards: <aqua>+" + Math.round(heat * data.setting("heat_shards", 30)) + "%"));
                lore.add(Component.empty());
                lore.add(TextUtils.parse("<!italic><yellow>Click to change"));
            }
            meta.lore(lore);
        });
        inventory.setItem(HEAT_SLOT, button);
    }

    @Override
    protected void onButtonClick(Player player, int slot) {
        if (slot != HEAT_SLOT || runs.heatUnlocked(player) == 0) return;
        runs.nextHeat(player);
        render(player);
    }

    @Override
    protected void onEntryClick(Player player, int index) {
        List<Integer> floors = runs.checkpoints(player);
        if (index >= floors.size()) return;
        String problem = runs.begin(player, floors.get(index));
        if (problem != null) TextUtils.error(player, problem);
    }

    @Override
    protected void back(Player player) {
        manager.openMenu(player);
    }
}
