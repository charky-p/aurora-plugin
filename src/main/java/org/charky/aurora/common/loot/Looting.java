package org.charky.aurora.common.loot;

// Looting takes a drop's min-max range and returns a new one, shifting the whole
// range up by one for every two levels, rounded up, so 1-3 becomes 2-4 at Looting 1
// and 2, and 4-6 at Looting 5. It was one a level until the first playtest showed it
// swamping the drops. Only stackable resources are boosted, never gear or keys
public final class Looting {
    private Looting() {
    }

    public record Range(int min, int max) {
    }

    public static Range apply(int min, int max, int level) {
        if (level <= 0) return new Range(min, max);

        int bonus = bonus(level);
        return new Range(min + bonus, max + bonus);
    }

    // The seam for scaling big levels differently later, a single place to change
    private static int bonus(int level) {
        return (level + 1) / 2;
    }
}
