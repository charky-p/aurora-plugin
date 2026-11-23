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
// boss floor they have cleared. Starting later skips the floors before (they pay
// nothing) and hands out their boons at random. Heat makes the run harder
// and pay more; Hard Mode raises the floors below the party's gear to its level
public class CheckpointMenu extends PagedMenu {
    private static final int HEAT_SLOT = 46;
    private static final int HARD_SLOT = 47;
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
                TextUtils.parse("<!italic><gray>after it next time. Heat and"),
                TextUtils.parse("<!italic><gray>Hard Mode are below."));
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
        inventory.setItem(HARD_SLOT, hardButton(player));
    }

    // Hard Mode: on or off, and what it does at the party's gear
    private ItemStack hardButton(Player player) {
        int hard = runs.hardFloor(player);
        boolean on = runs.hardChosen(player);
        ItemStack button = new ItemStack(on ? Material.NETHERITE_SWORD : Material.STONE_SWORD);
        button.editMeta(meta -> {
            meta.displayName(TextUtils.parse("<!italic><dark_red><b>Hard Mode " + (on ? "<green>ON" : "<gray>OFF")));
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            List<Component> lore = new ArrayList<>();
            if (hard <= 1) {
                lore.add(TextUtils.parse("<!italic><gray>Floors below your gear's level play"));
                lore.add(TextUtils.parse("<!italic><gray>and pay at it. Upgrade gear first."));
            } else {
                lore.add(TextUtils.parse("<!italic><gray>Floors below <white>" + hard + "</white> play and pay"));
                lore.add(TextUtils.parse("<!italic><gray>as floor " + hard + ", where your gear stands,"));
                lore.add(TextUtils.parse("<!italic><gray>and every floor pays <aqua>+" + Math.round(manager.data().setting("hard_shards", 25))
                        + "%</aqua> Shards."));
                lore.add(TextUtils.parse("<!italic><gray>Bosses keep their Cores."));
                lore.add(Component.empty());
                lore.add(TextUtils.parse("<!italic><yellow>Click to turn it " + (on ? "off" : "on")));
            }
            meta.lore(lore);
        });
        return button;
    }

    @Override
    protected void onButtonClick(Player player, int slot) {
        if (slot == HARD_SLOT && runs.hardFloor(player) > 1) {
            runs.toggleHard(player);
            render(player);
            return;
        }
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
