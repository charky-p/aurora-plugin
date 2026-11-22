package org.charky.aurora.features.gauntlet;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GauntletRunTest {
    @Test
    void aSoloRunIsNeverScaled() {
        assertEquals(1.0, GauntletRun.partyScale(1, 50));
        assertEquals(1.0, GauntletRun.partyScale(0, 50), "nobody left is no less than one player");
    }

    @Test
    void everyPlayerPastTheFirstAddsThePercent() {
        assertEquals(1.5, GauntletRun.partyScale(2, 50));
        assertEquals(2.8, GauntletRun.partyScale(4, 60), 1e-9);
        assertEquals(1.0, GauntletRun.partyScale(4, 0), "0 percent for a party pays what one player gets");
    }

    @Test
    void floorsClearedCountTheFloorInProgressOnlyOnceItIsDone() {
        GauntletRun run = new GauntletRun(0);
        run.floor = 7;
        run.state = GauntletRun.State.FIGHTING;
        assertEquals(6, run.cleared());
        run.state = GauntletRun.State.CHOOSING;
        assertEquals(7, run.cleared(), "picking a boon comes after the floor is cleared");
    }

    @Test
    void aPlayerIsChoosingWhileABoonOrAnAltarWaits() {
        GauntletRun run = new GauntletRun(0);
        GauntletRun.Member member = new GauntletRun.Member(UUID.randomUUID());
        run.members.put(member.id, member);
        assertFalse(run.anyChoosing());
        member.offered = List.of("might", "vitality");
        assertTrue(run.anyChoosing());
        member.offered = List.of();
        member.altar = true;
        assertTrue(run.anyChoosing());
    }
}
