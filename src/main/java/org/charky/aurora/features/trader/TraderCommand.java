package org.charky.aurora.features.trader;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.charky.aurora.core.Aurora;
import org.charky.aurora.core.commands.AuroraSubCommand;
import org.charky.aurora.common.util.TextUtils;

import java.util.List;

// TODO: adapt to get wand to remove traders
public class TraderCommand implements AuroraSubCommand {
    public TraderManager manager;

    public TraderCommand(Aurora plugin) {
        this.manager = plugin.getTraderManager();
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length < 4) {
            TextUtils.usage(sender, getFullUsage());
            return;
        }

        String traderId = args[0];
        if (!manager.traderExists(traderId)) {
            TextUtils.error(sender, "Trader '" + traderId + "' does not exist!");
            return;
        }

        try {
            double x = Double.parseDouble(args[1]);
            double y = Double.parseDouble(args[2]);
            double z = Double.parseDouble(args[3]);

            String worldName = "world";
            if (sender instanceof Player player) worldName = player.getWorld().getName();

            manager.spawnTrader(traderId, Bukkit.getWorld(worldName), x, y, z);
            TextUtils.success(sender, "Trader '" + traderId + "' successfully created");
        } catch (NumberFormatException e) {
            TextUtils.error(sender, "Invalid Coordinates: " + e.getMessage());
        }
    }

    @Override
    public List<String> tabComplete(Player player, String[] args) {
        return List.of();
    }

    @Override
    public String getUsage() {
        return "<trader> <x> <y> <z>";
    }

    @Override
    public String getName() {
        return "trader";
    }

    @Override
    public String getDescription() {
        return "Create an npc trader to interact with for shops";
    }
}
