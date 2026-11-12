package org.charky.aurora.features.mine.commands;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.charky.aurora.core.Aurora;
import org.charky.aurora.common.util.Pager;
import org.charky.aurora.common.util.TextUtils;

import java.util.Collections;
import java.util.List;

/**
 * Mine sub command to list all mines in database
 */
public class MineListSub extends MineSub {
    public MineListSub(Aurora plugin) {
        super(plugin);
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        List<String> names = TextUtils.getCompletions("", mineManager.getMines());

        if (names.isEmpty()) {
            TextUtils.warn(sender, "There are no mines created yet");
            return;
        }

        List<String> pageNames = Pager.open(sender, args, names, "MINES", 31);
        if (pageNames == null) return;

        for (String name : pageNames) {
            String message = " <dark_gray>» <gray>" + name;
            String infoIcon = "<click:run_command:'/aurora mine info " + name + "'><hover:show_text:'<aqua>Click for <aqua>Info'><aqua>ℹ</aqua></hover></click>";
            String tpIcon = "<click:run_command:'/aurora mine tp " + name + "'><hover:show_text:'<green>Teleport to <green>" + name + "'><green>➹</green></hover></click>";
            TextUtils.display(sender, message + " " + infoIcon + " " + tpIcon);
        }
        TextUtils.line(sender, "<aqua>");
    }

    @Override
    public List<String> tabComplete(Player player, String[] args) {
        return Collections.emptyList();
    }

    @Override
    public String getUsage() {
        return "[page]";
    }

    @Override
    public String getName() {
        return "list";
    }

    @Override
    public String getDescription() {
        return "Displays a list of all mines in alphabetical order";
    }
}
