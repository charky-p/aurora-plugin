package org.charky.aurora.commands;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.charky.aurora.MineManager;
import org.jetbrains.annotations.NotNull;

public class RemoveMine implements CommandExecutor {
    private final MineManager manager;

    public RemoveMine(MineManager manager) {
        this.manager = manager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String s, @NotNull String @NotNull [] args) {
        if (args.length < 1) {
            sender.sendMessage(Component.text("Usage: /remove-mine <mineName>"));
            return true;
        } else if (!manager.exists(args[0])) {
            sender.sendMessage(Component.text("Mine " + args[0] + " does not exist")
                    .color(NamedTextColor.RED));
            return true;
        }

        manager.removeMine(args[0]);
        sender.sendMessage(Component.text("Removed mine " + args[0] + " successfully!")
                .color(NamedTextColor.GREEN));
        return true;
    }
}
