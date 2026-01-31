package org.charky.aurora.commands.mine;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.charky.aurora.Aurora;
import org.charky.aurora.util.TextUtils;

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

        // TODO: printing information should be here, move getMineInfo logic here, by getting Mine object
        TextUtils.display(sender, "<aqua><st>                </st>MINE INFO<st>                </st>");
        for (String line : mineManager.getMineInfo(mineName)) {
            TextUtils.display(sender, " <dark_gray>» <aqua>" + line);
        }
        TextUtils.line(sender, "<aqua>");
    }

    @Override
    public List<String> tabComplete(Player player, String[] args) {
        if (args.length == 1) {
            return mineManager.getMines()
                    .stream()
                    .filter(name -> name.toLowerCase().startsWith(args[0].toLowerCase()))
                    .toList();
        }

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
