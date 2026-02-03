package org.charky.aurora.features.mine.commands;

import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.charky.aurora.common.util.TextUtils;
import org.charky.aurora.core.Aurora;

import java.util.Collections;
import java.util.List;

public class MineWandCreateSub extends MineSub {
    public MineWandCreateSub(Aurora plugin) {
        super(plugin);
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            TextUtils.error(sender, "Only players can use the wand creation command");
            return;
        }

        if (args.length < 1) {
            TextUtils.usage(player, getFullUsage());
            return;
        }

        String mineName = args[0];

        if (mineManager.exists(mineName)) {
            TextUtils.error(player, "Mine '" + mineName + "' already exists");
            return;
        }

        if (!wandManager.hasBothPositions(player.getUniqueId())) {
            TextUtils.error(player, "You must select two positions with the wand first!");
            return;
        }

        Location p1 = wandManager.getPos1(player.getUniqueId());
        Location p2 = wandManager.getPos2(player.getUniqueId());

        if (!p1.getWorld().equals(p2.getWorld())) {
            TextUtils.error(player, "Positions must be in the same world!");
            return;
        }

        int minX = Math.min(p1.getBlockX(), p2.getBlockX());
        int minY = Math.min(p1.getBlockY(), p2.getBlockY());
        int minZ = Math.min(p1.getBlockZ(), p2.getBlockZ());
        int maxX = Math.max(p1.getBlockX(), p2.getBlockX());
        int maxY = Math.max(p1.getBlockY(), p2.getBlockY());
        int maxZ = Math.max(p1.getBlockZ(), p2.getBlockZ());

        if ((maxX - minX + 1) * (maxY - minY + 1) * (maxZ - minZ + 1) > MAX_MINE_VOLUME) {
            TextUtils.error(player, "Mine volume exceeds limit of " + MAX_MINE_VOLUME);
            return;
        }

        // Optional parameters, threshold and delay
        double threshold = (args.length >= 2) ? Double.parseDouble(args[1]) : 101;
        int delay = (args.length >= 3) ? Math.max(20, Integer.parseInt(args[2])) : 1200;

        mineManager.addMine(mineName, p1.getWorld().getName(), minX, minY, minZ, maxX, maxY, maxZ, threshold, delay);
        TextUtils.success(player, "Mine '" + mineName + "' created using wand selection!");
    }

    @Override
    public List<String> tabComplete(Player player, String[] args) {
        return Collections.emptyList();
    }

    @Override
    public String getUsage() {
        return "<name> [threshold] [delay]";
    }

    @Override
    public String getName() {
        return "wandcreate";
    }

    @Override
    public String getDescription() {
        return "Create a mine using your current wand selection";
    }
}