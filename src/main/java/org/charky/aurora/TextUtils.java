package org.charky.aurora;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.charky.aurora.models.MessageType;

public class TextUtils {
    public static final Component PREFIX = Component.text()
            .append(Component.text("[", NamedTextColor.GRAY))
            .append(Component.text("AURORA", NamedTextColor.AQUA, TextDecoration.BOLD))
            .append(Component.text("]", NamedTextColor.GRAY))
            .append(Component.space())
            .build();

    public static Component format(String message) {
        return Component.text().append(PREFIX).append(Component.text(message, NamedTextColor.WHITE)).build();
    }

    public static Component format(String message, MessageType type) {
        return Component.text().append(PREFIX).append(Component.text(message, type.getColor())).build();
    }
}
