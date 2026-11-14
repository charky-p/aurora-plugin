package org.charky.aurora.features.perk;

import org.bukkit.Material;

// Permanent upgrades bought with Essence in /shop, one level at a time. Each level
// costs more than the last: level n costs costStep * n. Pure numbers, tested directly
public enum Perk {
    // Half a heart a level, 20 levels doubles the player's health
    VITALITY("vitality", "<red><b>Vitality", Material.GOLDEN_APPLE, 20, 50,
            "+half a heart of health per level"),
    // A crit doubles what a mined block drops, 100 levels makes every block a crit
    CRIT_RATE("crit_rate", "<yellow><b>Crit Rate", Material.DIAMOND_PICKAXE, 100, 10,
            "+1% chance per level for a mined block to drop double"),
    // Bought once. Keys found before can then be taken from the Armory again
    KEYRING("keyring", "<gold><b>Keyring", Material.TRIAL_KEY, 1, 5_000,
            "take any key you have found before from the Armory");

    private final String id;
    private final String display;
    private final Material icon;
    private final int maxLevel;
    private final int costStep;
    private final String effect;

    Perk(String id, String display, Material icon, int maxLevel, int costStep, String effect) {
        this.id = id;
        this.display = display;
        this.icon = icon;
        this.maxLevel = maxLevel;
        this.costStep = costStep;
        this.effect = effect;
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
}
