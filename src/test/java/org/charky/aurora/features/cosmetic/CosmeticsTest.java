package org.charky.aurora.features.cosmetic;

import net.kyori.adventure.text.format.NamedTextColor;
import org.charky.aurora.features.cosmetic.Cosmetic.Slot;
import org.charky.aurora.features.cosmetic.Cosmetic.Source;
import org.charky.aurora.features.cosmetic.Cosmetic.Type;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CosmeticsTest {
    @Test
    void idsAreUniqueAndFoundAgain() {
        Set<String> ids = new HashSet<>();
        for (Cosmetic cosmetic : Cosmetics.all()) {
            assertTrue(ids.add(cosmetic.id()), cosmetic.id());
            assertEquals(cosmetic, Cosmetics.get(cosmetic.id().toUpperCase()));
        }
        assertNull(Cosmetics.get("suffix_wizard"));
        assertNull(Cosmetics.get(null));
        // The second suffix is an unlock, not something to wear
        assertNull(Cosmetics.get(Cosmetics.SECOND_SUFFIX_SLOT));
    }

    @Test
    void whatIsSoldHasAPriceAndWhatIsEarnedHasHours() {
        for (Cosmetic cosmetic : Cosmetics.all()) {
            if (cosmetic.source() == Source.ESSENCE || cosmetic.source() == Source.PLAYTIME) {
                assertTrue(cosmetic.requirement() > 0, cosmetic.id());
            }
        }
    }

    @Test
    void coloursAreRealColours() {
        for (Type type : List.of(Type.CHAT_COLOR, Type.NAME_COLOR)) {
            for (Cosmetic cosmetic : Cosmetics.ofType(type)) {
                assertNotNull(NamedTextColor.NAMES.value(cosmetic.value()), cosmetic.id());
            }
        }
        assertNotNull(NamedTextColor.NAMES.value(Cosmetics.DEFAULT_CHAT_COLOR));
        assertNotNull(NamedTextColor.NAMES.value(Cosmetics.DEFAULT_NAME_COLOR));
    }

    // A zone or a world that is maxed out earns a suffix, and nothing else is tied to one.
    // A look names the kind of tool whose material unlocks it instead
    @Test
    void zonesAndWorldsEarnSuffixes() {
        for (Cosmetic cosmetic : Cosmetics.all()) {
            boolean earned = cosmetic.source() == Source.ZONE || cosmetic.source() == Source.WORLD;
            assertEquals(earned || cosmetic.source() == Source.MATERIAL, cosmetic.target() != null, cosmetic.id());
            if (earned) assertEquals(Type.SUFFIX, cosmetic.type(), cosmetic.id());
        }
        assertEquals("suffix_cobble", Cosmetics.earnedIn(Source.ZONE, "cobble").getFirst().id());
        assertEquals("suffix_duneworld", Cosmetics.earnedIn(Source.WORLD, "gormak").getFirst().id());
        // The Overworld is two regions, finishing either can be what completes it
        assertEquals("suffix_overworld", Cosmetics.earnedIn(Source.WORLD, "verdant").getFirst().id());
        assertEquals("suffix_overworld", Cosmetics.earnedIn(Source.WORLD, "origin_isles").getFirst().id());
    }

    @Test
    void everythingWornByDefaultIsFree() {
        for (Slot slot : Slot.values()) {
            Cosmetic standard = Cosmetics.defaultFor(slot);
            if (standard != null) assertEquals(Source.DEFAULT, standard.source(), slot.name());
        }
    }
}
