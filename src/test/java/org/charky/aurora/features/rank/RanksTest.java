package org.charky.aurora.features.rank;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class RanksTest {
    private static final long HOUR = 3600;

    @Test
    void playtimeEarnsTheHighestRankReached() {
        assertNull(Ranks.earnedBy(0));
        assertNull(Ranks.earnedBy(HOUR - 1));
        assertEquals("PLAYER", Ranks.earnedBy(HOUR).id());
        assertEquals("NOVICE", Ranks.earnedBy(49 * HOUR).id());
        assertEquals("GRANDMASTER", Ranks.earnedBy(600 * HOUR).id());
        assertEquals("ETERNAL", Ranks.earnedBy(9_000 * HOUR).id());
    }

    // A rank saved on a profile that has since been renamed or removed
    @Test
    void anUnknownRankFallsBackToMember() {
        assertSame(Ranks.MEMBER, Ranks.get("WIZARD"));
        assertSame(Ranks.MEMBER, Ranks.get(null));
        assertEquals("AURORA", Ranks.get("aurora").id());
    }
}
