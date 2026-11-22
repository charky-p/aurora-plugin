package org.charky.aurora.features.gauntlet;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.charky.aurora.common.menu.PagedMenu;
import org.charky.aurora.common.util.TextUtils;
import org.charky.aurora.features.spawner.MobData;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// Every mob of the tower in the order it first comes in, "???" until the player has
// killed one: then the floor it first comes in on, its health and attack there, a boss's
// abilities, and how many they have killed
public class BestiaryMenu extends PagedMenu {
    private final GauntletManager manager;

    public BestiaryMenu(GauntletManager manager) {
        super(manager.registry(), "Bestiary");
        this.manager = manager;
    }

    @Override
    protected List<ItemStack> entries(Player player) {
        GauntletProfile profile = manager.profile(player);
        List<ItemStack> items = new ArrayList<>();
        firstFloors(manager.data()).forEach((id, floor) -> items.add(entry(profile, id, floor)));
        return items;
    }

    // Every mob of the floors, with the floor it first comes in on, in that order
    static Map<String, Integer> firstFloors(GauntletData data) {
        Map<String, Integer> first = new LinkedHashMap<>();
        List<GauntletData.Floor> floors = data.floors();
        for (int i = 0; i < floors.size(); i++) {
            for (String id : floors.get(i).mobs().keySet()) first.putIfAbsent(id, i + 1);
        }
        return first;
    }

    // How many of the tower's mobs the player has killed at least once
    static int met(GauntletData data, GauntletProfile profile) {
        return (int) firstFloors(data).keySet().stream().filter(id -> profile.stat(GauntletRuns.SLAIN + id) > 0).count();
    }

    private ItemStack entry(GauntletProfile profile, String id, int floor) {
        GauntletData data = manager.data();
        boolean boss = data.floors().get(floor - 1).isBoss(id);
        int slain = profile.stat(GauntletRuns.SLAIN + id);
        MobData there = manager.registry().getMob(new GauntletData.Variant(id, floor, false, 0, null).id());
        if (slain == 0 || there == null) {
            return button(Material.GRAY_DYE, "<dark_gray><b>???", List.of(
                    "<gray>Comes in on floor <white>" + floor + (boss ? " <dark_red>as its boss" : ""),
                    "<dark_gray>Kill one to learn more"));
        }
        List<String> lines = new ArrayList<>();
        lines.add("<gray>Comes in on floor <white>" + floor + (boss ? " <dark_red>as its boss" : ""));
        lines.add("<gray>There: <red>" + Math.round(there.health()) + " health <gray>and <white>" + Math.round(there.attack()) + " attack");
        List<GauntletData.BossAbility> abilities = data.bosses().getOrDefault(id, List.of());
        if (!abilities.isEmpty()) {
            lines.add("<gray>Abilities: <gold>" + String.join(", ", abilities.stream().map(GauntletData.BossAbility::type).toList()));
        }
        lines.add("");
        lines.add("<gray>You have killed <white>" + slain);
        Material egg = Material.matchMaterial(there.type().name() + "_SPAWN_EGG");
        ItemStack item = new ItemStack(egg == null ? Material.SKELETON_SKULL : egg);
        item.editMeta(meta -> {
            meta.displayName(Component.empty().decoration(TextDecoration.ITALIC, false).append(there.displayName()));
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
            meta.lore(lore(lines));
        });
        return item;
    }

    @Override
    protected List<Component> info(Player player, List<ItemStack> entries) {
        GauntletData data = manager.data();
        return List.of(TextUtils.parse("<!italic><gray>The tower's mobs, in the order"),
                TextUtils.parse("<!italic><gray>they come in. Met: <white>" + met(data, manager.profile(player)) + "/"
                        + firstFloors(data).size()));
    }

    @Override
    protected void back(Player player) {
        manager.openMenu(player);
    }
}
