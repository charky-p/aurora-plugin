package org.charky.aurora.features.tool;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// A tool's stats are levels kept on the player, one set per realm, never on the
// item. In a realm that builds on another, the two sets are combined: enchant
// levels add up, so Winter Fortune 2 on top of Fortune 5 mines as Fortune 7 there,
// and the material is whichever is further along. Pure numbers, tested directly
public final class ToolLevels {
    // Not an enchant: how far along the tool's list of materials it is
    public static final String MATERIAL = "material";

    private ToolLevels() {
    }

    // Own levels first, then every realm they build on
    public static Map<String, Integer> combine(List<Map<String, Integer>> chain) {
        Map<String, Integer> result = new LinkedHashMap<>();
        for (Map<String, Integer> levels : chain) {
            levels.forEach((stat, level) -> result.merge(stat, level,
                    stat.equals(MATERIAL) ? Math::max : Integer::sum));
        }
        return result;
    }

    // The material a level stands for, the first for a new tool and the last
    // for a level past the end of the list
    public static String material(List<String> materials, int level) {
        return materials.get(Math.clamp(level, 0, materials.size() - 1));
    }

    // -1 for a material this tool does not come in
    public static int materialLevel(List<String> materials, String name) {
        for (int i = 0; i < materials.size(); i++) {
            if (materials.get(i).equalsIgnoreCase(name)) return i;
        }
        return -1;
    }
}
