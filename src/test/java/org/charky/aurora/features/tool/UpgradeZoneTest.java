package org.charky.aurora.features.tool;

import org.charky.aurora.features.tool.UpgradeZone.Status;
import org.charky.aurora.features.tool.UpgradeZone.Step;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class UpgradeZoneTest {
    private static Step step(String tool, String stat, int level) {
        return new Step(tool, stat, level, List.of());
    }

    // The Coal Seam: the pick goes Efficiency 3, Luck 2, Efficiency 4, the sword Sharpness 2
    private static final UpgradeZone COAL = new UpgradeZone("coal", "Coal Seam", "overworld", List.of("cobble"), List.of(
            step("pick", "efficiency", 3),
            step("sword", "sharpness", 2),
            step("pick", "luck", 2),
            step("pick", "efficiency", 4)));

    private static Map<String, Integer> levels(Object... pairs) {
        Map<String, Integer> levels = new HashMap<>();
        for (int i = 0; i < pairs.length; i += 2) levels.put((String) pairs[i], (Integer) pairs[i + 1]);
        return levels;
    }

    private static Step next(String tool, Map<String, Integer> levels) {
        return COAL.next(tool, stat -> levels.getOrDefault(stat, 0));
    }

    private static Status status(String tool, Map<String, Integer> levels) {
        return COAL.status(tool, stat -> levels.getOrDefault(stat, 0));
    }

    @Test
    void everyToolHasItsOwnChain() {
        assertEquals(List.of("pick", "sword"), COAL.tools());
        assertEquals(3, COAL.chain("pick").size());
        assertEquals(1, COAL.chain("sword").size());
        assertEquals(0, COAL.chain("axe").size());
    }

    @Test
    void stepsAreBoughtInOrder() {
        Map<String, Integer> pick = levels("efficiency", 2, "luck", 1);
        assertEquals(step("pick", "efficiency", 3), next("pick", pick));

        pick.put("efficiency", 3);
        assertEquals(step("pick", "luck", 2), next("pick", pick));

        pick.put("luck", 2);
        assertEquals(step("pick", "efficiency", 4), next("pick", pick));
    }

    @Test
    void aFinishedChainIsMaxed() {
        Map<String, Integer> pick = levels("efficiency", 4, "luck", 2);

        assertNull(next("pick", pick));
        assertEquals(Status.MAXED, status("pick", pick));
        // Levels from a later zone count as done too
        assertEquals(Status.MAXED, status("pick", levels("efficiency", 9, "luck", 9)));
    }

    @Test
    void aToolOneLevelShortCanBuyTheStep() {
        assertEquals(Status.OPEN, status("pick", levels("efficiency", 2, "luck", 1)));
        assertEquals(Status.OPEN, status("sword", levels("sharpness", 1)));
    }

    // Whether an earlier zone is unfinished is ToolManager's to say, with the zone's
    // name. A gap in levels alone is no reason: steps can jump
    @Test
    void aZoneAloneNeverSaysBehind() {
        assertEquals(Status.OPEN, status("pick", levels()));
        assertEquals(Status.OPEN, status("sword", levels()));
    }

    // Mudflats starts the marsh realm with a Diamond Pickaxe (material 5): the realm's own
    // count is 0, but the pick is Gold from Origin, so it is the next step, not a skip
    @Test
    void aRealmsFirstMaterialIsOpen() {
        UpgradeZone mudflats = new UpgradeZone("mudflats", "Mudflats", "marsh", List.of("darkoak"), List.of(
                step("pick", ToolLevels.MATERIAL, 5),
                step("pick", "efficiency", 1)));
        assertEquals(Status.OPEN, mudflats.status("pick", stat -> 0));
    }
}
