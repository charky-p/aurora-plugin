package org.charky.aurora.features.loadout;

import org.junit.jupiter.api.Test;

import static org.charky.aurora.features.loadout.Loadout.EXPERIMENTAL;
import static org.charky.aurora.features.loadout.Loadout.MAIN;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class LoadoutSwitchTest {
    @Test
    void worldsPickTheirLoadout() {
        assertEquals(EXPERIMENTAL, LoadoutManager.toPlay(EXPERIMENTAL, MAIN, false));
        assertEquals(MAIN, LoadoutManager.toPlay(MAIN, EXPERIMENTAL, false));
        assertNull(LoadoutManager.toPlay(MAIN, MAIN, false));
        assertNull(LoadoutManager.toPlay(EXPERIMENTAL, EXPERIMENTAL, false));
    }

    // A build world keeps whichever is in play
    @Test
    void buildWorldsKeepTheOneInPlay() {
        assertNull(LoadoutManager.toPlay(null, MAIN, false));
        assertNull(LoadoutManager.toPlay(null, EXPERIMENTAL, true));
    }

    // /aurora testworld: the main loadout stays in test worlds, a main world ends the visit
    @Test
    void carryingMainStaysInTestWorlds() {
        assertNull(LoadoutManager.toPlay(EXPERIMENTAL, MAIN, true));
        assertNull(LoadoutManager.toPlay(MAIN, MAIN, true));
    }
}
