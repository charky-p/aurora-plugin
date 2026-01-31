package org.charky.aurora.commands.mine;

import org.bukkit.Axis;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.charky.aurora.Aurora;
import org.charky.aurora.commands.AuroraSubCommand;
import org.charky.aurora.managers.MineManager;
import org.charky.aurora.util.TextUtils;

import java.util.Collections;
import java.util.List;

/**
 * TODO: Currently overlapping mines, decide whether this is a feature or error
 * Mine subcommand to create mines manually by coordinates.
 */
@Deprecated
public class MineCreateSub implements AuroraSubCommand {
    private final MineManager mineManager;

    public MineCreateSub(Aurora plugin) {
        this.mineManager = plugin.getMineManager();
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length < 7) {
            TextUtils.usage(sender, getFullUsage());
            return;
        }

        try {
            String name = args[0];

            if (mineManager.exists(name)) {
                TextUtils.error(sender, "Mine '" + name + "' already exists");
                return;
            }

            String worldName = (sender instanceof Player p) ? p.getWorld().getName() : "world";

            int x1 = parseCoordinate(sender, args[1], Axis.X);
            int y1 = Math.max(-64, Math.min(parseCoordinate(sender, args[2], Axis.Y), 319));
            int z1 = parseCoordinate(sender, args[3], Axis.Z);
            int x2 = parseCoordinate(sender, args[4], Axis.X);
            int y2 = Math.max(-64, Math.min(parseCoordinate(sender, args[5], Axis.Y), 319));
            int z2 = parseCoordinate(sender, args[6], Axis.Z);

            // Optional parameters, by default threshold is off (101%), delay is 1 minute
            double threshold = (args.length >= 8) ? Double.parseDouble(args[7]) : 101;
            int delay = (args.length >= 9) ? Math.max(20, Integer.parseInt(args[8])) : 1200;

            mineManager.addMine(
                    name,
                    worldName,
                    Math.min(x1, x2), Math.min(y1, y2), Math.min(z1, z2),
                    Math.max(x1, x2), Math.max(y1, y2), Math.max(z1, z2),
                    threshold,
                    delay
            );

            TextUtils.success(sender, "Mine '" + name + "' created successfully");
        } catch (NumberFormatException e) {
            TextUtils.error(sender, "Coordinates, threshold, and delay must be valid numbers");
        }
    }

    private int parseCoordinate(CommandSender sender, String input, Axis axis) throws NumberFormatException {
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
    public List<String> tabComplete(Player player, String[] args) {
        if (args.length == 1) return List.of("<name>");

        // Handle tab coordinates
        if (args.length >= 2 && args.length <= 7) {
            Block target = player.getTargetBlockExact(5);

            int coordIndex = (args.length - 2) % 3;

            String lookValue = "0";
            if (target != null) {
                lookValue = switch (coordIndex) {
                    case 0 -> String.valueOf(target.getX());
                    case 1 -> String.valueOf(target.getY());
                    default -> String.valueOf(target.getZ());
                };
            }

            return List.of(lookValue, "~");
        }

        return Collections.emptyList();
    }

    @Override
    public String getUsage() {
        return "create <name> <x1> <y1> <z1> <x2> <y2> <z2> [threshold] [delay]";
    }

    @Override
    public String getName() {
        return "create";
    }

    @Override
    public String getDescription() {
        return "Manually create a mine using coordinates";
    }
}
