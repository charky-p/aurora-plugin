package org.charky.aurora.features.loadout;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

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
}
