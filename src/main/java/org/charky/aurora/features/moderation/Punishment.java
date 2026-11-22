package org.charky.aurora.features.moderation;

import java.util.UUID;

// A mute, ban or kick and who gave it. until is 0 for one with no end, and for a kick
public record Punishment(int id, UUID uuid, String name, Type type, String reason, String by, long created,
                         long until, boolean lifted) {

    public enum Type {
        MUTE, BAN, KICK
    }

    public boolean forever() {
        return until == 0;
    }

    // Still holds at this time: not lifted, and not run out
    public boolean activeAt(long now) {
        return type != Type.KICK && !lifted && (forever() || now < until);
    }
}
