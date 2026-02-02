package org.charky.aurora.features.system.commands;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.attribute.Attribute;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.charky.aurora.common.util.TextUtils;
import org.jetbrains.annotations.NotNull;

/**
 * Temporary heal command
 * TODO: [DELETE] either remove this command or add as a helper command in aurora
 */
@Deprecated
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
        TextUtils.display(target, "<aqua>You have been fully healed!");
        if (target != player) {
            TextUtils.display(player, "<aqua>Healed " + target.getName());
        }

        return true;
    }
}
