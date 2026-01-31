package org.charky.aurora.commands.mine;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.charky.aurora.Aurora;
import org.charky.aurora.util.TextUtils;

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
        if (!mineManager.exists(mineName)) {
            TextUtils.error(sender, "Mine '" + mineName + "' does not exist");
            return;
        }

        mineManager.removeMine(mineName);
        TextUtils.success(sender, "Mine '" + mineName + "' has been successfully removed");
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
        return "delete";
    }

    @Override
    public String getDescription() {
        return "Delete a specific mine";
    }
}
