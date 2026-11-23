package org.charky.aurora.features.gauntlet;

// What a hit between players does in the Gauntlet's PvP zone and duels, on vanilla's
// scale (20 health is a whole player) whatever the gear. pvp_base_damage is a full sword
// hit between equal levels, times the weapon's share (gear.csv damage: an axe or a
// crossbow hits harder, a mace softer), how good the hit was (the swing's charge, a jump
// crit, a bow's draw, a mace's smash up to pvp_smash_max, Strength) and the levels:
//
//   (1 + effect x the weapon's level) / (1 + effect x the armour's level)
//
// At effect 3% and a base of 2: equal levels take 10 full hits, level 25 against level
// 30 is near even (11 hits against 10), level 1 against level 50 loses but lasts (25 hits
// against 5). Mobs are not hit this way
public final class GauntletPvpFormula {
    // gear_levels.csv: 2 Sharpness, Power and Protection a gear level
    static final double STAT_PER_LEVEL = 2;
    // A weapon that is not gauntlet gear (a fist, the grapple) hits like a quarter of a sword
    static final double BARE_SHARE = 0.25;
    // A sweep's splash onto the players beside the one struck, a share of a hit
    static final double SWEEP_SHARE = 0.25;

    private GauntletPvpFormula() {
    }

    // The weapon's level against the armour's, each level effect more
    public static double levelFactor(double weaponLevel, double armourLevel, double effect) {
        return (1 + effect * Math.max(0, weaponLevel)) / (1 + effect * Math.max(0, armourLevel));
    }

    // The hit, in health
    public static double damage(double base, double share, double quality, double weaponLevel, double armourLevel, double effect) {
        return base * share * quality * levelFactor(weaponLevel, armourLevel, effect);
    }

    // How many full sword hits a player of 20 health takes, to check the balance
    static int hitsToKill(double base, double weaponLevel, double armourLevel, double effect) {
        return (int) Math.ceil(20 / damage(base, 1, 1, weaponLevel, armourLevel, effect));
    }
}
