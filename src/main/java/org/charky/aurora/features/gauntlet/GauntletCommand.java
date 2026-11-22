package org.charky.aurora.features.gauntlet;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.charky.aurora.common.util.TextUtils;
import org.jetbrains.annotations.NotNull;

import java.util.List;

// /gauntlet: to the gauntlet's lobby from anywhere, its menu once there.
// /gauntlet boon reopens a boon waiting to be picked, /gauntlet leave goes home
public class GauntletCommand implements TabExecutor {
    private final GauntletManager manager;
    private final GauntletRuns runs;

    public GauntletCommand(GauntletManager manager, GauntletRuns runs) {
        this.manager = manager;
        this.runs = runs;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String @NotNull [] args) {
        if (!(sender instanceof Player player)) {
            TextUtils.error(sender, "Only players can run this command");
            return true;
        }
        String action = args.length > 0 ? args[0].toLowerCase() : "";
        String problem = switch (action) {
            case "boon" -> runs.openBoons(player);
            case "leave" -> {
                if (!manager.isHere(player)) yield "You are not in the Gauntlet";
                manager.leave(player);
                yield null;
            }
            default -> open(manager, runs, player);
        };
        if (problem != null) TextUtils.error(player, problem);
        return true;
    }

    // To the lobby from anywhere, the menu once there. Also the Gauntlet button in /a.
    // Null if it worked, otherwise why not
    public static String open(GauntletManager manager, GauntletRuns runs, Player player) {
        if (!manager.isHere(player)) return manager.sendToLobby(player);
        new GauntletMenu(manager, runs).open(player);
        return null;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                      @NotNull String label, @NotNull String @NotNull [] args) {
        return args.length == 1 ? TextUtils.getCompletions(args[0], List.of("boon", "leave")) : List.of();
    }
}
