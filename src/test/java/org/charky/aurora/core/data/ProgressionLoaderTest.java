package org.charky.aurora.core.data;

import org.charky.aurora.core.models.RealmModel;
import org.charky.aurora.core.models.ToolModel;
import org.charky.aurora.core.models.UpgradeModel;
import org.charky.aurora.features.tool.UpgradeZone;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProgressionLoaderTest {
    private static ToolModel tool(String name, String... materials) {
        ToolModel model = new ToolModel();
        model.name = name;
        model.kind = "mining";
        model.item = "pickaxe";
        model.materials = List.of(materials);
        return model;
    }

    private static UpgradeModel.StepModel step(String tool, String stat, String level) {
        UpgradeModel.StepModel step = new UpgradeModel.StepModel();
        step.tool = tool;
        step.stat = stat;
        step.level = level;
        return step;
    }

    private static UpgradeModel zone(String region, boolean released, List<String> before, UpgradeModel.StepModel... steps) {
        UpgradeModel model = new UpgradeModel();
        model.name = "Zone";
        model.realm = "Overworld";
        model.region = region;
        model.released = released;
        model.before = before;
        model.steps = List.of(steps);
        return model;
    }

    private static ProgressionLoader.Progression load(Map<String, UpgradeModel> zones) {
        RealmModel realm = new RealmModel();
        realm.name = "Overworld";
        return new ProgressionLoader(Logger.getAnonymousLogger()).load(
                Map.of("overworld", realm),
                Map.of("pick", tool("Pickaxe", "WOODEN", "STONE", "IRON"), "broken", tool("Broken")),
                zones, Map.of(), Set.of("efficiency")::contains);
    }

    @Test
    void stepsBecomeLevels() {
        ProgressionLoader.Progression loaded = load(Map.of("cobble", zone("origin_isles", true, List.of(),
                step("pick", "material", "IRON"),
                step("pick", "efficiency", "2"),
                step("pick", "unlock", "1"))));

        UpgradeZone cobble = loaded.zones().get("cobble");
        assertEquals("overworld", cobble.realm());
        assertEquals(List.of(2, 2, 1), cobble.steps().stream().map(UpgradeZone.Step::level).toList());
        assertEquals(List.of("material", "efficiency", "unlock"), cobble.steps().stream().map(UpgradeZone.Step::stat).toList());
    }

    // A tool with no materials is no tool, and a step that names nothing real is left out
    @Test
    void brokenDataIsSkipped() {
        ProgressionLoader.Progression loaded = load(Map.of("coal", zone("origin_isles", true, List.of(),
                step("broken", "efficiency", "1"),
                step("pick", "sharpness", "1"),
                step("pick", "material", "GOLDEN"),
                step("pick", "efficiency", "lots"),
                step("pick", "efficiency", "3"))));

        assertTrue(!loaded.tools().containsKey("broken"));
        assertEquals(1, loaded.zones().get("coal").steps().size());
    }

    @Test
    void regionsAndReleasesAreKept() {
        ProgressionLoader.Progression loaded = load(Map.of(
                "City", zone("Gormak", true, List.of("COBBLE")),
                "canyons", zone("khorvath", false, List.of("city"))));

        assertEquals("gormak", loaded.zoneRegions().get("city"));
        assertEquals(List.of("cobble"), loaded.zones().get("city").before());
        assertEquals(Set.of("gormak"), loaded.releasedRegions());
    }
}
