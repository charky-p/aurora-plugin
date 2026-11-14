package org.charky.aurora.features.system.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.charky.aurora.common.util.TextUtils;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

// A player command that only opens a menu: /shop, /ranks, /collections
public class OpenMenuCommand implements CommandExecutor {
    private final Consumer<Player> open;

    public OpenMenuCommand(Consumer<Player> open) {
        this.open = open;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String @NotNull [] args) {
        if (!(sender instanceof Player player)) {
            TextUtils.error(sender, "Players only.");
            return true;
        }

        open.accept(player);
        return true;
    }
}
