package org.charky.aurora.features.tutorial;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.charky.aurora.common.util.TextUtils;
import org.jetbrains.annotations.NotNull;

// /tutorial skip, for players who know the game already. The Skip link in chat runs it
public class TutorialCommand implements CommandExecutor {
    private final TutorialManager tutorial;

    public TutorialCommand(TutorialManager tutorial) {
        this.tutorial = tutorial;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String @NotNull [] args) {
        if (!(sender instanceof Player player)) return false;
        if (args.length < 1 || !args[0].equalsIgnoreCase("skip")) {
            TextUtils.usage(sender, "/tutorial skip");
            return true;
        }
        if (!tutorial.skip(player)) TextUtils.error(sender, "You are not in the tutorial");
        return true;
    }
}
