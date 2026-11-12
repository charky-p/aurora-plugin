package org.charky.aurora.features.trader.commands;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.charky.aurora.common.util.TextUtils;
import org.charky.aurora.core.Aurora;
import org.charky.aurora.features.trader.TraderSpot;

import java.util.Collections;
import java.util.List;

/**
 * Trader sub command to list all trader spots in the order they were placed,
 * paged the same way as mine and spawner list
 */
public class TraderListSub extends TraderSub {
    private static final int PAGE_SIZE = 10;

    public TraderListSub(Aurora plugin) {
        super(plugin);
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        List<TraderSpot> spots = traderManager.getSpots();
        if (spots.isEmpty()) {
            TextUtils.warn(sender, "There are no traders placed yet");
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

        int maxPages = (int) Math.ceil((double) spots.size() / PAGE_SIZE);
        if (page > maxPages || page < 1) {
            TextUtils.error(sender, "Page " + page + " does not exist. (Max: " + maxPages + ")");
            return;
        }

        int start = (page - 1) * PAGE_SIZE;
        int end = Math.min(start + PAGE_SIZE, spots.size());

        TextUtils.display(sender, "<aqua><st>                              </st> <b>TRADERS (<white>" + page + "/" + maxPages + "</white>)</b> <st>                              </st>");
        for (TraderSpot spot : spots.subList(start, end)) {
            String message = " <dark_gray>» <gray>" + spot.describe();
            String tpIcon = "<click:run_command:'/aurora trader tp " + spot.id() + "'><hover:show_text:'<green>Teleport to <green>#" + spot.id() + "'><green>➹</green></hover></click>";
            TextUtils.display(sender, message + " " + tpIcon);
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
        return "Displays a list of all trader spots";
    }
}
