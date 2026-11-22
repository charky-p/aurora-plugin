package org.charky.aurora.features.gauntlet;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.charky.aurora.common.util.TextUtils;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

// /gauntlet: to the gauntlet's lobby from anywhere, its menu once there. /gauntlet
// start starts a run (for the whole party, by its leader), /gauntlet forge opens the
// forge, /gauntlet party handles the party, /gauntlet boon reopens a boon waiting to
// be picked, /gauntlet leave goes home
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
            case "start" -> {
                if (!manager.isHere(player)) yield manager.sendToLobby(player);
                if (args.length < 2) yield runs.startFlow(player);
                try {
                    yield runs.begin(player, Integer.parseInt(args[1]));
                } catch (NumberFormatException e) {
                    yield "Not a floor: " + args[1];
                }
            }
            case "duel" -> duel(player, args);
            case "party" -> party(player, args);
            case "spectate" -> {
                if (args.length < 2) {
                    new SpectateMenu(manager).open(player);
                    yield null;
                }
                if (args[1].equalsIgnoreCase("stop")) {
                    if (!manager.isWatching(player)) yield "You are not spectating";
                    manager.spectate().stop(player, "Stopped spectating");
                    yield null;
                }
                yield manager.spectate().watch(player, Bukkit.getPlayerExact(args[1]));
            }
            case "shop" -> {
                if (!manager.isHere(player)) yield manager.sendToLobby(player);
                new ShardShopMenu(manager).open(player);
                yield null;
            }
            case "achievements" -> {
                new AchievementsMenu(manager).open(player);
                yield null;
            }
            case "talents" -> {
                if (!manager.isHere(player)) yield manager.sendToLobby(player);
                new TalentsMenu(manager).open(player);
                yield null;
            }
            case "forge" -> {
                if (!manager.isHere(player)) yield manager.sendToLobby(player);
                new ForgeMenu(manager, runs).open(player);
                yield null;
            }
            case "leave" -> {
                if (!manager.isHere(player)) yield "You are not in the Gauntlet";
                yield manager.pvp().leave(player, true);
            }
            default -> open(manager, runs, player);
        };
        if (problem != null) TextUtils.error(player, problem);
        return true;
    }

    // /gauntlet duel <player>, accept, decline or forfeit
    private String duel(Player player, String[] args) {
        GauntletDuels duels = manager.duels();
        if (args.length < 2) return "/gauntlet duel <player>, or accept, decline or forfeit";
        return switch (args[1].toLowerCase()) {
            case "accept" -> duels.accept(player);
            case "decline" -> duels.decline(player);
            case "forfeit" -> duels.forfeit(player);
            default -> duels.challenge(player, Bukkit.getPlayerExact(args[1]));
        };
    }

    // /gauntlet party: its menu, or invite <player>, accept, decline, leave, kick <player>, open (to
    // anyone, or closed again), join <leader> (an open party), list
    private String party(Player player, String[] args) {
        GauntletParty party = manager.party();
        if (args.length < 2) {
            if (!manager.isHere(player)) return manager.sendToLobby(player);
            new PartyMenu(manager).open(player);
            return null;
        }
        return switch (args[1].toLowerCase()) {
            case "invite" -> args.length < 3 ? "/gauntlet party invite <player>" : party.invite(player, Bukkit.getPlayerExact(args[2]));
            case "accept" -> party.accept(player);
            case "decline" -> party.decline(player);
            case "leave" -> party.leave(player);
            case "kick" -> args.length < 3 ? "/gauntlet party kick <player>" : party.kick(player, Bukkit.getPlayerExact(args[2]));
            case "open" -> party.toggleOpen(player);
            case "join" -> args.length < 3 ? "/gauntlet party join <leader>" : party.join(player, Bukkit.getPlayerExact(args[2]));
            case "list" -> {
                party.list(player);
                yield null;
            }
            default -> "/gauntlet party invite <player>, accept, decline, leave, kick <player>, open, join <leader> or list";
        };
    }

    // To the lobby from anywhere, the menu once there. Also the Gauntlet button in /a.
    // Null if it worked, otherwise why not
    public static String open(GauntletManager manager, GauntletRuns runs, Player player) {
        if (!manager.isHere(player)) return manager.sendToLobby(player);
        manager.openMenu(player);
        return null;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                      @NotNull String label, @NotNull String @NotNull [] args) {
        if (args.length == 1) return TextUtils.getCompletions(args[0], List.of("start", "forge", "talents", "party", "duel", "spectate",
                "achievements", "shop", "boon", "leave"));
        if (args.length == 2 && args[0].equalsIgnoreCase("spectate")) {
            List<String> options = new ArrayList<>(List.of("stop"));
            for (Player online : Bukkit.getOnlinePlayers()) {
                if (!online.equals(sender) && (manager.inRun(online) || manager.inDuel(online))) options.add(online.getName());
            }
            return TextUtils.getCompletions(args[1], options);
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("party")) {
            return TextUtils.getCompletions(args[1], List.of("invite", "accept", "decline", "leave", "kick", "open", "join", "list"));
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("party") && args[1].equalsIgnoreCase("join")) {
            return TextUtils.getCompletions(args[2], manager.party().openLeaders().stream().map(Player::getName).toList());
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("party")
                && (args[1].equalsIgnoreCase("invite") || args[1].equalsIgnoreCase("kick"))) {
            List<String> names = new ArrayList<>();
            for (Player online : Bukkit.getOnlinePlayers()) if (!online.equals(sender) && manager.isHere(online)) names.add(online.getName());
            return TextUtils.getCompletions(args[2], names);
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("duel")) {
            List<String> options = new ArrayList<>(List.of("accept", "decline", "forfeit"));
            for (Player online : Bukkit.getOnlinePlayers()) if (!online.equals(sender)) options.add(online.getName());
            return TextUtils.getCompletions(args[1], options);
        }
        return List.of();
    }
}
