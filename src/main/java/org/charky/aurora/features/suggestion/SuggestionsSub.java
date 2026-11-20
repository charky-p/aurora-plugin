package org.charky.aurora.features.suggestion;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.charky.aurora.common.util.Pager;
import org.charky.aurora.common.util.TextUtils;
import org.charky.aurora.core.Aurora;
import org.charky.aurora.features.system.commands.AuroraSubCommand;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Reads what players sent with /suggest, newest first, ten to a page. Clicking a
 * number puts the command to delete it in chat, for one that has been dealt with
 */
public class SuggestionsSub implements AuroraSubCommand {
    private static final DateTimeFormatter WHEN = DateTimeFormatter.ofPattern("d MMM HH:mm").withZone(ZoneId.systemDefault());

    private final Aurora plugin;

    public SuggestionsSub(Aurora plugin) {
        this.plugin = plugin;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        SuggestionManager suggestions = plugin.getSuggestionManager();
        if (args.length > 0 && args[0].equalsIgnoreCase("delete")) {
            delete(sender, suggestions, args);
            return;
        }

        List<Suggestion> all = suggestions.newestFirst();
        if (all.isEmpty()) {
            TextUtils.send(sender, "<gray>No suggestions yet");
            return;
        }
        List<Suggestion> page = Pager.open(sender, args, all, "SUGGESTIONS", 26);
        if (page == null) return;

        for (Suggestion suggestion : page) {
            // What the player wrote is plain text, never read as colours or click events
            sender.sendMessage(TextUtils.parse("<click:suggest_command:'/aurora suggestions delete " + suggestion.id() + "'>"
                            + "<hover:show_text:'<gray>Click to delete'><dark_gray>#" + suggestion.id() + "</hover></click> "
                            + "<gray>" + WHEN.format(Instant.ofEpochMilli(suggestion.time())) + " ")
                    .append(Component.text(suggestion.name(), NamedTextColor.WHITE))
                    .append(Component.text(": " + suggestion.text(), NamedTextColor.GRAY)));
        }
    }

    private void delete(CommandSender sender, SuggestionManager suggestions, String[] args) {
        if (args.length < 2) {
            TextUtils.usage(sender, "/aurora suggestions delete <id>");
            return;
        }
        int id;
        try {
            id = Integer.parseInt(args[1].replace("#", ""));
        } catch (NumberFormatException e) {
            TextUtils.error(sender, "Not a suggestion number: " + args[1]);
            return;
        }
        if (suggestions.delete(id)) TextUtils.success(sender, "Suggestion #" + id + " deleted");
        else TextUtils.error(sender, "No suggestion #" + id);
    }

    @Override
    public List<String> tabComplete(Player player, String[] args) {
        if (args.length == 1) return TextUtils.getCompletions(args[0], List.of("delete"));
        if (args.length == 2 && args[0].equalsIgnoreCase("delete")) {
            return TextUtils.getCompletions(args[1], plugin.getSuggestionManager().newestFirst().stream()
                    .map(suggestion -> String.valueOf(suggestion.id())).toList());
        }
        return List.of();
    }

    @Override
    public String getUsage() {
        return "[page] | delete <id>";
    }

    @Override
    public String getName() {
        return "suggestions";
    }

    @Override
    public String getDescription() {
        return "Read what players sent with /suggest, newest first, and delete ones dealt with";
    }
}
