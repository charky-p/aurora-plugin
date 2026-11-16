package org.charky.aurora.features.tool;

import org.charky.aurora.features.tool.Realms.Realm;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ToolLevelsTest {
    private static final List<String> MATERIALS = List.of("WOODEN", "STONE", "COPPER", "IRON");

    private static final Realm OVERWORLD = new Realm("overworld", "Overworld", List.of("aurora_mines"), null);
    private static final Realm DUNE = new Realm("duneworld", "Duneworld", List.of("gormak"), "");
    private static final Realm WINTER = new Realm("winter", "Winter", List.of("xmas"), "overworld");
    private static final Realms REALMS = new Realms(List.of(OVERWORLD, DUNE, WINTER));

    @Test
    void aRealmAloneKeepsItsOwnLevels() {
        assertEquals(Map.of("fortune", 3, "efficiency", 2),
                ToolLevels.combine(List.of(Map.of("fortune", 3, "efficiency", 2))));
        assertTrue(ToolLevels.combine(List.of()).isEmpty());
    }

    // Winter Fortune 2 on top of Fortune 5 mines as Fortune 7, only in the winter world
    @Test
    void levelsAddOnTopOfTheRealmTheyBuildOn() {
        Map<String, Integer> winter = Map.of("fortune", 2);
        Map<String, Integer> overworld = Map.of("fortune", 5, "efficiency", 4);

        assertEquals(Map.of("fortune", 7, "efficiency", 4), ToolLevels.combine(List.of(winter, overworld)));
    }

    @Test
    void theMaterialIsTheBetterOfTheTwoNotTheirSum() {
        Map<String, Integer> combined = ToolLevels.combine(List.of(
                Map.of(ToolLevels.MATERIAL, 1), Map.of(ToolLevels.MATERIAL, 3)));

        assertEquals(3, combined.get(ToolLevels.MATERIAL));
    }

    @Test
    void aMaterialLevelIsItsPlaceInTheList() {
        assertEquals("WOODEN", ToolLevels.material(MATERIALS, 0));
        assertEquals("COPPER", ToolLevels.material(MATERIALS, 2));
        // Out of range never breaks the item
        assertEquals("IRON", ToolLevels.material(MATERIALS, 99));
        assertEquals("WOODEN", ToolLevels.material(MATERIALS, -1));

        assertEquals(2, ToolLevels.materialLevel(MATERIALS, "copper"));
        assertEquals(-1, ToolLevels.materialLevel(MATERIALS, "DIAMOND"));
    }

    @Test
    void aWorldBelongsToItsRealm() {
        assertSame(DUNE, REALMS.ofWorld("gormak"));
        assertSame(WINTER, REALMS.ofWorld("XMAS"));
        // A world nobody listed is the first realm's
        assertSame(OVERWORLD, REALMS.ofWorld("somewhere_else"));
        assertNull(new Realms(List.of()).ofWorld("anything"));
    }

    @Test
    void aRealmChainsToWhatItBuildsOn() {
        assertEquals(List.of(WINTER, OVERWORLD), REALMS.chain(WINTER));
        // A mini world with no base stands alone
        assertEquals(List.of(DUNE), REALMS.chain(DUNE));
        assertEquals(List.of(OVERWORLD), REALMS.chain(OVERWORLD));
    }

    @Test
    void aBaseThatLoopsOrIsMissingEndsTheChain() {
        Realm a = new Realm("a", "A", List.of(), "b");
        Realm b = new Realm("b", "B", List.of(), "a");
        Realm lost = new Realm("lost", "Lost", List.of(), "nowhere");
        Realms realms = new Realms(List.of(a, b, lost));

        assertEquals(List.of(a, b), realms.chain(a));
        assertEquals(List.of(lost), realms.chain(lost));
    }
}
