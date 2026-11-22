package org.charky.aurora.features.gauntlet;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GauntletBonusesTest {
    private static GauntletData.Boon boon(String id, int max, String rarity, List<String> requires) {
        return new GauntletData.Boon(id, id, "STONE", "", Map.of("damage", 10.0), max, rarity, requires);
    }

    private static final Map<String, GauntletData.Boon> BOONS = Map.of(
            "might", boon("might", 5, "common", List.of()),
            "cleave", boon("cleave", 2, "epic", List.of()),
            "second_wind", boon("second_wind", 1, "epic", List.of()));

    @Test
    void aLegendaryOpensOnceEveryBoonItNeedsHasTheStacks() {
        GauntletData.Boon whirlwind = boon("whirlwind", 1, "legendary", List.of("cleave", "might"));
        GauntletRun.Member member = new GauntletRun.Member(UUID.randomUUID());
        member.boons.put("might", 2);
        member.boons.put("cleave", 1);
        assertFalse(GauntletBonuses.unlocked(member, whirlwind, BOONS, 2));
        member.boons.put("cleave", 2);
        assertTrue(GauntletBonuses.unlocked(member, whirlwind, BOONS, 2));
        // An ordinary boon is always open
        assertTrue(GauntletBonuses.unlocked(new GauntletRun.Member(UUID.randomUUID()), BOONS.get("might"), BOONS, 2));
    }

    @Test
    void aBoonWithAMaxBelowTheStacksNeedsOnlyItsMax() {
        GauntletData.Boon aegis = boon("aegis", 1, "legendary", List.of("second_wind"));
        GauntletRun.Member member = new GauntletRun.Member(UUID.randomUUID());
        assertFalse(GauntletBonuses.unlocked(member, aegis, BOONS, 2));
        member.boons.put("second_wind", 1);
        assertTrue(GauntletBonuses.unlocked(member, aegis, BOONS, 2), "Second Wind's max is 1");
    }
}
