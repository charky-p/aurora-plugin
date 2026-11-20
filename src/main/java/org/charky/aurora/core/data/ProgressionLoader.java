package org.charky.aurora.core.data;

import org.bukkit.inventory.ItemStack;
import org.charky.aurora.common.loot.MenuItem;
import org.charky.aurora.core.models.GuiModel;
import org.charky.aurora.core.models.RealmModel;
import org.charky.aurora.core.models.ToolModel;
import org.charky.aurora.core.models.UpgradeModel;
import org.charky.aurora.features.tool.Realms;
import org.charky.aurora.features.tool.ToolLevels;
import org.charky.aurora.features.tool.ToolType;
import org.charky.aurora.features.tool.UpgradeZone;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.logging.Logger;

// Builds how players progress: the realms, the tools that live on them and every
// zone's upgrades, with the regions those zones belong to and which are released.
// A step for a tool, material or enchant that does not exist is skipped and logged
public class ProgressionLoader {
    public record Progression(Realms realms, Map<String, ToolType> tools, Map<String, UpgradeZone> zones,
                              Map<String, String> zoneRegions, Set<String> releasedRegions) {
    }

    private final Logger logger;

    public ProgressionLoader(Logger logger) {
        this.logger = logger;
    }

    // items prices the steps, isEnchant tells an enchant from a stat that does not exist
    public Progression load(Map<String, RealmModel> realmData, Map<String, ToolModel> toolData,
                            Map<String, UpgradeModel> upgradeData, Map<String, ItemStack> items,
                            Predicate<String> isEnchant) {
        List<Realms.Realm> realms = new ArrayList<>();
        realmData.forEach((id, model) ->
                realms.add(new Realms.Realm(id.toLowerCase(), model.name, List.copyOf(model.worlds), model.base, model.part_of)));

        Map<String, ToolType> tools = new LinkedHashMap<>();
        toolData.forEach((id, model) -> {
            if (model.materials.isEmpty() || model.item == null) {
                logger.severe("TOOL ERROR: '" + id + "' needs an item and at least one material");
                return;
            }
            tools.put(id.toLowerCase(), new ToolType(id.toLowerCase(), model.name, model.kind,
                    model.item.toUpperCase(), List.copyOf(model.materials)));
        });

        Map<String, UpgradeZone> zones = new LinkedHashMap<>();
        Map<String, String> zoneRegions = new LinkedHashMap<>();
        Set<String> released = new HashSet<>();
        upgradeData.forEach((id, model) -> {
            String zoneId = id.toLowerCase();
            List<UpgradeZone.Step> steps = new ArrayList<>();
            for (UpgradeModel.StepModel step : model.steps) {
                ToolType tool = step.tool == null ? null : tools.get(step.tool.toLowerCase());
                int level = tool == null ? -1 : level(tool, step.stat, step.level, isEnchant);
                if (level < 0) {
                    logger.severe("UPGRADE ERROR: '" + id + "' " + step.tool + " " + step.stat + " " + step.level);
                    continue;
                }
                steps.add(new UpgradeZone.Step(tool.id(), step.stat.toLowerCase(), level, costs(id, step, items)));
            }
            List<String> before = model.before == null ? List.of() : model.before.stream().map(String::toLowerCase).toList();
            zones.put(zoneId, new UpgradeZone(zoneId, model.name, model.realm == null ? "" : model.realm.toLowerCase(), before, steps));
            String region = model.region == null ? "" : model.region.toLowerCase();
            zoneRegions.put(zoneId, region);
            if (model.released) released.add(region);
        });
        return new Progression(new Realms(realms), tools, zones, zoneRegions, released);
    }

    private List<MenuItem> costs(String zone, UpgradeModel.StepModel step, Map<String, ItemStack> items) {
        List<MenuItem> costs = new ArrayList<>();
        for (GuiModel.CostEntry entry : step.cost) {
            ItemStack item = items.get(entry.id.toLowerCase());
            if (item == null) {
                logger.severe("UPGRADE ERROR: '" + zone + "' costs unknown item '" + entry.id + "'");
                continue;
            }
            MenuItem cost = new MenuItem(entry.id.toLowerCase(), item);
            cost.setAmount(entry.amount);
            costs.add(cost);
        }
        return costs;
    }

    // -1 for a material the tool does not come in, an unknown enchant or a bad number
    private static int level(ToolType tool, String stat, String level, Predicate<String> isEnchant) {
        if (stat == null || level == null) return -1;
        if (stat.equalsIgnoreCase(ToolLevels.MATERIAL)) return ToolLevels.materialLevel(tool.materials(), level);
        if (stat.equalsIgnoreCase(ToolLevels.UNLOCK)) return 1;
        if (!isEnchant.test(stat.toLowerCase())) return -1;
        try {
            return Integer.parseInt(level);
        } catch (NumberFormatException e) {
            return -1;
        }
    }
}
