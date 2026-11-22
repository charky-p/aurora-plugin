package org.charky.aurora.features.loadout;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

// Which of a player's loadouts is in play. The experimental one is for trying the game in
// the test worlds from a fresh start, without touching the real one. Slots 2 and 3 are
// extra main-world loadouts for admins, picked with /aurora loadout, never on a leaderboard
public enum Loadout {
    MAIN("main", "1"),
    EXPERIMENTAL("experimental", "experimental"),
    SLOT_2("slot2", "2"),
    SLOT_3("slot3", "3");

    // Every test world scripts/build_test_world.py makes starts with this
    public static final String TEST_WORLD_PREFIX = "aurora_test";

    // Kept in the database, never change one
    private final String id;
    // What /aurora loadout calls it
    private final String label;

    Loadout(String id, String label) {
        this.id = id;
        this.label = label;
    }

    // The loadout a world plays with
    public static Loadout of(String worldName) {
        return worldName.toLowerCase().startsWith(TEST_WORLD_PREFIX) ? EXPERIMENTAL : MAIN;
    }

    // The id the experimental loadout is put away under. From before there were more than
    // two, when it held whichever one was put away
    public static UUID shadowOf(UUID uuid) {
        return UUID.nameUUIDFromBytes(("aurora-loadout:" + uuid).getBytes(StandardCharsets.UTF_8));
    }

    // The id this loadout is kept under while it is put away, always the same and never
    // another loadout's. The one in play is under the player's own id
    public UUID homeOf(UUID uuid) {
        if (this == EXPERIMENTAL) return shadowOf(uuid);
        return UUID.nameUUIDFromBytes(("aurora-loadout:" + id + ":" + uuid).getBytes(StandardCharsets.UTF_8));
    }

    public String id() {
        return id;
    }

    public String label() {
        return label;
    }

    // Plays in the main worlds: everything but experimental
    public boolean isMainWorld() {
        return this != EXPERIMENTAL;
    }

    // MAIN for anything unknown
    public static Loadout byId(String id) {
        for (Loadout loadout : values()) {
            if (loadout.id.equalsIgnoreCase(id)) return loadout;
        }
        return MAIN;
    }

    // By id or label, main or 1, null for anything else
    public static Loadout byName(String name) {
        for (Loadout loadout : values()) {
            if (loadout.id.equalsIgnoreCase(name) || loadout.label.equalsIgnoreCase(name)) return loadout;
        }
        return null;
    }
}
