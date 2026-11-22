package org.charky.aurora.features.gauntlet;

import org.charky.aurora.features.combat.CombatFormulas;

// What a hit does in the Gauntlet's PvP zone, in hearts, before swing charge, crits
// and Strength. Gauntlet gear carries Aurora stats, and vanilla damage ignores most of
// them (armour has no vanilla armour points), so players hit each other by these:
//
//   hearts = BASE_HEARTS x the gap between the weapon's Sharpness (or Power) and the
//            victim's Defence, 3% a level, held between MIN and MAX
//          x what half of the victim's Protection leaves (CombatFormulas)
//
// Defence counts half, a set of four pieces adds up to twice the level each piece
// is, so equal gear meets at a gap of 0: about 6 to 8 full hits to kill at any level
public final class GauntletPvpFormula {
    static final double BASE_HEARTS = 2.0;
    static final double PER_LEVEL = 0.03;
    static final double MIN_GAP = 0.4;
    static final double MAX_GAP = 2.5;
    static final double PROTECTION_SHARE = 0.5;
    public static final double CRIT = 1.5;

    private GauntletPvpFormula() {
    }

    // armour is the Defence of every piece worn added up, shield the best shield's
    public static int defence(int armour, int shield) {
        return (armour + shield) / 2;
    }

    public static double hearts(int attack, int defence, double protection) {
        double gap = Math.clamp(1 + PER_LEVEL * (attack - defence), MIN_GAP, MAX_GAP);
        double left = 1 - PROTECTION_SHARE * (1 - CombatFormulas.protectionMultiplier(protection));
        return BASE_HEARTS * gap * left;
    }
}
