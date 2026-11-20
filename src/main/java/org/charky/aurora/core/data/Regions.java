package org.charky.aurora.core.data;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

// The region each zone belongs to, in zones.csv order, and which regions are marked
// released in regions.csv. Players are only shown released regions
public class Regions {
    private final Map<String, String> zoneRegions;
    private final Set<String> released;

    // zoneRegions is zone id -> region, both lower case
    public Regions(Map<String, String> zoneRegions, Set<String> released) {
        this.zoneRegions = Collections.unmodifiableMap(new LinkedHashMap<>(zoneRegions));
        this.released = Set.copyOf(released);
    }

    public static Regions none() {
        return new Regions(Map.of(), Set.of());
    }

    // The region a zone is part of, as zones.csv names it, empty for an unknown zone
    public String of(String zoneId) {
        return zoneRegions.getOrDefault(zoneId, "");
    }

    public boolean isReleased(String region) {
        return region != null && released.contains(region.toLowerCase());
    }

    public boolean isZoneReleased(String zoneId) {
        return isReleased(of(zoneId));
    }

    // The released zones in zones.csv order
    public List<String> releasedZones() {
        return zoneRegions.keySet().stream().filter(this::isZoneReleased).toList();
    }

    public List<String> zonesOf(String region) {
        return zoneRegions.entrySet().stream().filter(entry -> entry.getValue().equals(region))
                .map(Map.Entry::getKey).toList();
    }

    // The region a CSV region column means: the region itself (gormak) or one of its
    // zones (gormak_city). Null for one that is no region, like fishing
    public String fromColumn(String column) {
        if (column == null || column.isBlank()) return null;
        String best = null;
        for (String region : zoneRegions.values()) {
            if ((column.equals(region) || column.startsWith(region + "_")) && (best == null || region.length() > best.length())) {
                best = region;
            }
        }
        return best;
    }

    // Something from a CSV region column players are shown: no region at all, or a
    // released one. A column that names no region, like fishing, is switched off
    public boolean isColumnReleased(String column) {
        if (column == null || column.isBlank()) return true;
        return isReleased(fromColumn(column));
    }
}
