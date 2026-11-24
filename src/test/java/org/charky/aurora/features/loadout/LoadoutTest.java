package org.charky.aurora.features.loadout;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class LoadoutTest {
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
}
