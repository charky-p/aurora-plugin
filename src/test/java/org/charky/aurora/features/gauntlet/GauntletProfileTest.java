package org.charky.aurora.features.gauntlet;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class GauntletProfileTest {
    @Test
    void aPriceThatCanBePaidIsTaken() {
        GauntletProfile profile = new GauntletProfile();
        profile.shards = 100;
        profile.cores = 3;
        assertNull(profile.pay(60, 2));
        assertEquals(40, profile.shards);
        assertEquals(1, profile.cores);
    }

    @Test
    void aPriceThatCantBePaidTakesNothingAndSaysWhatIsMissing() {
        GauntletProfile profile = new GauntletProfile();
        profile.shards = 50;
        profile.cores = 5;
        assertEquals("You need 10 more Shards", profile.pay(60, 0));
        assertEquals("You need 2 more Cores", profile.pay(10, 7));
        assertEquals(50, profile.shards, "nothing is taken from a price that can't be paid");
        assertEquals(5, profile.cores);
    }

    @Test
    void lockedGearHasNoLevel() {
        GauntletProfile profile = new GauntletProfile();
        assertEquals(-1, profile.level("bow"));
        profile.gear.put("bow", 0);
        assertEquals(0, profile.level("bow"));
    }
}
