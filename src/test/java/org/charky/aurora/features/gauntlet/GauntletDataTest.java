package org.charky.aurora.features.gauntlet;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

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
                Map.of(), List.of(), Map.of(), List.of(), List.of(), Map.of());
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
    void parsingLeavesOutUnknownMobsAndFillsDefaults() {
        Map<String, Object> mobs = new LinkedHashMap<>();
        mobs.put("g_rotter", 3);
        mobs.put("g_nobody", 2);
        Map<String, Object> root = Map.of(
                "floors", List.of(Map.of("mobs", mobs, "health", 1.5, "attack", 2, "kill", 0, "shards", 4, "cores", 0)),
                "boons", Map.of("might", Map.of("name", "Might", "stat", "damage", "amount", 10, "max", 5),
                        "whirlwind", Map.of("name", "Whirlwind", "effects", Map.of("cleave", 40, "damage", 20), "max", 1,
                                "rarity", "legendary", "requires", List.of("cleave", "might"))),
                "bosses", Map.of("g_rot_king", List.of(Map.of("type", "slam", "cooldown", 0))),
                "settings", Map.of("party_max", "4"));
        List<String> logged = new ArrayList<>();
        Logger logger = Logger.getAnonymousLogger();
        logger.setUseParentHandlers(false);
        logger.addHandler(new Handler() {
            @Override
            public void publish(LogRecord record) {
                logged.add(record.getMessage());
            }

            @Override
            public void flush() {
            }

            @Override
            public void close() {
            }
        });

        GauntletData data = GauntletData.parse(root, Set.of("g_rotter"), logger);

        assertEquals(Map.of("g_rotter", 3), data.floors().getFirst().mobs());
        assertEquals(List.of("GAUNTLET ERROR: no mob 'g_nobody'"), logged);
        assertEquals(1, data.floors().getFirst().kill(), "a kill pays at least 1 Shard");
        assertEquals("common", data.boons().get("might").rarity(), "a boon without a rarity is common");
        assertEquals(10, data.boons().get("might").effect("damage"), "a file from before effects has one stat and amount");
        assertFalse(data.boons().get("might").legendary());
        GauntletData.Boon whirlwind = data.boons().get("whirlwind");
        assertEquals(Map.of("cleave", 40.0, "damage", 20.0), whirlwind.effects());
        assertEquals(List.of("cleave", "might"), whirlwind.requires());
        assertEquals(1, data.bosses().get("g_rot_king").getFirst().cooldown(), "a cooldown is at least a second");
        assertEquals(4, data.setting("party_max", 0), "settings read as numbers");
        assertTrue(data.gear().isEmpty() && data.levels().isEmpty(), "missing sections are empty");
    }

    // Gear levels wait for the bosses: 10 a boss beaten and 10 before the first
    @Test
    void levelCapRisesWithEachBoss() {
        GauntletData data = GauntletData.parse(Map.of(), Set.of(), Logger.getAnonymousLogger());
        assertEquals(10, data.levelCap(0));
        assertEquals(10, data.levelCap(9));
        assertEquals(20, data.levelCap(10));
        assertEquals(30, data.levelCap(29), "until floor 30's boss falls");
        assertEquals(40, data.levelCap(30));
        assertEquals(30, GauntletData.nextBoss(29));
        assertEquals(40, GauntletData.nextBoss(30));
    }

    // A gear level has every vanilla enchant from its level on, each at the most reached
    @Test
    void gearEnchantsComeWithLevels() {
        GauntletData.Gear mace = new GauntletData.Gear("mace", "Mace", "MACE", List.of("NONE"), "melee", 90, 600, 15, 8,
                List.of(new GauntletData.GearEnchant(0, "wind_burst", 1), new GauntletData.GearEnchant(20, "wind_burst", 2),
                        new GauntletData.GearEnchant(30, "density", 1)));
        assertEquals(Map.of("wind_burst", 1), mace.enchantsAt(8));
        assertEquals(Map.of("wind_burst", 2), mace.enchantsAt(20));
        assertEquals(Map.of("wind_burst", 2, "density", 1), mace.enchantsAt(30));
        assertEquals("Wind Burst II", GauntletGear.enchantName("wind_burst", 2));
    }

    // The starting kit's own items and the main game's tools and items
    @Test
    void setupItems() {
        GauntletData.SetupItem quiver = new GauntletData.SetupItem("quiver", "Quiver", "ARROW", 16, 4, 120, 0, "");
        assertNull(quiver.tool());
        assertNull(quiver.mainItem());
        assertEquals("48", quiver.startsWith(3));
        assertEquals(64, quiver.count(5), "at most a stack");

        GauntletData.SetupItem grapple = new GauntletData.SetupItem("grapple", "Grapple", "TOOL@grapple", 1, 6, 400, 0, "");
        assertEquals("grapple", grapple.tool());
        assertNull(grapple.mainItem());
        assertEquals(0, grapple.upgrades(1), "no upgrades at the first level");
        assertEquals(5, grapple.upgrades(6));
        assertEquals("level 3", grapple.startsWith(3));
        assertEquals("nothing", grapple.startsWith(0));

        GauntletData.SetupItem dash = new GauntletData.SetupItem("dash", "Dash Feather", "ITEM@dash_feather", 1, 1, 1500, 0, "");
        assertEquals("dash_feather", dash.mainItem());
        assertNull(dash.tool());
        assertEquals("1", dash.startsWith(1));
    }
}
