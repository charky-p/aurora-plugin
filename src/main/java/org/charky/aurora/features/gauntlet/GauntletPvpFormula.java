package org.charky.aurora.features.gauntlet;

import java.util.List;
import java.util.Map;

// What a hit does in the Gauntlet's PvP zone and duels: vanilla's own hit first (the
// weapon's damage and swing speed, crits, sweeps, Strength, a mace's smash, a raised
// shield, an arrow's draw), then vanilla's armour for the material each Gauntlet piece
// is shown as (Gauntlet armour carries no vanilla armour points, Aurora prices mob hits
// itself), then the Gauntlet's levels:
//
//   hit x (1 + effect x the weapon's level) / (1 + effect x the armour's level)
//
// At equal levels a fight is vanilla's; every level ahead hits harder, behind softer
public final class GauntletPvpFormula {
    // gear_levels.csv: 2 Sharpness, Power and Protection a gear level
    static final double STAT_PER_LEVEL = 2;

    // Vanilla's armour points a piece (helmet, chestplate, leggings, boots) and toughness
    private static final Map<String, int[]> POINTS = Map.of(
            "LEATHER", new int[]{1, 3, 2, 1},
            "CHAINMAIL", new int[]{2, 5, 4, 1},
            "COPPER", new int[]{2, 4, 3, 1},
            "IRON", new int[]{2, 6, 5, 2},
            "GOLDEN", new int[]{2, 5, 3, 1},
            "DIAMOND", new int[]{3, 8, 6, 3},
            "NETHERITE", new int[]{3, 8, 6, 3});
    private static final Map<String, Integer> TOUGHNESS = Map.of("DIAMOND", 2, "NETHERITE", 3);
    private static final List<String> PIECES = List.of("HELMET", "CHESTPLATE", "LEGGINGS", "BOOTS");

    private GauntletPvpFormula() {
    }

    // The Gauntlet's part: the weapon's level against the armour's, each level effect more
    public static double levelFactor(double weaponLevel, double armourLevel, double effect) {
        return (1 + effect * Math.max(0, weaponLevel)) / (1 + effect * Math.max(0, armourLevel));
    }

    // Vanilla's armour: points take off 4% each, less against big hits unless there is
    // toughness, at most 80%
    public static double armourMultiplier(double damage, double points, double toughness) {
        double reduced = Math.min(20, Math.max(points / 5, points - 4 * damage / (toughness + 8)));
        return 1 - reduced / 25;
    }

    // The armour points and toughness of pieces worn, by their materials (DIAMOND_HELMET)
    public static double[] armour(List<String> materials) {
        double points = 0, toughness = 0;
        for (String material : materials) {
            int split = material.lastIndexOf('_');
            if (split < 0) continue;
            int[] tier = POINTS.get(material.substring(0, split));
            int piece = PIECES.indexOf(material.substring(split + 1));
            if (tier == null || piece < 0) continue;
            points += tier[piece];
            toughness += TOUGHNESS.getOrDefault(material.substring(0, split), 0);
        }
        return new double[]{points, toughness};
    }
}
