package org.charky.aurora.features.player;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LevelsTest {
    // XP from level 0 up to a level
    private static long totalXp(int level) {
        long total = 0;
        for (int i = 0; i < level; i++) total += Levels.xpForNext(i);
        return total;
    }

    @Test
    void theFirstLevelNeedsOneXpAndNoLevelNeedsLessThanTheLast() {
        assertEquals(1, Levels.xpForNext(0));
        for (int level = 0; level < 200; level++) {
            assertTrue(Levels.xpForNext(level + 1) >= Levels.xpForNext(level));
        }
    }

    // The pace that was chosen. A new player mines about one cobblestone every two
    // seconds, one XP each, and is at two or three XP a second by the Coal Seam:
    // levels 1 to 5 in about ten seconds, 5 to 10 in under a minute, 10 to 15 in
    // about two minutes
    @Test
    void theFirstLevelsFlyBy() {
        assertTrue(totalXp(5) <= 10);
        long toTen = totalXp(10) - totalXp(5);
        assertTrue(toTen >= 20 && toTen <= 60);
        long toFifteen = totalXp(15) - totalXp(10);
        assertTrue(toFifteen >= 150 && toFifteen <= 300);
    }

    // After that every level needs a tenth more than the last. A full Origin and
    // Gormak run is somewhere between 50,000 and 100,000 XP and ends past level 50,
    // with level 100 many runs further
    @Test
    void lateLevelsGrowByATenth() {
        assertEquals(Levels.LATE_GROWTH, (double) Levels.xpForNext(81) / Levels.xpForNext(80), 0.01);
        long fifty = totalXp(50);
        assertTrue(fifty >= 15_000 && fifty <= 40_000);
        assertTrue(totalXp(100) > fifty * 50);
    }

    @Test
    void xpBelowTheNextLevelOnlyFillsTheBar() {
        long needed = Levels.xpForNext(10);
        Levels.Gain gain = Levels.add(10, 1, needed - 2);

        assertEquals(10, gain.level());
        assertEquals(needed - 1, gain.xp());
        assertEquals(0, gain.levelsGained());
        assertEquals(0, gain.essence());
    }

    @Test
    void reachingTheNextLevelKeepsWhatIsLeftOver() {
        long needed = Levels.xpForNext(10);
        Levels.Gain gain = Levels.add(10, 1, needed + 1);

        assertEquals(11, gain.level());
        assertEquals(2, gain.xp());
        assertEquals(1, gain.levelsGained());
        assertEquals(Levels.essenceFor(11), gain.essence());
    }

    // A boss kill can be worth several levels at once
    @Test
    void oneBigGainCanCoverSeveralLevels() {
        Levels.Gain gain = Levels.add(0, 0, totalXp(3) + 1);

        assertEquals(3, gain.level());
        assertEquals(1, gain.xp());
        assertEquals(3, gain.levelsGained());
        assertEquals(Levels.essenceFor(1) + Levels.essenceFor(2) + Levels.essenceFor(3), gain.essence());
    }

    // Higher levels never pay less, a milestone pays its multiplier times what the level
    // after it does, every hundredth the flat monument, far more than the milestone before
    @Test
    void higherLevelsPayMoreAndMilestonesManyTimesOver() {
        int previous = 0;
        for (int level = 1; level <= 250; level++) {
            int essence = Levels.essenceFor(level);
            if (level % Levels.MONUMENT_EVERY == 0) {
                assertEquals(Levels.MONUMENT_ESSENCE, essence, "level " + level);
                assertTrue(essence > Levels.essenceFor(level - Levels.MILESTONE_EVERY), "level " + level);
            } else if (Levels.isMilestone(level)) {
                assertEquals(Levels.MILESTONE_MULTIPLIER * Levels.essenceFor(level + 1), essence, "level " + level);
            } else {
                assertTrue(essence >= previous, "level " + level);
                previous = essence;
            }
        }
        assertFalse(Levels.isMilestone(Levels.MILESTONE_EVERY + 1));
    }

    // Levels alone should buy a perk or a few cosmetics by the end of Gormak, never the best
    @Test
    void levelsToFiftyPayAboutSevenHundredEssence() {
        int essence = 0;
        for (int level = 1; level <= 50; level++) essence += Levels.essenceFor(level);

        assertTrue(essence >= 650 && essence <= 750, "got " + essence);
    }

    @Test
    void theBarNeverReadsFull() {
        long needed = Levels.xpForNext(10);
        assertEquals(0f, Levels.progress(4, 0));
        assertEquals(0.5f, Levels.progress(10, needed / 2), 0.05f);
        assertTrue(Levels.progress(10, needed) < 1f);
    }
}