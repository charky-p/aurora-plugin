package org.charky.aurora.features.tool;

import java.util.List;

// A kind of tool from tools.csv: a pick, an axe, a sword, a piece of armour. The
// item is the vanilla name after the material, so IRON and PICKAXE make
// IRON_PICKAXE. Materials are in the order they are unlocked
public record ToolType(String id, String name, String kind, String item, List<String> materials) {
}
