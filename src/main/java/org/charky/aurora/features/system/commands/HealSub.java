package org.charky.aurora.features.system.commands;

import org.bukkit.Bukkit;
import org.bukkit.attribute.Attribute;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.charky.aurora.common.util.TextUtils;

import java.util.List;

/**
 * Aurora admin command to restore health and hunger and put out fire, for testing combat
 */
public class HealSub implements AuroraSubCommand {
    @Override
    public void execute(CommandSender sender, String[] args) {
        Player target;
        if (args.length > 0) {
            target = Bukkit.getPlayerExact(args[0]);
            if (target == null) {
                TextUtils.error(sender, "Player not found: " + args[0]);
                return;
            }
        } else if (sender instanceof Player player) {
            target = player;
        } else {
            TextUtils.usage(sender, getFullUsage());
            return;
        }

        target.setHealth(target.getAttribute(Attribute.MAX_HEALTH).getValue());
        target.setFoodLevel(20);
        target.setFireTicks(0);

        TextUtils.success(target, "You have been fully healed");
        if (target != sender) TextUtils.success(sender, "Healed " + target.getName());
    }

    @Override
    public List<String> tabComplete(Player player, String[] args) {
        if (args.length != 1) return List.of();
        return TextUtils.getCompletions(args[0], Bukkit.getOnlinePlayers().stream().map(Player::getName).toList());
    }

    @Override
    public String getUsage() {
        return "[player]";
    }

    @Override
    public String getName() {
        return "heal";
    }

    @Override
    public String getDescription() {
        return "Restore health and hunger and put out fire";
    }
}
