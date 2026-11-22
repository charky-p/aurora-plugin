package org.charky.aurora.features.core;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.charky.aurora.common.loot.MenuItem;
import org.charky.aurora.common.menu.PagedMenu;
import org.charky.aurora.common.util.TextUtils;
import org.charky.aurora.core.Aurora;
import org.charky.aurora.core.DataRegistry;
import org.charky.aurora.features.player.BackpackData;
import org.charky.aurora.features.tool.UpgradeZone;
import org.charky.aurora.common.menu.CostLine;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// The Overworld Core, sold by the Core Smith in Golem's Rest. One core, upgraded one
// zone at a time in the order the game is played: a step needs its zone maxed and
// costs CORE_RUNS times everything that zone costs, with a few of the Overworld
// Golem's shards. The last step opens the way to the Caverns
public final class OverworldCore {
    public static final String ACCESS = "caverns";
    public static final String SHARD = "golem_shard";
    // How many times over a zone's own costs a core step asks for
    public static final int CORE_RUNS = 25;
    // The zone the Golem lives in sells no core step, its shards go into all of them
    private static final String GOLEM_ZONE = "golems_rest";
    // Saved with the perks, the number of steps done
    public static final String KEY = "overworld_core";

    private OverworldCore() {
    }

    // Every zone in play order, the Golem's own left out
    public static List<String> zones(DataRegistry registry) {
        List<String> zones = new ArrayList<>(registry.getUpgradeZoneIds());
        zones.remove(GOLEM_ZONE);
        return zones;
    }

    // A step's cost: the zone's costs times CORE_RUNS, then 1 shard, rising by one
    // every ten steps
    public static List<MenuItem> cost(DataRegistry registry, int step, String zoneId) {
        UpgradeZone zone = registry.getUpgradeZone(zoneId);
        Map<String, Integer> totals = new LinkedHashMap<>();
        Map<String, MenuItem> items = new LinkedHashMap<>();
        for (UpgradeZone.Step s : zone.steps()) {
            for (MenuItem c : s.costs()) {
                totals.merge(c.getId(), c.getAmount(), Integer::sum);
                items.putIfAbsent(c.getId(), c);
            }
        }
        List<MenuItem> cost = new ArrayList<>();
        totals.forEach((id, amount) -> {
            MenuItem item = items.get(id).copy();
            item.setAmount(amount * CORE_RUNS);
            cost.add(item);
        });
        MenuItem shard = new MenuItem(SHARD, registry.getAllItems().get(SHARD));
        shard.setAmount(1 + step / 10);
        if (shard.getItem() != null) cost.add(shard);
        return cost;
    }

    public static void open(Aurora plugin, Player player) {
        new Menu(plugin).open(player);
    }

    private static final class Menu extends PagedMenu {
        private final Aurora plugin;
        private final List<String> zones;

        Menu(Aurora plugin) {
            super(plugin.getRegistry(), "The Overworld Core");
            this.plugin = plugin;
            this.zones = zones(plugin.getRegistry());
        }

        private int done(Player player) {
            return plugin.getPerkManager().raw(player.getUniqueId(), KEY);
        }

        @Override
        protected List<ItemStack> entries(Player player) {
            int done = done(player);
            BackpackData backpack = plugin.getBackpackManager().get(player.getUniqueId());
            List<ItemStack> items = new ArrayList<>();
            for (int i = 0; i < zones.size(); i++) {
                String zoneId = zones.get(i);
                UpgradeZone zone = registry.getUpgradeZone(zoneId);
                boolean complete = i < done;
                boolean next = i == done;
                ItemStack item = new ItemStack(complete ? Material.HEAVY_CORE : next ? Material.CONDUIT : Material.GRAY_DYE);
                int step = i;
                item.editMeta(meta -> {
                    meta.displayName(TextUtils.parse("<!italic>" + (complete ? "<green>" : next ? "<yellow>" : "<gray>")
                            + "<b>" + (step + 1) + ".</b> " + zone.name()));
                    meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
                    List<Component> lore = new ArrayList<>();
                    if (complete) lore.add(TextUtils.parse("<!italic><green>Infused"));
                    else {
                        boolean maxed = plugin.getUpgradeShop().maxed(player, zoneId);
                        lore.add(TextUtils.parse("<!italic>" + (maxed ? "<green>" : "<red>") + "Needs " + zone.name() + " maxed"));
                        lore.add(Component.empty());
                        for (MenuItem c : cost(registry, step, zoneId)) {
                            long have = backpack == null ? 0 : backpack.getAmount(player, c.getId());
                            lore.add(CostLine.of(c, have));
                        }
                        if (next) {
                            lore.add(Component.empty());
                            lore.add(TextUtils.parse("<!italic><yellow>Click to infuse"));
                        }
                    }
                    meta.lore(lore);
                });
                items.add(item);
            }
            return items;
        }

        @Override
        protected List<Component> info(Player player, List<ItemStack> entries) {
            int done = done(player);
            return List.of(
                    TextUtils.parse("<!italic><gray>Infused: <white>" + done + "<dark_gray>/<gray>" + zones.size()),
                    TextUtils.parse("<!italic><dark_gray>Every zone, in order, then the Caverns open"));
        }

        @Override
        protected void onEntryClick(Player player, int index) {
            int done = done(player);
            if (index != done || index >= zones.size()) return;
            String zoneId = zones.get(index);
            if (!plugin.getUpgradeShop().maxed(player, zoneId)) {
                TextUtils.error(player, "Max " + registry.getUpgradeZone(zoneId).name() + " first");
                return;
            }
            BackpackData backpack = plugin.getBackpackManager().get(player.getUniqueId());
            List<MenuItem> cost = cost(registry, index, zoneId);
            if (backpack == null || !backpack.hasItems(player, cost)) {
                TextUtils.error(player, "Not enough items");
                return;
            }
            backpack.removeItems(player, cost);
            plugin.getPerkManager().setRaw(player.getUniqueId(), KEY, done + 1);
            player.playSound(player.getLocation(), Sound.BLOCK_BEACON_POWER_SELECT, 0.8f, 1.2f);

            if (done + 1 == zones.size()) {
                plugin.getAccessManager().grant(player.getUniqueId(), ACCESS);
                player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);
                player.sendMessage(TextUtils.parse("<white><b>THE OVERWORLD CORE</b> <gray>is whole. The way to the Caverns is open."));
            } else {
                TextUtils.success(player, "Infused " + registry.getUpgradeZone(zoneId).name() + " into the Core");
            }
            render(player);
        }
    }
}
