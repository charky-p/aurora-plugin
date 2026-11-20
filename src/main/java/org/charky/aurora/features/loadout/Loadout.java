package org.charky.aurora.features.loadout;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

// Which of a player's two loadouts is in play. The experimental one is for trying the
// game in the test worlds from a fresh start, without touching the real one
public enum Loadout {
    MAIN,
    EXPERIMENTAL;

    // Every test world scripts/build_test_world.py makes starts with this
    public static final String TEST_WORLD_PREFIX = "aurora_test";

    // The loadout a world plays with
    public static Loadout of(String worldName) {
        return worldName.toLowerCase().startsWith(TEST_WORLD_PREFIX) ? EXPERIMENTAL : MAIN;
    }

    // The id a player's other loadout is kept under while it is put away, always the same
    public static UUID shadowOf(UUID uuid) {
        return UUID.nameUUIDFromBytes(("aurora-loadout:" + uuid).getBytes(StandardCharsets.UTF_8));
    }

    public String id() {
        return name().toLowerCase();
    }

    // MAIN for anything unknown
    public static Loadout byId(String id) {
        for (Loadout loadout : values()) {
            if (loadout.id().equalsIgnoreCase(id)) return loadout;
        }
        return MAIN;
    }
}
