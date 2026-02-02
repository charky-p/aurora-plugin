package org.charky.aurora.features.mine.commands;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.charky.aurora.core.Aurora;
import org.charky.aurora.common.util.TextUtils;

import java.util.Collections;
import java.util.List;

/**
 * Mine sub command to list all mines in database
 * TODO: Add list pages, with /aurora mine list [page], with maybe max 20 mines per page
 */
public class MineListSub extends MineSub {
    public MineListSub(Aurora plugin) {
        super(plugin);
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        List<String> names = mineManager.getMines()
                .stream()
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();
        if (names.isEmpty()) {
            TextUtils.warn(sender, "There are no mines created yet");
            return;
        }

        TextUtils.display(sender, "<aqua><b>Mines:");
        for (String name : names) {
            String message = " <dark_gray>» <gray>" + name +
                    " <dark_gray>[<click:run_command:'/aurora mine info " + name + "'><hover:show_text:'<gray>Click for <aqua>Info'> <aqua>ℹ</aqua></hover></click>]" +
                    " <dark_gray>[<click:run_command:'/aurora mine tp " + name + "'><hover:show_text:'<gray>Teleport to <green>" + name + "'> <green>⚙</green></hover></click>]";
            TextUtils.display(sender, message);
        }
    }

    @Override
    public List<String> tabComplete(Player player, String[] args) {
        return Collections.emptyList();
    }

    @Override
    public String getUsage() {
        return "";
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
