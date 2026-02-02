package org.charky.aurora.features.mine.commands;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.charky.aurora.core.Aurora;
import org.charky.aurora.common.util.TextUtils;

import java.util.Collections;
import java.util.List;

/**
 * Mine sub command to forcefully trigger mine regeneration
 */
public class MineResetSub extends MineSub {
    public MineResetSub(Aurora plugin) {
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

        mineManager.resetMine(mineName);
        TextUtils.success(sender, "Mine '" + mineName + "' successfully reset");
    }

    @Override
    public List<String> tabComplete(Player player, String[] args) {
        if (args.length != 1) return Collections.emptyList();

        return mineManager.getMines()
                .stream()
                .filter(name -> name.toLowerCase().startsWith(args[0].toLowerCase()))
                .sorted()
                .toList();
    }

    @Override
    public String getUsage() {
        return "<mine>";
    }

    @Override
    public String getName() {
        return "reset";
    }

    @Override
    public String getDescription() {
        return "Immediately resets a mine";
    }
}
