package org.charky.aurora.features.gauntlet;

import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.charky.aurora.common.util.TextUtils;
import org.charky.aurora.core.Aurora;
import org.charky.aurora.features.system.WandManager;
import org.charky.aurora.features.system.commands.AuroraSubCommand;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.BiFunction;

/**
 * Aurora admin command for setting up the gauntlet: its lobby, the arena saved from
 * the wand, the spawns inside the arena, the care package spot and the PvP zone
 */
public class GauntletSub implements AuroraSubCommand {
    private static final List<String> ACTIONS = List.of("lobby", "arena", "spawn", "mobspawn", "clearmobspawns",
            "package", "pvp", "startportal", "duelarena", "duelspawn", "board", "status", "reset");

    private final Aurora plugin;

    public GauntletSub(Aurora plugin) {
        this.plugin = plugin;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length < 1 || !ACTIONS.contains(args[0].toLowerCase())) {
            TextUtils.usage(sender, getFullUsage());
            return;
        }
        GauntletManager manager = plugin.getGauntletManager();
        GauntletArena arena = manager.arena();
        String action = args[0].toLowerCase();
        if (action.equals("status")) {
            status(sender, manager, arena);
            return;
        }
        if (action.equals("reset")) {
            reset(sender, manager, args);
            return;
        }
        if (!(sender instanceof Player player)) {
            TextUtils.error(sender, "Stand where it goes, from in game");
            return;
        }
        Location here = player.getLocation();
        switch (action) {
            case "lobby" -> lobby(player, arena, here);
            case "arena" -> fromWand(player, "arena", arena::saveArena, "Arena saved. Runs paste a copy of it every "
                    + GauntletArena.SLOT_SPACING + " blocks along z. Now set its spawn, mob spawns and package spot inside it");
            case "spawn" -> {
                if (!insideArena(sender, arena, here)) return;
                arena.set(GauntletArena.SPAWN, here);
                TextUtils.success(sender, "Players start their runs here");
            }
            case "mobspawn" -> {
                if (!insideArena(sender, arena, here)) return;
                arena.addMobSpawn(here);
                TextUtils.success(sender, "Mob spawn " + arena.mobSpawns().size() + " added");
            }
            case "clearmobspawns" -> {
                arena.clearMobSpawns();
                TextUtils.success(sender, "Every mob spawn removed");
            }
            case "package" -> {
                if (!insideArena(sender, arena, here)) return;
                arena.set(GauntletArena.PACKAGE, here);
                TextUtils.success(sender, "Care packages land here");
            }
            case "duelarena" -> fromWand(player, "duel arena", arena::saveDuelArena, "Duel arena saved. Duels paste a copy of it every "
                    + GauntletArena.SLOT_SPACING + " blocks along x. Now set its two spawns with /aurora gauntlet duelspawn 1 and 2, inside it");
            case "duelspawn" -> duelSpawn(player, arena, here, args);
            case "board" -> board(player, manager, args);
            case "startportal" -> fromWand(player, "start portal", arena::setStart, "Start portal set, walking into it starts a run");
            case "pvp" -> fromWand(player, "PvP zone", arena::setPvp, "PvP zone set, players can fight inside it");
            default -> {
            }
        }
    }

    private static void status(CommandSender sender, GauntletManager manager, GauntletArena arena) {
        String missing = arena.missing();
        TextUtils.send(sender, missing == null
                ? "<green>The Gauntlet is ready: " + arena.mobSpawns().size() + " mob spawns, "
                + manager.data().floors().size() + " floors"
                : "<red>Not ready: " + missing);
        String duels = arena.duelMissing();
        TextUtils.send(sender, duels == null ? "<green>Duels are ready" : "<yellow>Duels not ready: " + duels);
    }

    private static void reset(CommandSender sender, GauntletManager manager, String[] args) {
        if (args.length < 2) {
            TextUtils.usage(sender, "/aurora gauntlet reset <player>");
            return;
        }
        Player target = AuroraSubCommand.targetPlayer(sender, args, 1);
        if (target == null) return;
        manager.reset(target);
        TextUtils.success(sender, target.getName() + "'s Gauntlet progress is reset");
        if (!target.equals(sender)) TextUtils.send(target, "<red>Your Gauntlet progress was reset by staff");
    }

    // Its whole world becomes the Gauntlet's, inventories swapped on the way in
    private void lobby(Player player, GauntletArena arena, Location here) {
        Location main = plugin.getMainSpawn();
        if (main != null && here.getWorld().equals(main.getWorld())) {
            TextUtils.error(player, "The Gauntlet needs a world of its own, not the main world. /aurora world create <name>");
            return;
        }
        arena.set(GauntletArena.LOBBY, here);
        TextUtils.success(player, "The Gauntlet's lobby is here, " + here.getWorld().getName() + " is now its world");
    }

    private static void duelSpawn(Player player, GauntletArena arena, Location here, String[] args) {
        if (args.length < 2 || !(args[1].equals("1") || args[1].equals("2"))) {
            TextUtils.usage(player, "/aurora gauntlet duelspawn <1|2>");
            return;
        }
        if (arena.duelBox() == null) {
            TextUtils.error(player, "Save the duel arena first, /aurora gauntlet duelarena");
            return;
        }
        if (!arena.duelBox().contains(here)) {
            TextUtils.error(player, "Stand inside the duel arena you saved");
            return;
        }
        arena.setDuelSpawn(Integer.parseInt(args[1]), here);
        TextUtils.success(player, "Duelist " + args[1] + " starts here");
    }

    // A leaderboard standing in the lobby, or the nearest one taken down
    private static void board(Player player, GauntletManager manager, String[] args) {
        String kind = args.length < 2 ? "" : args[1].toLowerCase();
        if (kind.equals("remove")) {
            String removed = manager.boards().removeNearest(player);
            if (removed == null) TextUtils.error(player, "No Gauntlet board within 8 blocks");
            else TextUtils.success(player, "Took down the " + removed + " board");
            return;
        }
        if (!GauntletBoards.KINDS.contains(kind)) {
            TextUtils.usage(player, "/aurora gauntlet board <" + String.join("|", GauntletBoards.KINDS) + "|remove>");
            return;
        }
        if (!manager.isHere(player)) {
            TextUtils.error(player, "Boards go in the Gauntlet's world");
            return;
        }
        manager.boards().place(player, kind);
        TextUtils.success(player, "The " + kind + " board is up, it keeps itself up to date");
    }

    // The wand's selection handed to the arena, which says what is wrong with it or null
    private void fromWand(Player player, String what, BiFunction<Location, Location, String> save, String done) {
        WandManager wand = plugin.getWandManager();
        UUID uuid = player.getUniqueId();
        if (!wand.hasBothPositions(uuid)) {
            TextUtils.error(player, "Select the " + what + " with the wand first, /aurora wand");
            return;
        }
        String problem = save.apply(wand.getPos1(uuid), wand.getPos2(uuid));
        if (problem != null) TextUtils.error(player, problem);
        else TextUtils.success(player, done);
    }

    // Spawns are saved in the template arena, and moved into each run's copy
    private static boolean insideArena(CommandSender sender, GauntletArena arena, Location here) {
        if (arena.arenaBox() == null) {
            TextUtils.error(sender, "Save the arena first, /aurora gauntlet arena");
            return false;
        }
        if (!arena.arenaBox().contains(here)) {
            TextUtils.error(sender, "Stand inside the arena you saved");
            return false;
        }
        return true;
    }

    @Override
    public List<String> tabComplete(Player player, String[] args) {
        if (args.length == 1) return TextUtils.getCompletions(args[0], ACTIONS);
        if (args.length == 2 && args[0].equalsIgnoreCase("reset")) return TextUtils.playerCompletions(args[1]);
        if (args.length == 2 && args[0].equalsIgnoreCase("board")) {
            List<String> options = new ArrayList<>(GauntletBoards.KINDS);
            options.add("remove");
            return TextUtils.getCompletions(args[1], options);
        }
        return List.of();
    }

    @Override
    public String getUsage() {
        return "<lobby|arena|spawn|mobspawn|clearmobspawns|package|pvp|startportal|duelarena|duelspawn|board|status|reset> [player|1|2|kind]";
    }

    @Override
    public String getName() {
        return "gauntlet";
    }

    @Override
    public String getDescription() {
        return "Set up the Gauntlet: its lobby, arena, spawns, care package spot and PvP zone";
    }

    @Override
    public List<String> getInfo() {
        return List.of(
                "lobby - players arrive here; its world becomes the Gauntlet's world",
                "arena - save the wand selection as the arena (in the lobby's world); runs paste copies of it",
                "spawn - where players start a run, inside the arena",
                "mobspawn - add a spot mobs come in at, inside the arena (add several)",
                "clearmobspawns - remove every mob spawn",
                "package - where care packages land, inside the arena",
                "pvp - the wand selection is the PvP zone, the only place players can fight",
                "startportal - the wand selection, in the lobby, starts a run when walked into",
                "duelarena - save the wand selection as the duel arena (in the lobby's world); duels paste copies of it",
                "duelspawn <1|2> - where each duelist starts, inside the duel arena",
                "board <floors|pvp|duels|week> - a leaderboard where you stand, kept up to date; board remove takes the nearest down",
                "status - what is still missing",
                "reset <player> - wipe a player's Gauntlet progress: Shards, Cores, best floor, gear, kit, milestones");
    }
}
