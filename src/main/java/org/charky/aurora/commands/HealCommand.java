package org.charky.aurora.commands;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.attribute.Attribute;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.charky.aurora.util.MessageType;
import org.charky.aurora.util.TextUtils;
import org.jetbrains.annotations.NotNull;

public class HealCommand implements CommandExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String s, @NotNull String @NotNull [] args) {
        if (!(sender instanceof Player player)) return true;

        Player target = player;
        if (args.length > 0) {
            target = Bukkit.getPlayer(args[0]);
            if (target == null) {
                player.sendMessage(Component.text("Player not found!").color(NamedTextColor.GOLD));
                return true;
            }
        }

        double maxHealth = target.getAttribute(Attribute.MAX_HEALTH).getValue();
        target.setHealth(maxHealth);
        target.setFoodLevel(20);
        target.setFireTicks(0);

        // TODO: Has aurora tag, maybe remove
        TextUtils.format(target, "You have been fully healed!", MessageType.SUCCESS);
        if (target != player) {
            TextUtils.format(player, "Healed " + target.getName(), MessageType.SUCCESS);
        }

        return true;
    }
}
