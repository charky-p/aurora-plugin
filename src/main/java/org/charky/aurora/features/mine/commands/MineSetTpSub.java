package org.charky.aurora.features.mine.commands;

import org.bukkit.Axis;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.charky.aurora.common.util.LocationUtils;
import org.charky.aurora.core.Aurora;
import org.charky.aurora.common.util.TextUtils;

import java.util.Collections;
import java.util.List;

/**
 * Mine sub command to set the teleport point of a mine, used for tp and mine reset teleports
 * TODO: @NotNull for these sub commands
 */
public class MineSetTpSub extends MineSub {
    public MineSetTpSub(Aurora plugin) {
        super(plugin);
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length < 4) {
            TextUtils.usage(sender, getFullUsage());
            return;
        }

        String mineName = args[0];
        if (!mineManager.exists(mineName)) {
            TextUtils.error(sender, "Mine '" + mineName + "' does not exist");
            return;
        }

        try {
            double x = LocationUtils.parseCoordinate(sender, args[1], Axis.X);
            double y = LocationUtils.parseCoordinate(sender, args[2], Axis.Y);
            double z = LocationUtils.parseCoordinate(sender, args[3], Axis.Z);

            mineManager.setTp(mineName, x, y, z);
            TextUtils.success(sender, "Set teleport point of '" + mineName + "' to [" + x + ", " + y + ", " + z + "]");
        } catch (NumberFormatException e) {
            TextUtils.error(sender, "Invalid coordinates: " + e.getMessage());
        }
    }

    @Override
    public List<String> tabComplete(Player player, String[] args) {
        if (args.length == 1) return TextUtils.getCompletions(args[0], mineManager.getMines());

        return Collections.emptyList();
    }

    @Override
    public String getUsage() {
        return "<mine> <x> <y> <z>";
    }

    @Override
    public String getName() {
        return "settp";
    }

    @Override
    public String getDescription() {
        return "Sets the teleport point of a mine for mine resets or teleporting to a mine";
    }
}
