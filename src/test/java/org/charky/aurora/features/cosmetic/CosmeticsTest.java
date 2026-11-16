package org.charky.aurora.features.cosmetic;

import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.charky.aurora.features.cosmetic.Cosmetic.Slot;
import org.charky.aurora.features.cosmetic.Cosmetic.Source;
import org.charky.aurora.features.cosmetic.Cosmetic.Type;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CosmeticsTest {
    @Test
    void everyKindHasSomethingToWear() {
        for (Type type : Type.values()) {
            assertFalse(Cosmetics.ofType(type).isEmpty(), type.name());
        }
    }

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
    void everyNameCanBeShown() {
        for (Cosmetic cosmetic : Cosmetics.all()) {
            assertFalse(PlainTextComponentSerializer.plainText().serialize(cosmetic.component()).isBlank(), cosmetic.id());
        }
    }

    @Test
    void coloursAreRealColours() {
        for (Type type : List.of(Type.CHAT_COLOR, Type.NAME_COLOR)) {
            assertEquals(12, Cosmetics.ofType(type).size());
            for (Cosmetic cosmetic : Cosmetics.ofType(type)) {
                assertNotNull(NamedTextColor.NAMES.value(cosmetic.value()), cosmetic.id());
            }
        }
        assertNotNull(NamedTextColor.NAMES.value(Cosmetics.DEFAULT_CHAT_COLOR));
        assertNotNull(NamedTextColor.NAMES.value(Cosmetics.DEFAULT_NAME_COLOR));
    }

    @Test
    void everyJoinMessageNamesThePlayer() {
        for (Cosmetic cosmetic : Cosmetics.ofType(Type.JOIN_MESSAGE)) {
            assertTrue(cosmetic.value().contains(Cosmetics.PLAYER_TAG), cosmetic.id());
        }
    }

    @Test
    void onlySuffixesThatCostRealMoneyAreBold() {
        for (Cosmetic suffix : Cosmetics.ofType(Type.SUFFIX)) {
            if (suffix.source() != Source.GRANT) assertFalse(suffix.display().contains("<b>"), suffix.id());
        }
    }

    // A zone or a world that is maxed out earns a suffix, and nothing else is tied to one
    @Test
    void zonesAndWorldsEarnSuffixes() {
        for (Cosmetic cosmetic : Cosmetics.all()) {
            boolean earned = cosmetic.source() == Source.ZONE || cosmetic.source() == Source.WORLD;
            assertEquals(earned, cosmetic.target() != null, cosmetic.id());
            if (earned) assertEquals(Type.SUFFIX, cosmetic.type(), cosmetic.id());
        }
        assertEquals("suffix_cobble", Cosmetics.earnedIn(Source.ZONE, "cobble").getFirst().id());
        assertEquals("suffix_duneworld", Cosmetics.earnedIn(Source.WORLD, "gormak").getFirst().id());
        // The Overworld is two regions, finishing either can be what completes it
        assertEquals("suffix_overworld", Cosmetics.earnedIn(Source.WORLD, "verdant").getFirst().id());
        assertEquals("suffix_overworld", Cosmetics.earnedIn(Source.WORLD, "origin_isles").getFirst().id());
    }

    // Light gray chat is free, white comes with time played, the rest cost real money
    @Test
    void coloursBeyondThePlainOnesAreNotSoldForEssence() {
        for (Type type : List.of(Type.CHAT_COLOR, Type.NAME_COLOR)) {
            for (Cosmetic cosmetic : Cosmetics.ofType(type)) assertTrue(cosmetic.source() != Source.ESSENCE, cosmetic.id());
        }
        assertEquals(Source.PLAYTIME, Cosmetics.get("chat_white").source());
    }

    // New players wear NEW, speak in light grey under a white name and join the plain way
    @Test
    void untouchedSlotsHoldTheirDefaults() {
        assertEquals("suffix_new", Cosmetics.defaultFor(Slot.SUFFIX_1).id());
        assertEquals("chat_light_gray", Cosmetics.defaultFor(Slot.CHAT_COLOR).id());
        assertEquals("name_white", Cosmetics.defaultFor(Slot.NAME_COLOR).id());
        assertEquals("join_default", Cosmetics.defaultFor(Slot.JOIN_MESSAGE).id());
        assertNull(Cosmetics.defaultFor(Slot.SUFFIX_2));
        assertNull(Cosmetics.defaultFor(Slot.TRAIL));
        assertNull(Cosmetics.defaultFor(Slot.KILL_EFFECT));
    }

    @Test
    void everythingWornByDefaultIsFree() {
        for (Slot slot : Slot.values()) {
            Cosmetic standard = Cosmetics.defaultFor(slot);
            if (standard != null) assertEquals(Source.DEFAULT, standard.source(), slot.name());
        }
    }

    @Test
    void suffixesHaveTwoPlacesAndTheRestOne() {
        assertEquals(Slot.SUFFIX_1, Slot.of(Type.SUFFIX));
        for (Type type : Type.values()) {
            long slots = java.util.Arrays.stream(Slot.values()).filter(slot -> slot.type() == type).count();
            assertEquals(type == Type.SUFFIX ? 2 : 1, slots, type.name());
        }
    }
}
