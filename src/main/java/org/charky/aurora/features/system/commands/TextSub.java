package org.charky.aurora.features.system.commands;

import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.persistence.PersistentDataType;
import org.charky.aurora.common.util.TextUtils;
import org.charky.aurora.core.Aurora;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/**
 * Floating text, like "PvP zone" over a gate. It is a text display entity saved with
 * the world, so it goes wherever the world goes, deploys included
 */
public class TextSub implements AuroraSubCommand {
    private static final List<String> ACTIONS = List.of("add", "remove", "list");
    // How far /aurora text remove and list look
    private static final double REACH = 8;

    private final NamespacedKey key;

    public TextSub(Aurora plugin) {
        this.key = new NamespacedKey(plugin, "floating_text");
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            TextUtils.error(sender, "Floating text is placed where you stand, in game");
            return;
        }
        if (args.length == 0 || !ACTIONS.contains(args[0].toLowerCase())) {
            TextUtils.usage(sender, getFullUsage());
            return;
        }

        switch (args[0].toLowerCase()) {
            case "add" -> add(player, args);
            case "remove" -> remove(player);
            case "list" -> list(player);
        }
    }

    // At eye height where the player stands, MiniMessage colours and \n for a new line
    private void add(Player player, String[] args) {
        if (args.length < 2) {
            TextUtils.usage(player, "/aurora text add <text>, MiniMessage colours, \\n for a new line");
            return;
        }
        String raw = String.join(" ", Arrays.copyOfRange(args, 1, args.length)).replace("\\n", "<newline>");
        Location at = player.getEyeLocation();
        player.getWorld().spawn(at, TextDisplay.class, display -> {
            display.text(TextUtils.parse(raw));
            display.setBillboard(Display.Billboard.CENTER);
            display.setPersistent(true);
            display.getPersistentDataContainer().set(key, PersistentDataType.STRING, raw);
        });
        TextUtils.success(player, "Floating text placed, /aurora text remove takes away the nearest one");
    }

    private void remove(Player player) {
        TextDisplay nearest = nearby(player).stream().findFirst().orElse(null);
        if (nearest == null) {
            TextUtils.error(player, "No floating text within " + (int) REACH + " blocks");
            return;
        }
        String raw = nearest.getPersistentDataContainer().get(key, PersistentDataType.STRING);
        nearest.remove();
        TextUtils.success(player, "Removed: " + raw);
    }

    private void list(Player player) {
        List<TextDisplay> texts = nearby(player);
        TextUtils.display(player, "<aqua><b>FLOATING TEXT</b> <gray>within " + (int) REACH + " blocks");
        for (TextDisplay text : texts) {
            Location at = text.getLocation();
            TextUtils.display(player, " <dark_gray>» <white>" + text.getPersistentDataContainer().get(key, PersistentDataType.STRING)
                    + " <gray>" + at.getBlockX() + " " + at.getBlockY() + " " + at.getBlockZ());
        }
    }

    // Only text placed with this command, nearest first
    private List<TextDisplay> nearby(Player player) {
        Location at = player.getLocation();
        return player.getNearbyEntities(REACH, REACH, REACH).stream()
                .filter(entity -> entity instanceof TextDisplay
                        && entity.getPersistentDataContainer().has(key, PersistentDataType.STRING))
                .map(entity -> (TextDisplay) entity)
                .sorted(Comparator.comparingDouble((Entity entity) -> entity.getLocation().distanceSquared(at)))
                .toList();
    }

    @Override
    public List<String> tabComplete(Player player, String[] args) {
        if (args.length == 1) return TextUtils.getCompletions(args[0], ACTIONS);
        return List.of();
    }

    @Override
    public String getUsage() {
        return "<add <text>|remove|list>";
    }

    @Override
    public String getName() {
        return "text";
    }

    @Override
    public String getDescription() {
        return "Floating text where you stand, MiniMessage colours, \\n for a new line";
    }
}
