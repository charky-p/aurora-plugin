package org.charky.aurora.features.keys;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.charky.aurora.common.util.TextUtils;
import org.charky.aurora.core.Aurora;
import org.charky.aurora.features.system.commands.AuroraSubCommand;

import java.util.List;
import java.util.Set;

/**
 * Aurora admin command for where a player may go: what their passes have opened,
 * and giving or taking an access id by hand for testing
 */
public class AccessSub implements AuroraSubCommand {
    private static final List<String> ACTIONS = List.of("give", "take");

    private final Aurora plugin;

    public AccessSub(Aurora plugin) {
        this.plugin = plugin;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length < 1) {
            TextUtils.usage(sender, getFullUsage());
            return;
        }
        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) {
            TextUtils.error(sender, "Player not found: " + args[0]);
            return;
        }
        AccessManager access = plugin.getAccessManager();

        if (args.length == 1) {
            Set<String> ids = access.of(target.getUniqueId());
            TextUtils.display(sender, "<aqua>" + target.getName() + " <gray>can go to: <white>"
                    + (ids.isEmpty() ? "<gray>nowhere locked yet" : String.join(", ", ids)));
            return;
        }
        if (args.length < 3 || !ACTIONS.contains(args[1].toLowerCase())) {
            TextUtils.usage(sender, getFullUsage());
            return;
        }

        String id = args[2].toLowerCase();
        if (args[1].equalsIgnoreCase("give")) {
            if (!plugin.getRegistry().getUnlockTags().contains(id)) {
                TextUtils.error(sender, "No pass opens '" + id + "', known: " + String.join(", ", plugin.getRegistry().getUnlockTags()));
                return;
            }
            if (access.grant(target.getUniqueId(), id)) TextUtils.success(sender, target.getName() + " can now go to " + id);
            else TextUtils.error(sender, target.getName() + " could already go to " + id);
        } else {
            if (access.revoke(target.getUniqueId(), id)) TextUtils.success(sender, target.getName() + " can no longer go to " + id);
            else TextUtils.error(sender, target.getName() + " could not go to " + id);
        }
    }

    @Override
    public List<String> tabComplete(Player player, String[] args) {
        if (args.length == 1) return TextUtils.playerCompletions(args[0]);
        if (args.length == 2) return TextUtils.getCompletions(args[1], ACTIONS);
        if (args.length == 3) return TextUtils.getCompletions(args[2], plugin.getRegistry().getUnlockTags());
        return List.of();
    }

    @Override
    public String getUsage() {
        return "<player> [give|take <access>]";
    }

    @Override
    public String getName() {
        return "access";
    }

    @Override
    public String getDescription() {
        return "Where a player may go: list it, or give or take an access id like gormak";
    }
}
