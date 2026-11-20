package org.charky.aurora.common.util;

import org.bukkit.Material;
import org.bukkit.Tag;

import java.util.Set;

// The kinds of mining tool and the blocks each one mines: what vanilla gives it, plus a
// few blocks no vanilla tool is for that Aurora hands to one, slime to the shovel and
// froglights to the hoe. Tools get these as their rules (ItemFactory) and MineListener
// checks the rules. The tags are only read in methods, they need a running server
public enum MiningTool {
    PICKAXE, AXE, SHOVEL, HOE;

    private static final Set<Material> SHOVEL_EXTRAS = Set.of(Material.SLIME_BLOCK);
    private static final Set<Material> HOE_EXTRAS = Set.of(
            Material.OCHRE_FROGLIGHT, Material.VERDANT_FROGLIGHT, Material.PEARLESCENT_FROGLIGHT);

    // The kind of tool an item is, null for one that mines nothing
    public static MiningTool of(Material item) {
        if (Tag.ITEMS_PICKAXES.isTagged(item)) return PICKAXE;
        if (Tag.ITEMS_AXES.isTagged(item)) return AXE;
        if (Tag.ITEMS_SHOVELS.isTagged(item)) return SHOVEL;
        if (Tag.ITEMS_HOES.isTagged(item)) return HOE;
        return null;
    }

    // The tool a block needs, the pickaxe for anything no tool is for
    public static MiningTool forBlock(Material block) {
        if (SHOVEL_EXTRAS.contains(block)) return SHOVEL;
        if (HOE_EXTRAS.contains(block)) return HOE;
        if (Tag.MINEABLE_AXE.isTagged(block)) return AXE;
        if (Tag.MINEABLE_SHOVEL.isTagged(block)) return SHOVEL;
        if (Tag.MINEABLE_HOE.isTagged(block)) return HOE;
        return PICKAXE;
    }

    // What vanilla has this tool mine
    public Tag<Material> blocks() {
        return switch (this) {
            case PICKAXE -> Tag.MINEABLE_PICKAXE;
            case AXE -> Tag.MINEABLE_AXE;
            case SHOVEL -> Tag.MINEABLE_SHOVEL;
            case HOE -> Tag.MINEABLE_HOE;
        };
    }

    // Blocks it mines on top of the vanilla ones, empty for most
    public Set<Material> extras() {
        return switch (this) {
            case SHOVEL -> SHOVEL_EXTRAS;
            case HOE -> HOE_EXTRAS;
            default -> Set.of();
        };
    }

    // "an axe", "a hoe", for messages
    public String withArticle() {
        String name = name().toLowerCase();
        return (this == AXE ? "an " : "a ") + name;
    }
}
