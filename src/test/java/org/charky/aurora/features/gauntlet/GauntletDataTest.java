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
        return new GauntletData(floors, Map.of(), List.of(), Map.of(), List.of(), List.of(), Map.of(), Map.of(), Map.of(), Map.of());
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
        assertEquals(new GauntletData.Variant("g_rotter", 12, false, null), plain);
        GauntletData.Variant full = new GauntletData.Variant("g_rot_king", 10, true, "swift");
        assertEquals(full, GauntletData.Variant.parse(full.id()));
        assertEquals("g_rot_king", GauntletData.baseMobId(full.id()));
        assertNull(GauntletData.Variant.parse("g_rotter"));
        assertNull(GauntletData.Variant.parse("g_rotter#x"));
    }

    @Test
    void noFloorsNoFloor() {
        assertNull(GauntletData.empty().floor(1));
        assertFalse(GauntletData.empty().ready());
    }
}
