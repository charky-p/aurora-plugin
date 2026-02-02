package org.charky.aurora.commands.mine;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.charky.aurora.Aurora;
import org.charky.aurora.util.TextUtils;

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
        if (!mineManager.exists(mineName)) {
            TextUtils.error(sender, "Mine '" + mineName + "' does not exist!");
            return;
        }

        Player target;
        if (args.length >= 2) {
            target = Bukkit.getPlayer(args[1]);
            if (target == null) {
                TextUtils.error(sender, "Player '" + args[1] + "' is not online");
                return;
            }
        } else {
            if (!(sender instanceof Player player)) {
                TextUtils.error(sender, "Console must specify a player to teleport");
                return;
            }
            target = player;
        }

        mineManager.tpMine(target, mineName);
        TextUtils.success(target, "Teleported to " + mineName + " successfully");
        if (target != sender) {
            TextUtils.success(sender, "Teleported player '" + target.getName() + "' to '" + mineName + "' successfully");
        }
    }

    @Override
    public List<String> tabComplete(Player player, String[] args) {
        if (args.length == 1) {
            return mineManager.getMines()
                    .stream()
                    .filter(name -> name.toLowerCase().startsWith(args[0].toLowerCase()))
                    .sorted()
                    .toList();
        } else if (args.length == 2) {
            return Bukkit.getOnlinePlayers()
                    .stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(args[1].toLowerCase()))
                    .toList();
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
