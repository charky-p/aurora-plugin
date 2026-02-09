package org.charky.aurora.common.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.CommandSender;

import java.util.Collection;
import java.util.List;

public class TextUtils {
    private static final String NAME ="<gradient:#7F7FD5:#91EAE4><b>AURORA</b></gradient>";
    private static final String SERVER = "<gradient:#7F7FD5:#91EAE4><bold>AURORAMINES</bold></gradient>";
    private static final String PREFIX = "<gray>[" + NAME + "<gray>] <white>";
    private static final MiniMessage mm = MiniMessage.miniMessage();

    // Stop instances
    private TextUtils() {}

    public static Component getServer() {
        return parse(SERVER);
    }

    public static Component parse(String message) {
        return mm.deserialize(message);
    }

    // Proper command with prefix
    public static void send(CommandSender sender, String message) {
        sender.sendMessage(parse(PREFIX + message));
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

    public static List<String> getCompletions(String input, Collection<String> options) {
        return options
                .stream()
                .filter(name -> name.toLowerCase().startsWith(input.toLowerCase()))
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();
    }
}
