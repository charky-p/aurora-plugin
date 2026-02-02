package org.charky.aurora.commands.spawner;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.charky.aurora.Aurora;
import org.charky.aurora.util.TextUtils;

import java.util.Collections;
import java.util.List;

/**
 * TODO: Add spawner pages if there r too many spawners, so /spawner list [page]
 * Spawner sub command to display a list of all spawners
 */
public class SpawnerListSub extends SpawnerSub {
    public SpawnerListSub(Aurora plugin) {
        super(plugin);
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        List<String> spawners = spawnerManager.getSpawners()
                        .stream()
                        .sorted(String.CASE_INSENSITIVE_ORDER)
                        .toList();

        if (spawners.isEmpty()) {
            TextUtils.error(sender, "There are no spawners created");
            return;
        }

        TextUtils.display(sender, "<aqua><b><st>                  </st>SPAWNERS<st>                  </st>");
        for (String spawnerName : spawners) {
            String message = " <dark_gray>» <gray>" + spawnerName +
                    " <dark_gray>[<click:run_command:'/aurora spawner info " + spawnerName + "'><hover:show_text:'<gray>Click for <aqua>Info'> <aqua>ℹ</aqua></hover></click>]" +
                    " <dark_gray>[<click:run_command:'/aurora spawner tp " + spawnerName + "'><hover:show_text:'<gray>Teleport to <green>" + spawnerName + "'> <green>⚙</green></hover></click>]";
            TextUtils.display(sender, message);
        }
        TextUtils.line(sender, "<aqua>");
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
        return "Display a list of all spawners";
    }
}
