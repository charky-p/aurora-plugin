package org.charky.aurora.features.cosmetic;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.charky.aurora.common.util.TextUtils;

// One thing a player can show off, chosen in /customise. Cosmetics change how a
// player looks to others and nothing about how the game plays.
// The requirement is seconds played for PLAYTIME and Essence for ESSENCE. The value
// is what the cosmetic is made of: a colour name, a particle, a join message, the
// name of a trim pattern. The target is the zone of a ZONE cosmetic and the region
// of a WORLD one, null for the rest
public record Cosmetic(String id, Type type, String display, Material icon, Source source,
                       long requirement, String note, String value, String target) {
    public enum Type {
        SUFFIX("Suffixes", "Shown after your name in chat"),
        CHAT_COLOR("Chat Colours", "The colour of what you say"),
        NAME_COLOR("Name Colours", "The colour of your name in chat and the tab list"),
        JOIN_MESSAGE("Join Messages", "What everyone reads when you join"),
        TRAIL("Trails", "Particles that follow you as you walk"),
        KILL_EFFECT("Kill Effects", "What happens where a mob you kill falls"),
        DEATH_MESSAGE("Death Messages", "What everyone reads when you die"),
        TRIM_PATTERN("Armour Trims", "The pattern on the armour you wear"),
        TRIM_MATERIAL("Trim Colours", "What your armour trim is made of");

        private final String title;
        private final String description;

        Type(String title, String description) {
            this.title = title;
            this.description = description;
        }

        public String title() { return title; }
        public String description() { return description; }
    }

    public enum Source {
        // Everyone has it
        DEFAULT,
        // Unlocked by time online
        PLAYTIME,
        // Earned by buying every upgrade a zone sells
        ZONE,
        // Earned by doing that in every zone of a world
        WORLD,
        // Bought with Essence in /customise
        ESSENCE,
        // Handed out by an admin, for donors and events
        GRANT
    }

    // Where a cosmetic is worn. Suffixes have two places, everything else one
    public enum Slot {
        SUFFIX_1("suffix_1", Type.SUFFIX),
        SUFFIX_2("suffix_2", Type.SUFFIX),
        CHAT_COLOR("chat_color", Type.CHAT_COLOR),
        NAME_COLOR("name_color", Type.NAME_COLOR),
        JOIN_MESSAGE("join_message", Type.JOIN_MESSAGE),
        TRAIL("trail", Type.TRAIL),
        KILL_EFFECT("kill_effect", Type.KILL_EFFECT),
        DEATH_MESSAGE("death_message", Type.DEATH_MESSAGE),
        TRIM_PATTERN("trim_pattern", Type.TRIM_PATTERN),
        TRIM_MATERIAL("trim_material", Type.TRIM_MATERIAL);

        private final String id;
        private final Type type;

        Slot(String id, Type type) {
            this.id = id;
            this.type = type;
        }

        public String id() { return id; }
        public Type type() { return type; }

        // The first slot of a type, the only one for everything but suffixes
        public static Slot of(Type type) {
            for (Slot slot : values()) {
                if (slot.type == type) return slot;
            }
            throw new IllegalArgumentException("No slot for " + type);
        }
    }

    public Component component() {
        return TextUtils.parse(display);
    }
}
