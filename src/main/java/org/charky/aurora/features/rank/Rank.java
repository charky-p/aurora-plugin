package org.charky.aurora.features.rank;

import net.kyori.adventure.text.Component;
import org.charky.aurora.common.util.TextUtils;

// A name shown beside a player's own, cosmetic only. The requirement is seconds
// played for a playtime rank and Essence for a shop rank. The target is the zone of
// a zone rank and the region of a world rank, null for the rest
public record Rank(String id, String display, Kind kind, long requirement, String target) {
    public enum Kind {
        // What everyone starts with
        DEFAULT,
        // Unlocked by time online
        PLAYTIME,
        // Earned by buying every upgrade a zone sells
        ZONE,
        // Earned by doing that in every zone of a world
        WORLD,
        // Bought with Essence in /shop
        SHOP,
        // Handed out by an admin to donors
        DONOR,
        // Handed out by an admin to the people who run the server
        STAFF
    }

    // The bold, coloured name
    public Component component() {
        return TextUtils.parse(display);
    }
}
