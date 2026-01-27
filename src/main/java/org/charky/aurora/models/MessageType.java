package org.charky.aurora.models;

import net.kyori.adventure.text.format.NamedTextColor;

public enum MessageType {
    SUCCESS(NamedTextColor.GREEN),
    ERROR(NamedTextColor.RED),
    WARNING(NamedTextColor.YELLOW);

    private final NamedTextColor color;
    MessageType(NamedTextColor color) {
        this.color = color;
    }

    public NamedTextColor getColor() {
        return this.color;
    }
}
