package org.charky.aurora.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.CommandSender;

public class TextUtils {
    public static final Component PREFIX = Component.text()
            .append(Component.text("[", NamedTextColor.GRAY))
            .append(Component.text("AURORA", NamedTextColor.AQUA, TextDecoration.BOLD))
            .append(Component.text("]", NamedTextColor.GRAY))
            .append(Component.space())
            .build();

    private static final String prefix = "<gray>[<aqua><b>AURORA</b><gray>] <white>";
    private static final MiniMessage mm = MiniMessage.miniMessage();

    private TextUtils() {}
    @Deprecated
    public static Component format(String message) {
        return Component.text().append(PREFIX).append(Component.text(message, NamedTextColor.WHITE)).build();
    }

    @Deprecated
    public static Component format(String message, MessageType type) {
        return Component.text().append(PREFIX).append(Component.text(message, type.getColor())).build();
    }

    @Deprecated
    public static void format(CommandSender sender, String message, MessageType type) {
        sender.sendMessage(format(message, type));
    }

    public static Component parse(String message) {
        return mm.deserialize(message);
    }

    // Proper command with prefix
    public static void send(CommandSender sender, String message) {
        sender.sendMessage(parse(prefix + message));
    }

    // Display without prefix
    public static void display(CommandSender sender, String message) {
        sender.sendMessage(parse(message));
    }

    // Usage prefix
    public static void usage(CommandSender sender, String usage) {
        send(sender, "<red>Usage: " + usage);
    }

    // TODO: Add @NotNull?
    public static void error(CommandSender sender, String error) {
        send(sender, "<red>" + error);
    }

    public static void success(CommandSender sender, String success) {
        send(sender, "<green>" + success);
    }

    public static void warn(CommandSender sender, String warning) {
        send(sender, "<yellow>" + warning);
    }

    public static void line(CommandSender sender, String color) {
        display(sender, color + "<st>                                                                                 </st>");
    }
}
