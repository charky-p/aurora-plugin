package org.charky.aurora.features.loadout;

import org.junit.jupiter.api.Test;

import static org.charky.aurora.features.loadout.Loadout.EXPERIMENTAL;
import static org.charky.aurora.features.loadout.Loadout.MAIN;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class LoadoutSwitchTest {
    @Test
    void worldsPickTheirLoadout() {
        assertEquals(EXPERIMENTAL, LoadoutManager.toPlay(EXPERIMENTAL, MAIN));
        assertEquals(MAIN, LoadoutManager.toPlay(MAIN, EXPERIMENTAL));
        assertNull(LoadoutManager.toPlay(MAIN, MAIN));
        assertNull(LoadoutManager.toPlay(EXPERIMENTAL, EXPERIMENTAL));
    }

    // A build world keeps whichever is in play
    @Test
    void buildWorldsKeepTheOneInPlay() {
        assertNull(LoadoutManager.toPlay(null, MAIN));
        assertNull(LoadoutManager.toPlay(null, EXPERIMENTAL));
    }
}
