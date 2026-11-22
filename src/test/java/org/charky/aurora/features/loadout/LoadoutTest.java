package org.charky.aurora.features.loadout;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class LoadoutTest {
    @Test
    void testWorldsAreExperimental() {
        assertEquals(Loadout.EXPERIMENTAL, Loadout.of("aurora_test"));
        assertEquals(Loadout.EXPERIMENTAL, Loadout.of("Aurora_Test_Gormak"));
        assertEquals(Loadout.MAIN, Loadout.of("aurora_overworld"));
        assertEquals(Loadout.MAIN, Loadout.of("build_world"));
        assertEquals(Loadout.MAIN, Loadout.of("my_aurora_test"));
    }

    // The put-away id never changes and never is the player's own
    @Test
    void shadowIsStable() {
        UUID uuid = UUID.randomUUID();
        assertEquals(Loadout.shadowOf(uuid), Loadout.shadowOf(uuid));
        assertNotEquals(uuid, Loadout.shadowOf(uuid));
        assertNotEquals(Loadout.shadowOf(uuid), Loadout.shadowOf(UUID.randomUUID()));
    }

    @Test
    void idsRoundTrip() {
        for (Loadout loadout : Loadout.values()) assertEquals(loadout, Loadout.byId(loadout.id()));
        assertEquals(Loadout.MAIN, Loadout.byId("nonsense"));
        assertEquals(Loadout.MAIN, Loadout.byId(null));
    }

    // Every loadout is kept under its own id while put away, never another's or the
    // player's own, and experimental keeps the id from before there were slots
    @Test
    void homesAreDistinct() {
        UUID uuid = UUID.randomUUID();
        java.util.Set<UUID> homes = new java.util.HashSet<>();
        for (Loadout loadout : Loadout.values()) {
            assertEquals(loadout.homeOf(uuid), loadout.homeOf(uuid));
            assertNotEquals(uuid, loadout.homeOf(uuid));
            homes.add(loadout.homeOf(uuid));
        }
        assertEquals(Loadout.values().length, homes.size());
        assertEquals(Loadout.shadowOf(uuid), Loadout.EXPERIMENTAL.homeOf(uuid));
    }

    @Test
    void namesPickSlots() {
        assertEquals(Loadout.MAIN, Loadout.byName("1"));
        assertEquals(Loadout.MAIN, Loadout.byName("main"));
        assertEquals(Loadout.SLOT_2, Loadout.byName("2"));
        assertEquals(Loadout.SLOT_3, Loadout.byName("3"));
        assertEquals(Loadout.EXPERIMENTAL, Loadout.byName("experimental"));
        assertNull(Loadout.byName("4"));
    }
}
