package org.charky.aurora.features.system.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.charky.aurora.common.util.TextUtils;
import org.jetbrains.annotations.NotNull;

// /discord, a link to the server's Discord to click
public class DiscordCommand implements CommandExecutor {
    // TODO: the invite, like https://discord.gg/abc123. While blank /discord says it is coming soon
    private static final String INVITE = "";

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String @NotNull [] args) {
        if (INVITE.isBlank()) {
            TextUtils.send(sender, "<gray>The Aurora Discord is coming soon");
            return true;
        }
        TextUtils.send(sender, "<gray>Join the Discord: <click:open_url:'" + INVITE + "'>"
                + "<hover:show_text:'<gray>Open the invite'><aqua><u>" + INVITE + "</u></aqua></hover></click>");
        return true;
    }
}
