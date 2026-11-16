package org.charky.aurora.features.rank;

import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RanksTest {
    private static final long HOUR = 3600;

    private static List<String> ids(Rank.Kind kind) {
        return Ranks.ofKind(kind).stream().map(Rank::id).toList();
    }

    @Test
    void everyoneStartsAsMember() {
        assertEquals(List.of("MEMBER"), ids(Rank.Kind.DEFAULT));
        assertSame(Ranks.MEMBER, Ranks.get("MEMBER"));
    }

    @Test
    void thePlaytimeLadderIsInOrder() {
        assertEquals(List.of("PLAYER", "ROOKIE", "APPRENTICE", "NOVICE", "EXPERT",
                "NOBLE", "LORD", "DUKE", "MASTER", "GRANDMASTER", "IMMORTAL", "ETERNAL"), ids(Rank.Kind.PLAYTIME));

        List<Long> hours = Ranks.ofKind(Rank.Kind.PLAYTIME).stream().map(rank -> rank.requirement() / HOUR).toList();
        assertEquals(List.of(1L, 5L, 10L, 25L, 50L, 100L, 150L, 250L, 350L, 500L, 750L, 1000L), hours);
    }

    @Test
    void theShopSellsRanksCheapestFirstAndAuroraIsTheBest() {
        assertEquals(List.of("EMBER", "FROST", "VERDANT", "STORM", "GILDED", "INFERNO", "ECLIPSE", "VOID", "AURORA"),
                ids(Rank.Kind.SHOP));

        List<Long> prices = Ranks.ofKind(Rank.Kind.SHOP).stream().map(Rank::requirement).toList();
        assertEquals(List.of(100L, 300L, 750L, 1_200L, 2_000L, 3_500L, 5_000L, 9_000L, 15_000L), prices);
    }

    // Handed out by an admin, never earned or bought
    @Test
    void donorAndStaffRanksHaveNoRequirement() {
        assertEquals(List.of("LUMINARY", "CELESTIAL", "NOVA", "ZENITH"), ids(Rank.Kind.DONOR));
        assertEquals(23, ids(Rank.Kind.STAFF).size());
        assertTrue(ids(Rank.Kind.STAFF).containsAll(List.of("HELPER", "MOD", "SENIOR_MOD", "ADMIN", "SENIOR_ADMIN", "FOUNDER")));
        assertEquals("OWNER", ids(Rank.Kind.STAFF).getLast());
        for (Rank rank : Ranks.all()) {
            if (rank.kind() == Rank.Kind.DONOR || rank.kind() == Rank.Kind.STAFF) assertEquals(0, rank.requirement());
        }
    }

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

    @Test
    void everyRankShowsItsOwnNameInBold() {
        for (Rank rank : Ranks.all()) {
            assertEquals(rank.id().replace('_', ' '), PlainTextComponentSerializer.plainText().serialize(rank.component()));
            assertTrue(rank.display().contains("<b>"), rank.id());
        }
    }

    @Test
    void everyRankButMemberHasAGradient() {
        for (Rank rank : Ranks.all()) {
            if (rank != Ranks.MEMBER) assertTrue(rank.display().startsWith("<gradient:"), rank.id());
        }
    }
}
