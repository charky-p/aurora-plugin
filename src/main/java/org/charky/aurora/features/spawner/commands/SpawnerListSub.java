package org.charky.aurora.features.spawner.commands;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.charky.aurora.common.util.Pager;
import org.charky.aurora.common.util.TextUtils;
import org.charky.aurora.core.Aurora;

import java.util.Collections;
import java.util.List;

/**
 * Spawner sub command to list all spawners, paged the same way as mine list
 */
public class SpawnerListSub extends SpawnerSub {
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

        List<String> pageNames = Pager.open(sender, args, names, "SPAWNERS", 29);
        if (pageNames == null) return;

        for (String name : pageNames) {
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
