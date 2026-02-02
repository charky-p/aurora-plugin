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
    private static final int PAGE_SIZE = 10;

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

        int page = 1;
        if (args.length > 0) {
            try {
                page = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                TextUtils.error(sender, "Invalid page number: " + args[0]);
                return;
            }
        }

        int maxPages = (int) Math.ceil((double) names.size() / PAGE_SIZE);

        if (page > maxPages || page < 1) {
            TextUtils.error(sender, "Page " + page + " does not exist. (Max: " + maxPages + ")");
            return;
        }

        int start = (page - 1) * PAGE_SIZE;
        int end = Math.min(start + PAGE_SIZE, names.size());
        List<String> pageNames = names.subList(start, end);
//        TextUtils.display(sender, "<aqua><b>Mines:");
        TextUtils.display(sender, "<aqua><st>                               </st> <b>MINES (<white>" + page + "/" + maxPages + "</white>)</b> <st>                               </st>");
        for (String name : pageNames) {
            String message = " <dark_gray>» <gray>" + name;
            String infoIcon = "<click:run_command:'/aurora mine info " + name + "'><hover:show_text:'<aqua>Click for <aqua>Info'><aqua>ℹ</aqua></hover></click>";
            String tpIcon = "<click:run_command:'/aurora mine tp " + name + "'><hover:show_text:'<green>Teleport to <green>" + name + "'><green>➹</green></hover></click>";

//            String infoIcon = "<click:run_command:'/aurora mine info " + name + "'><hover:show_text:'<aqua>Click for <aqua>Info'><dark_gray>[<aqua>ℹ</aqua>]</hover></click>";
//            String tpIcon = "<click:run_command:'/aurora mine tp " + name + "'><hover:show_text:'<green>Teleport to <green>" + name + "'><dark_gray>[<green>➹</green>]</hover></click>";
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
