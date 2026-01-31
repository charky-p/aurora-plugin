package org.charky.aurora.commands.mine;

import org.bukkit.Axis;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.charky.aurora.Aurora;
import org.charky.aurora.DataRegistry;
import org.charky.aurora.commands.AuroraSubCommand;
import org.charky.aurora.managers.MineManager;

public abstract class MineSub implements AuroraSubCommand {
    protected final MineManager mineManager;
    protected final DataRegistry registry;

    public MineSub(Aurora plugin) {
        this.mineManager = plugin.getMineManager();
        this.registry = plugin.getRegistry();
    }

    protected int parseCoordinate(CommandSender sender, String input, Axis axis) throws NumberFormatException {
        // Handle ~ as current coordinate
        if (input.startsWith("~")) {
            if (!(sender instanceof Player player)) {
                throw new NumberFormatException("Relative coordinates (~) cannot be used from console");
            }

            Location location = player.getLocation();
            int origin = switch (axis) {
                case Axis.X -> location.getBlockX();
                case Axis.Y -> location.getBlockY();
                case Axis.Z -> location.getBlockZ();
            };

            if (input.length() == 1) return origin;

            // Handle ~+5, ~-5, ~5
            String offset = input.substring(1);
            if (offset.startsWith("+")) offset = offset.substring(1);
            return origin + Integer.parseInt(offset);
        }
        return Integer.parseInt(input);
    }

    @Override
    public String getFullUsage() {
        return "/aurora mine " + getName() + " " + getUsage();
    }
}
