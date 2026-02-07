package org.charky.aurora.features.mine.commands;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.charky.aurora.core.Aurora;
import org.charky.aurora.common.util.TextUtils;
import org.charky.aurora.features.mine.Mine;
import org.charky.aurora.features.mine.MineData;

import java.util.Collections;
import java.util.List;

/**
 * Mine sub command to get information about a mine
 */
public class MineInfoSub extends MineSub {
    public MineInfoSub(Aurora plugin) {
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
            TextUtils.error(sender, "Mine '" + mineName + "' does not exist");
            return;
        }

        MineData data = mineManager.getMine(mineName).getData();
        TextUtils.display(sender, "<aqua><st>                                </st> <b>MINE INFO</b> <st>                                </st>");
        TextUtils.display(sender, " <dark_gray>» <aqua>Name: <white>" + data.getName());
        TextUtils.display(sender, " <dark_gray>» <aqua>World: <white>" + data.getWorld());
        TextUtils.display(sender, " <dark_gray>» <aqua>Coordinate 1: <white>" + data.getX1() + "<gray>, <white>" + data.getY1() + "<gray>, <white>" + data.getZ1());
        TextUtils.display(sender, " <dark_gray>» <aqua>Coordinate 2: <white>" + data.getX2() + "<gray>, <white>" + data.getY2() + "<gray>, <white>" + data.getZ2());
        TextUtils.display(sender, " <dark_gray>» <aqua>Reset Threshold: <white>" + data.getThreshold() + "%");
        TextUtils.display(sender, " <dark_gray>» <aqua>Delay: <white>" + data.getDelay() + "s");
        TextUtils.display(sender, " <dark_gray>» <aqua>Teleport Coordinate: <white>" + data.getTpX() + "<gray>, <white>" + data.getTpY() + "<gray>, <white>" + data.getTpZ());
        TextUtils.line(sender, "<aqua>");
    }

    @Override
    public List<String> tabComplete(Player player, String[] args) {
        if (args.length == 1) return TextUtils.getCompletions(args[0], mineManager.getMines());

        return Collections.emptyList();
    }

    @Override
    public String getUsage() {
        return "<mine>";
    }

    @Override
    public String getName() {
        return "info";
    }

    @Override
    public String getDescription() {
        return "Display information about a mine";
    }
}
