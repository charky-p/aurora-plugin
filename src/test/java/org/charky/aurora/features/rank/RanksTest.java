package org.charky.aurora.features.rank;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class RanksTest {
    // Each playtime rank is earned at its requirement, and a second short of it is still the one before
    @Test
    void playtimeEarnsTheHighestRankReached() {
        List<Rank> ladder = Ranks.ofKind(Rank.Kind.PLAYTIME);
        assertFalse(ladder.isEmpty());
        assertNull(Ranks.earnedBy(0));
        for (int i = 0; i < ladder.size(); i++) {
            Rank rank = ladder.get(i);
            assertSame(rank, Ranks.earnedBy(rank.requirement()), rank.id());
            assertSame(i == 0 ? null : ladder.get(i - 1), Ranks.earnedBy(rank.requirement() - 1), rank.id());
        }
        assertSame(ladder.getLast(), Ranks.earnedBy(ladder.getLast().requirement() * 10));
    }

    // A rank saved on a profile that has since been renamed or removed
    @Test
    void anUnknownRankFallsBackToMember() {
        assertSame(Ranks.MEMBER, Ranks.get("WIZARD"));
        assertSame(Ranks.MEMBER, Ranks.get(null));
        assertEquals("AURORA", Ranks.get("aurora").id());
    }
}
