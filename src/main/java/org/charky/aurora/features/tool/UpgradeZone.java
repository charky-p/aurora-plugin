package org.charky.aurora.features.tool;

import org.charky.aurora.common.loot.MenuItem;

import java.util.ArrayList;
import java.util.List;
import java.util.function.ToIntFunction;

// What one zone's shop sells: upgrade steps from upgrades.csv, priced by the price
// formula when the data is generated. Each tool has a single chain of steps in the
// zone, bought one at a time in order. Levels bought here belong to the zone's realm.
// before is every zone played on the way here: a tool is only sold here once all of
// its steps there are bought (ToolManager checks it, since it needs those zones)
public record UpgradeZone(String id, String name, String realm, List<String> before, List<Step> steps) {
    // One level of one stat of one tool. For the material the level is its place
    // in the tool's list of materials
    public record Step(String tool, String stat, int level, List<MenuItem> costs) {
    }

    public enum Status {
        // Every step of the chain is bought
        MAXED,
        // The next step can be bought
        OPEN,
        // The tool is behind: an earlier zone's steps come first
        BEHIND
    }

    // The tools this zone upgrades, in the order they first appear
    public List<String> tools() {
        List<String> tools = new ArrayList<>();
        for (Step step : steps) {
            if (!tools.contains(step.tool())) tools.add(step.tool());
        }
        return tools;
    }

    public List<Step> chain(String tool) {
        return steps.stream().filter(step -> step.tool().equals(tool)).toList();
    }

    // The first step of a tool's chain the player has not reached, null when
    // there is none left. current gives the player's level of a stat
    public Step next(String tool, ToIntFunction<String> current) {
        for (Step step : chain(tool)) {
            if (current.applyAsInt(step.stat()) < step.level()) return step;
        }
        return null;
    }

    // A step raises its stat by one, so a tool more than one level short of it
    // skipped something an earlier zone sells. Not the material: a realm's first one
    // follows on from the realms it builds on (Mudflats' Diamond Pickaxe after Origin's
    // Gold), while its own count starts again from nothing
    public Status status(String tool, ToIntFunction<String> current) {
        Step next = next(tool, current);
        if (next == null) return Status.MAXED;
        if (next.stat().equals(ToolLevels.MATERIAL)) return Status.OPEN;
        return current.applyAsInt(next.stat()) < next.level() - 1 ? Status.BEHIND : Status.OPEN;
    }
}
