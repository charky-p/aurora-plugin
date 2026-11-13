package org.charky.aurora.features.mine.commands;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.charky.aurora.core.Aurora;
import org.charky.aurora.common.util.TextUtils;

import java.util.Collections;
import java.util.List;

/**
 * Mine sub command to remove a mine from the database
 */
public class MineDeleteSub extends MineSub {
    public MineDeleteSub(Aurora plugin) {
        super(plugin);
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length < 1) {
            TextUtils.usage(sender, getFullUsage());
            return;
        }

        String mineName = args[0];
        if (find(sender, mineName) == null) return;

        mineManager.removeMine(mineName);
        TextUtils.success(sender, "Mine '" + mineName + "' has been successfully removed");
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
        return "delete";
    }

    @Override
    public String getDescription() {
        return "Delete a specific mine";
    }
}
