package org.charky.aurora.core.data;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RegionsTest {
    // Origin Isles and Gormak released, Gormak Deep a region of its own that is not
    private static Regions regions() {
        Map<String, String> zones = new LinkedHashMap<>();
        zones.put("cobble", "origin_isles");
        zones.put("coal", "origin_isles");
        zones.put("city", "gormak");
        zones.put("caves", "gormak_deep");
        zones.put("canyons", "khorvath");
        return new Regions(zones, Set.of("origin_isles", "gormak"));
    }

    @Test
    void zonesFollowTheirRegion() {
        Regions regions = regions();
        assertEquals("gormak", regions.of("city"));
        assertEquals("", regions.of("nowhere"));
        assertTrue(regions.isZoneReleased("coal"));
        assertFalse(regions.isZoneReleased("canyons"));
        assertFalse(regions.isZoneReleased("nowhere"));
        assertTrue(regions.isReleased("GORMAK"));
        assertFalse(regions.isReleased(null));
    }

    @Test
    void zonesComeInFileOrder() {
        Regions regions = regions();
        assertEquals(List.of("cobble", "coal", "city"), regions.releasedZones());
        assertEquals(List.of("cobble", "coal"), regions.zonesOf("origin_isles"));
        assertEquals(List.of(), regions.zonesOf("duat"));
    }

    // A column is a region or one of its zones, and the longest region that fits wins,
    // so gormak_deep_caves is Gormak Deep, not Gormak
    @Test
    void columnsMeanTheLongestRegion() {
        Regions regions = regions();
        assertEquals("gormak", regions.fromColumn("gormak"));
        assertEquals("gormak", regions.fromColumn("gormak_city"));
        assertEquals("gormak_deep", regions.fromColumn("gormak_deep_caves"));
        assertNull(regions.fromColumn("gormakx"));
        assertNull(regions.fromColumn("fishing"));
        assertNull(regions.fromColumn(""));
    }

    // No column is always shown, one naming no region is switched off
    @Test
    void columnsAreReleasedWithTheirRegion() {
        Regions regions = regions();
        assertTrue(regions.isColumnReleased(null));
        assertTrue(regions.isColumnReleased(" "));
        assertTrue(regions.isColumnReleased("origin_isles"));
        assertTrue(regions.isColumnReleased("gormak_city"));
        assertFalse(regions.isColumnReleased("gormak_deep_caves"));
        assertFalse(regions.isColumnReleased("fishing"));
    }

    @Test
    void noneHasNothing() {
        assertEquals(List.of(), Regions.none().releasedZones());
        assertTrue(Regions.none().isColumnReleased(null));
        assertFalse(Regions.none().isColumnReleased("gormak"));
    }
}
