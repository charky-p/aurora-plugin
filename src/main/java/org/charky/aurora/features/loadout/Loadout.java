package org.charky.aurora.features.loadout;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

// A player's loadouts. Only MAIN is played now: experimental (the old test worlds) and the
// admin slots 2 and 3 are kept so whoever still has one in play is switched back to main,
// and so their put-away rows stay off the leaderboards
public enum Loadout {
    MAIN("main"),
    EXPERIMENTAL("experimental"),
    SLOT_2("slot2"),
    SLOT_3("slot3");

    // Kept in the database, never change one
    private final String id;

    Loadout(String id) {
        this.id = id;
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

    // MAIN for anything unknown
    public static Loadout byId(String id) {
        for (Loadout loadout : values()) {
            if (loadout.id.equalsIgnoreCase(id)) return loadout;
        }
        return MAIN;
    }
}
