package org.charky.aurora.features.spawner.commands;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.charky.aurora.common.util.TextUtils;
import org.charky.aurora.core.Aurora;

import java.util.Collections;
import java.util.List;

/**
 * Spawner sub command to list all spawners, paged the same way as mine list
 */
public class SpawnerListSub extends SpawnerSub {
    private static final int PAGE_SIZE = 10;

    public SpawnerListSub(Aurora plugin) {
        super(plugin);
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        List<String> names = TextUtils.getCompletions("", spawnerManager.getSpawners());
        if (names.isEmpty()) {
            TextUtils.warn(sender, "There are no spawners created yet");
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

        TextUtils.display(sender, "<aqua><st>                             </st> <b>SPAWNERS (<white>" + page + "/" + maxPages + "</white>)</b> <st>                             </st>");
        for (String name : names.subList(start, end)) {
            String state = spawnerManager.getSpawner(name).isActive() ? "" : " <red>(off)";
            String message = " <dark_gray>» <gray>" + name + state;
            String infoIcon = "<click:run_command:'/aurora spawner info " + name + "'><hover:show_text:'<aqua>Click for <aqua>Info'><aqua>ℹ</aqua></hover></click>";
            String tpIcon = "<click:run_command:'/aurora spawner tp " + name + "'><hover:show_text:'<green>Teleport to <green>" + name + "'><green>➹</green></hover></click>";
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
        return "Displays a list of all spawners in alphabetical order";
    }
}
