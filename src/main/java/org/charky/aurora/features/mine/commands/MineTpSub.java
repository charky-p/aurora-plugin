package org.charky.aurora.features.mine.commands;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.charky.aurora.core.Aurora;
import org.charky.aurora.features.system.commands.AuroraSubCommand;
import org.charky.aurora.common.util.TextUtils;

import java.util.Collections;
import java.util.List;

/**
 * Mine sub command to teleport a player to a mine tp location
 */
public class MineTpSub extends MineSub {
    public MineTpSub(Aurora plugin) {
        super(plugin);
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length < 1) {
            TextUtils.usage(sender, getFullUsage());
            return;
        }

        String mineName = args[0];
        if (find(sender, mineName) == null) return;

        Player target = AuroraSubCommand.targetPlayer(sender, args, 1);
        if (target == null) return;

        mineManager.tpMine(target, mineName);
        TextUtils.success(target, "Teleported to " + mineName + " successfully");
        if (target != sender) {
            TextUtils.success(sender, "Teleported player '" + target.getName() + "' to '" + mineName + "' successfully");
        }
    }

    @Override
    public List<String> tabComplete(Player player, String[] args) {
        if (args.length == 1) return TextUtils.getCompletions(args[0], mineManager.getMines());
        else if (args.length == 2) {
            return TextUtils.playerCompletions(args[1]);
        }

        return Collections.emptyList();
    }

    @Override
    public String getUsage() {
        return "<mine> [player]";
    }

    @Override
    public String getName() {
        return "tp";
    }

    @Override
    public String getDescription() {
        return "Teleports yourself or another player to a mine";
    }
}
