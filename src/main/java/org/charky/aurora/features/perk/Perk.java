package org.charky.aurora.features.perk;

import org.bukkit.Material;

// Permanent upgrades bought with Essence in /shop, one level at a time. Each level
// costs more than the last: level n costs costStep * n. Only utility is sold: dash, double jump,
// mounts, the God Tool and the Keyring. Pure numbers, tested directly
public enum Perk {
    // Not sold any more, they change how fast the game goes. Levels bought still count
    // Half a heart a level, 20 levels doubles the player's health
    VITALITY("vitality", "<red><b>Vitality", Material.GOLDEN_APPLE, 20, 50,
            "+half a heart of health per level", false),
    // A crit doubles what a mined block drops, 100 levels makes every block a crit
    CRIT_RATE("crit_rate", "<yellow><b>Crit Rate", Material.DIAMOND_PICKAXE, 100, 10,
            "+1% chance per level for a mined block to drop double", false),
    // Utility, bought once each. None of them makes a zone faster to finish
    // Keys found before can then be taken from Collections again
    KEYRING("keyring", "<gold><b>Keyring", Material.TRIAL_KEY, 1, 5_000,
            "take any key you have found before from Collections", true),
    DASH("dash", "<aqua><b>Dash", Material.FEATHER, 1, 2_500,
            "right click the Dash Feather to dash forward, click again for the feather", true),
    DOUBLE_JUMP("double_jump", "<green><b>Double Jump", Material.RABBIT_FOOT, 1, 4_000,
            "jump again in mid-air for a second jump, once until you land", true),
    GOD_TOOL("god_tool", "<light_purple><b>God Tool", Material.NETHERITE_PICKAXE, 1, 10_000,
            "one tool that mines like your pick, axe and shovel, each block with that tool's levels", true),
    // Mounts have three levels, each one faster: level n costs costStep * n
    MOUNT_PIG("mount_pig", "<light_purple><b>Pig Mount", Material.CARROT_ON_A_STICK, 3, 1_000,
            "a pig to ride, called with its saddle. Each level is faster", true),
    MOUNT_CHICKEN("mount_chicken", "<white><b>Chicken Mount", Material.FEATHER, 3, 1_500,
            "a chicken to ride that floats down from heights. Each level is faster", true),
    MOUNT_HORSE("mount_horse", "<gold><b>Horse Mount", Material.SADDLE, 3, 3_000,
            "a fast horse to ride. Each level runs and jumps further, level 3 swims", true),
    MOUNT_CAMEL("mount_camel", "<yellow><b>Camel Mount", Material.CACTUS, 3, 4_000,
            "a camel that dashes when you jump. Each level is faster", true),
    MOUNT_SKELETON("mount_skeleton", "<gray><b>Skeleton Horse Mount", Material.BONE, 3, 6_000,
            "a skeleton horse faster than any horse, that rides underwater. Each level is faster", true),
    MOUNT_GHAST("mount_ghast", "<white><b>Happy Ghast Mount", Material.WHITE_HARNESS, 3, 8_000,
            "a happy ghast that flies where you look. Each level flies faster", true),
    MOUNT_PHANTOM("mount_phantom", "<dark_aqua><b>Phantom Mount", Material.PHANTOM_MEMBRANE, 3, 12_000,
            "the fastest flyer: goes where you look while you hold forward. Each level is faster", true);

    private final String id;
    private final String display;
    private final Material icon;
    private final int maxLevel;
    private final int costStep;
    private final String effect;
    private final boolean sold;

    Perk(String id, String display, Material icon, int maxLevel, int costStep, String effect, boolean sold) {
        this.id = id;
        this.display = display;
        this.icon = icon;
        this.maxLevel = maxLevel;
        this.costStep = costStep;
        this.effect = effect;
        this.sold = sold;
    }

    // What buying this level costs, coming from the level below it
    public int cost(int level) {
        return costStep * level;
    }

    // Everything spent to stand at this level
    public int totalCost(int level) {
        return costStep * level * (level + 1) / 2;
    }

    // Null for an id saved by a perk that no longer exists
    public static Perk byId(String id) {
        for (Perk perk : values()) {
            if (perk.id.equalsIgnoreCase(id)) return perk;
        }
        return null;
    }

    public String id() { return id; }
    public String display() { return display; }
    public Material icon() { return icon; }
    public int maxLevel() { return maxLevel; }
    public String effect() { return effect; }
    public boolean sold() { return sold; }

    // The mount a perk gives, null for every other perk
    public String mount() {
        return id.startsWith("mount_") ? id.substring("mount_".length()) : null;
    }
}
