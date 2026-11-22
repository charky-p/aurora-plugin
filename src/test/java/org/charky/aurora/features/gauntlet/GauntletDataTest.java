package org.charky.aurora.features.gauntlet;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GauntletDataTest {
    private static GauntletData twoFloors() {
        List<GauntletData.Floor> floors = List.of(
                new GauntletData.Floor(Map.of("g_rotter", 3), 1.0, 1, 1, 4, 0),
                new GauntletData.Floor(Map.of("g_rot_king", 1, "g_rotter", 4), 2.0, 10, 2, 10, 1));
        return new GauntletData(floors, Map.of(), List.of(), Map.of(), List.of(), List.of(), Map.of(), Map.of(), Map.of(), Map.of(),
                Map.of(), List.of(), Map.of(), List.of(), Map.of());
    }

    @Test
    void floorsInTheFileAreTakenAsTheyAre() {
        GauntletData data = twoFloors();
        assertEquals(1.0, data.floor(1).health());
        assertFalse(data.floor(1).boss());
        assertTrue(data.floor(2).boss());
    }

    @Test
    void pastTheLastFloorItRepeatsHarder() {
        GauntletData data = twoFloors();
        GauntletData.Floor third = data.floor(3);
        assertEquals(data.floor(2).mobs(), third.mobs());
        assertEquals(2.0 * GauntletData.ENDLESS_HEALTH, third.health(), 1e-9);
        assertEquals(Math.round(10 + GauntletData.ATTACK_PER_FLOOR), third.attack());
        assertEquals(11, third.shards());
        // Every 10th floor still pays a Core
        assertEquals(0, third.cores());
        assertTrue(data.floor(10).cores() > 0);
    }

    @Test
    void aBossKillPaysFiveKills() {
        GauntletData.Floor boss = twoFloors().floor(2);
        assertEquals(2, boss.killPay("g_rotter"));
        assertEquals(2 * GauntletData.BOSS_KILLS, boss.killPay("g_rot_king"));
        // On a floor with no boss a lone mob is just a mob
        assertEquals(1, twoFloors().floor(1).killPay("g_rotter"));
        assertEquals("g_rotter", GauntletData.baseMobId("g_rotter#12"));
    }

    @Test
    void variantIdsGoBothWays() {
        GauntletData.Variant plain = GauntletData.Variant.parse("g_rotter#12");
        assertEquals(new GauntletData.Variant("g_rotter", 12, false, 0, null), plain);
        GauntletData.Variant full = new GauntletData.Variant("g_rot_king", 10, true, 3, "swift");
        assertEquals("g_rot_king#10*^3~swift", full.id());
        GauntletData.Variant affixed = new GauntletData.Variant("g_brute", 14, true, "vampiric", 2, "horde");
        assertEquals("g_brute#14*!vampiric^2~horde", affixed.id());
        assertEquals(affixed, GauntletData.Variant.parse(affixed.id()));
        assertEquals(full, GauntletData.Variant.parse(full.id()));
        assertEquals("g_rot_king", GauntletData.baseMobId(full.id()));
        assertNull(GauntletData.Variant.parse("g_rotter"));
        assertNull(GauntletData.Variant.parse("g_rotter#x"));
    }

    @Test
    void aScaledChallengeRisesWithTheBestFloor() {
        GauntletData.Challenge climb = new GauntletData.Challenge("climb", "Clear floor {goal}", "floor", 5, 60, 0, true);
        assertEquals(5, climb.goalFor(0));
        assertEquals(20, climb.goalFor(23));
        assertEquals("Clear floor 20", climb.describe(23));
        GauntletData.Challenge kills = new GauntletData.Challenge("slayer", "Kill {goal} mobs", "kills", 60, 40, 0, false);
        assertEquals(60, kills.goalFor(40));
    }

    @Test
    void heatUnlocksEveryTenFloorsFromFifteen() {
        GauntletData data = twoFloors();
        assertEquals(0, data.heatUnlocked(14));
        assertEquals(1, data.heatUnlocked(15));
        assertEquals(1, data.heatUnlocked(24));
        assertEquals(2, data.heatUnlocked(25));
        assertEquals(5, data.heatUnlocked(500));
    }

    @Test
    void noFloorsNoFloor() {
        assertNull(GauntletData.empty().floor(1));
        assertFalse(GauntletData.empty().ready());
    }
}
