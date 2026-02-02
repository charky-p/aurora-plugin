package org.charky.aurora.commands.mine;

import org.bukkit.Axis;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.charky.aurora.Aurora;
import org.charky.aurora.util.TextUtils;

import java.util.Collections;
import java.util.List;

/**
 * TODO: Currently overlapping mines, decide whether this is a feature or error
 * TODO: Mine is WAY too large error
 * TODO: Add create2 here
 * Mine subcommand to create mines manually by coordinates.
 */
public class MineCreateSub extends MineSub {
    public MineCreateSub(Aurora plugin) {
        super(plugin);
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length < 7) {
            TextUtils.usage(sender, getFullUsage());
            return;
        }

        try {
            String mineName = args[0];

            if (mineManager.exists(mineName)) {
                TextUtils.error(sender, "Mine '" + mineName + "' already exists");
                return;
            }

            String worldName = (sender instanceof Player p) ? p.getWorld().getName() : "world";

            double x1 = TextUtils.parseCoordinate(sender, args[1], Axis.X);
            double y1 = Math.max(-64, Math.min(TextUtils.parseCoordinate(sender, args[2], Axis.Y), 319));
            double z1 = TextUtils.parseCoordinate(sender, args[3], Axis.Z);
            double x2 = TextUtils.parseCoordinate(sender, args[4], Axis.X);
            double y2 = Math.max(-64, Math.min(TextUtils.parseCoordinate(sender, args[5], Axis.Y), 319));
            double z2 = TextUtils.parseCoordinate(sender, args[6], Axis.Z);

            // Optional parameters, by default threshold is off (101%), delay is 1 minute
            double threshold = (args.length >= 8) ? Double.parseDouble(args[7]) : 101;
            int delay = (args.length >= 9) ? Math.max(20, Integer.parseInt(args[8])) : 1200;

            int minX = (int) Math.floor(Math.min(x1, x2));
            int minY = (int) Math.floor(Math.min(y1, y2));
            int minZ = (int) Math.floor(Math.min(z1, z2));
            int maxX = (int) Math.floor(Math.max(x1, x2));
            int maxY = (int) Math.floor(Math.max(y1, y2));
            int maxZ = (int) Math.floor(Math.max(z1, z2));

            mineManager.addMine(mineName, worldName, minX, minY, minZ, maxX, maxY, maxZ, threshold, delay);
            TextUtils.success(sender, "Mine '" + mineName + "' created successfully");
        } catch (NumberFormatException e) {
            TextUtils.error(sender, "Coordinates, threshold, and delay must be valid numbers");
        }
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
        return "<name> <x1> <y1> <z1> <x2> <y2> <z2> [threshold] [delay]";
    }

    @Override
    public String getName() {
        return "create";
    }

    @Override
    public String getDescription() {
        return "Manually create a mine using coordinates";
    }

    // TODO: add this officially here
//    private void createMine2(@NotNull CommandSender sender, @NotNull String @NotNull [] args) {
//        if (!(sender instanceof Player player)) return;
//
//        if (args.length < 2) {
//            TextUtils.format(player, "Usage: /aurora mine create <name>", MessageType.ERROR);
//            return;
//        }
//
//        String mineName = args[1];
//        if (!wandManager.hasBothPositions(player.getUniqueId())) {
//            TextUtils.format(player, "You must select two positions with the Aurora Wand first!", MessageType.ERROR);
//            return;
//        }
//
//        Location p1 = wandManager.getPos1(player.getUniqueId());
//        Location p2 = wandManager.getPos2(player.getUniqueId());
//
//        // We pass the locations directly to the manager
//        manager.addMine(
//                mineName,
//                p1.getWorld().getName(),
//                Math.min(p1.getBlockX(), p2.getBlockX()),
//                Math.min(p1.getBlockY(), p2.getBlockY()),
//                Math.min(p1.getBlockZ(), p2.getBlockZ()),
//                Math.max(p1.getBlockX(), p2.getBlockX()),
//                Math.max(p1.getBlockY(), p2.getBlockY()),
//                Math.max(p1.getBlockZ(), p2.getBlockZ()),
//                101, // default threshold
//                1200 // default delay
//        );
//
//        TextUtils.format(player, "Mine '" + mineName + "' created using your wand selection!", MessageType.SUCCESS);
//    }
}
