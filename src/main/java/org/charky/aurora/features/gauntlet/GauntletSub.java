package org.charky.aurora.features.gauntlet;

import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.charky.aurora.common.util.TextUtils;
import org.charky.aurora.core.Aurora;
import org.charky.aurora.features.system.WandManager;
import org.charky.aurora.features.system.commands.AuroraSubCommand;

import java.util.List;
import java.util.UUID;

/**
 * Aurora admin command for setting up the gauntlet: its lobby, the arena saved from
 * the wand, the spawns inside the arena, the care package spot and the PvP zone
 */
public class GauntletSub implements AuroraSubCommand {
    private static final List<String> ACTIONS = List.of("lobby", "arena", "spawn", "mobspawn", "clearmobspawns",
            "package", "pvp", "status");

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
            String missing = arena.missing();
            TextUtils.send(sender, missing == null
                    ? "<green>The Gauntlet is ready: " + arena.mobSpawns().size() + " mob spawns, "
                    + manager.data().floors().size() + " floors"
                    : "<red>Not ready: " + missing);
            return;
        }
        if (!(sender instanceof Player player)) {
            TextUtils.error(sender, "Stand where it goes, from in game");
            return;
        }
        Location here = player.getLocation();
        switch (action) {
            case "lobby" -> {
                // Its whole world becomes the Gauntlet's, inventories swapped on the way in
                Location main = plugin.getMainSpawn();
                if (main != null && here.getWorld().equals(main.getWorld())) {
                    TextUtils.error(sender, "The Gauntlet needs a world of its own, not the main world. /aurora world create <name>");
                    return;
                }
                arena.set(GauntletArena.LOBBY, here);
                TextUtils.success(sender, "The Gauntlet's lobby is here, " + here.getWorld().getName() + " is now its world");
            }
            case "arena" -> {
                WandManager wand = plugin.getWandManager();
                UUID uuid = player.getUniqueId();
                if (!wand.hasBothPositions(uuid)) {
                    TextUtils.error(sender, "Select the arena with the wand first, /aurora wand");
                    return;
                }
                String problem = arena.saveArena(wand.getPos1(uuid), wand.getPos2(uuid));
                if (problem != null) TextUtils.error(sender, problem);
                else TextUtils.success(sender, "Arena saved. Runs paste a copy of it every " + GauntletArena.SLOT_SPACING
                        + " blocks along z. Now set its spawn, mob spawns and package spot inside it");
            }
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
            case "pvp" -> {
                WandManager wand = plugin.getWandManager();
                UUID uuid = player.getUniqueId();
                if (!wand.hasBothPositions(uuid)) {
                    TextUtils.error(sender, "Select the PvP zone with the wand first, /aurora wand");
                    return;
                }
                String problem = arena.setPvp(wand.getPos1(uuid), wand.getPos2(uuid));
                if (problem != null) TextUtils.error(sender, problem);
                else TextUtils.success(sender, "PvP zone set, players can fight inside it");
            }
            default -> {
            }
        }
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
        return List.of();
    }

    @Override
    public String getUsage() {
        return "<lobby|arena|spawn|mobspawn|clearmobspawns|package|pvp|status>";
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
                "status - what is still missing");
    }
}
