package org.charky.aurora.common.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Axis;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

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

    @Deprecated
    public static String getFormattedTPS() {
        double tps = Bukkit.getTPS()[0]; // Get the 1-minute average

        // Color coding for the "Aurora" UI feel
        String color;
        if (tps >= 18.0) color = "<green>";
        else if (tps >= 15.0) color = "<yellow>";
        else color = "<red>";

        return String.format("%s%.2f", color, Math.min(tps, 20.0));
    }

    // TODO: Make locaitionUtils or smthin
    public static double parseCoordinate(CommandSender sender, String input, Axis axis) throws NumberFormatException {
        if (input.startsWith("~")) {
            if (!(sender instanceof Player player)) {
                throw new NumberFormatException("Relative coordinates (~) cannot be used from console");
            }

            Location location = player.getLocation();
            double base = switch (axis) {
                case X -> location.getX();
                case Y -> location.getY();
                case Z -> location.getZ();
            };

            if (input.length() == 1) return base;
            return base + Double.parseDouble(input.substring(1));
        }
        return Double.parseDouble(input);
    }
}
