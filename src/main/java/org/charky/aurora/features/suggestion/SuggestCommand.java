package org.charky.aurora.features.suggestion;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.charky.aurora.common.util.TextUtils;
import org.jetbrains.annotations.NotNull;

import java.util.List;

// /suggest <text>, sends an idea to the staff
public class SuggestCommand implements CommandExecutor, TabCompleter {
    private final SuggestionManager suggestions;

    public SuggestCommand(SuggestionManager suggestions) {
        this.suggestions = suggestions;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String @NotNull [] args) {
        if (!(sender instanceof Player player)) {
            TextUtils.error(sender, "Only players can send suggestions");
            return true;
        }
        if (args.length == 0) {
            TextUtils.usage(sender, "/suggest <your idea>");
            return true;
        }

        String problem = suggestions.suggest(player, String.join(" ", args));
        if (problem != null) TextUtils.error(player, problem);
        else TextUtils.success(player, "Thanks! Your suggestion was sent to the staff");
        return true;
    }

    // Nothing to complete, rather than player names
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                      @NotNull String label, @NotNull String @NotNull [] args) {
        return List.of();
    }
}
