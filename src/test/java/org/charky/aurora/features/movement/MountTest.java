package org.charky.aurora.features.movement;

import org.charky.aurora.features.perk.Perk;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MountTest {
    // Every mount perk sold has a mount to spawn, or its saddle does nothing
    @Test
    void everyMountPerkHasAMount() {
        for (Perk perk : Perk.values()) {
            if (perk.mount() != null) assertNotNull(Mount.byId(perk.mount()), perk.id());
        }
    }

    // Each level is faster, and a level outside 1 to 3 counts as the nearest
    @Test
    void levelsGetFaster() {
        for (Mount mount : Mount.values()) {
            assertTrue(mount.speed(2) > mount.speed(1), mount.name());
            assertTrue(mount.speed(3) > mount.speed(2), mount.name());
            assertEquals(mount.speed(1), mount.speed(0));
            assertEquals(mount.speed(3), mount.speed(9));
        }
    }
}
