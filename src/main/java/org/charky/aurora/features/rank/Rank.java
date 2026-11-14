package org.charky.aurora.features.rank;

import net.kyori.adventure.text.Component;
import org.charky.aurora.common.util.TextUtils;

// A name shown beside a player's own, cosmetic only. The requirement is seconds
// played for a playtime rank and Essence for a shop rank
public record Rank(String id, String display, Kind kind, long requirement) {
    public enum Kind {
        // What everyone starts with
        DEFAULT,
        // Unlocked by time online
        PLAYTIME,
        // Bought with Essence in /shop
        SHOP
    }

    // The bold, coloured name
    public Component component() {
        return TextUtils.parse(display);
    }
}
