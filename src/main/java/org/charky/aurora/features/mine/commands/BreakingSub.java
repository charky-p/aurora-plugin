package org.charky.aurora.features.mine.commands;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.charky.aurora.common.util.TextUtils;
import org.charky.aurora.core.Aurora;
import org.charky.aurora.features.mine.MineManager;
import org.charky.aurora.features.system.commands.AuroraSubCommand;

import java.util.List;

/**
 * Turns an admin's real block breaking off and on. Off, they mine like a player and
 * break nothing outside a mine, until they turn it back on or the server restarts.
 * On its own it switches to the other one
 */
public class BreakingSub implements AuroraSubCommand {
    private final Aurora plugin;

    public BreakingSub(Aurora plugin) {
        this.plugin = plugin;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            TextUtils.error(sender, "Only players break blocks");
            return;
        }
        MineManager mines = plugin.getMineManager();
        boolean on;
        if (args.length == 0) {
            on = !mines.breaksForReal(player);
        } else if (args[0].equalsIgnoreCase("on")) {
            on = true;
        } else if (args[0].equalsIgnoreCase("off")) {
            on = false;
        } else {
            TextUtils.usage(sender, getFullUsage());
            return;
        }
        mines.setBreaking(player.getUniqueId(), on);
        TextUtils.success(player, on
                ? "Breaking is on: you break blocks for real outside mines"
                : "Breaking is off: you mine like a player and break nothing outside mines");
    }

    @Override
    public List<String> tabComplete(Player player, String[] args) {
        if (args.length != 1) return List.of();
        return TextUtils.getCompletions(args[0], List.of("on", "off"));
    }

    @Override
    public String getUsage() {
        return "[on|off]";
    }

    @Override
    public String getName() {
        return "breaking";
    }

    @Override
    public String getDescription() {
        return "Turn your real block breaking outside mines off or on";
    }
}
