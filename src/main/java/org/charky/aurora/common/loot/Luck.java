package org.charky.aurora.common.loot;

// Luck on the weapon makes gear drop more often: every level adds a tenth of the
// drop's own chance, so a 2% blade is 4% at Luck 10 and 5% at Luck 15. Only gear,
// never resources, which always drop, or keys
public final class Luck {
    private static final double BONUS_PER_LEVEL = 0.10;
    private static final double CERTAIN = 100.0;

    private Luck() {
    }

    // Both chances are percentages
    public static double chance(double base, int luck) {
        if (luck <= 0) return base;
        return Math.min(CERTAIN, base * (1 + BONUS_PER_LEVEL * luck));
    }
}
