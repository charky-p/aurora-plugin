package org.charky.aurora.features.gauntlet;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BestiaryMenuTest {
    @Test
    void mobsComeInOrderOfTheirFirstFloorAndCountOnceKilled() {
        Map<String, Integer> second = new LinkedHashMap<>();
        second.put("g_rotter", 4);
        second.put("g_crawler", 1);
        GauntletData data = new GauntletData(List.of(
                new GauntletData.Floor(Map.of("g_rotter", 3), 1.0, 1, 1, 4, 0),
                new GauntletData.Floor(second, 1.1, 2, 1, 5, 0)),
                Map.of(), List.of(), Map.of(), List.of(), List.of(), Map.of(), Map.of(), Map.of(), Map.of(),
                Map.of(), List.of(), Map.of(), List.of(), List.of(), Map.of());

        assertEquals(Map.of("g_rotter", 1, "g_crawler", 2), BestiaryMenu.firstFloors(data));
        assertEquals(List.of("g_rotter", "g_crawler"), List.copyOf(BestiaryMenu.firstFloors(data).keySet()));

        GauntletProfile profile = new GauntletProfile();
        assertEquals(0, BestiaryMenu.met(data, profile));
        profile.stats.put(GauntletRuns.SLAIN + "g_crawler", 2);
        assertEquals(1, BestiaryMenu.met(data, profile));
    }
}
