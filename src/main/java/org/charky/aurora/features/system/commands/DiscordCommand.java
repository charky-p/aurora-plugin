package org.charky.aurora.features.system.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.charky.aurora.common.util.TextUtils;
import org.jetbrains.annotations.NotNull;

// /discord, a link to the server's Discord to click
public class DiscordCommand implements CommandExecutor {
    private static final String INVITE = "https://discord.gg/MefQWSgkBA";

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String @NotNull [] args) {
        TextUtils.send(sender, "<gray>Join the Discord: <click:open_url:'" + INVITE + "'>"
                + "<hover:show_text:'<gray>Open the invite'><aqua><u>" + INVITE + "</u></aqua></hover></click>");
        return true;
    }
}
