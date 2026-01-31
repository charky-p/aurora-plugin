package org.charky.aurora.commands.mine;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.charky.aurora.Aurora;
import org.charky.aurora.util.TextUtils;

import java.util.Collections;
import java.util.List;

/**
 * Mine sub command to rename a mine
 */
public class MineRenameSub extends MineSub {
    public MineRenameSub(Aurora plugin) {
        super(plugin);
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length < 2) {
            TextUtils.usage(sender, getFullUsage());
            return;
        }

        String oldName = args[0];
        if (!mineManager.exists(oldName)) {
            TextUtils.error(sender, "Mine '" + oldName + "' does not exist");
            return;
        }

        String newName = args[1];
        if (mineManager.exists(newName)) {
            TextUtils.error(sender, "Mine '" + newName + "' already exists");
            return;
        }

        mineManager.renameMine(oldName, newName);
        TextUtils.success(sender, "Mine '" + oldName + "' renamed to '" + newName + "' successfully");
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
        return "<mine> <newName>";
    }

    @Override
    public String getName() {
        return "rename";
    }

    @Override
    public String getDescription() {
        return "Renames a mine to another name";
    }
}
