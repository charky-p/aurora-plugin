package org.charky.aurora.features.pvp;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.charky.aurora.common.util.Box;
import org.charky.aurora.common.util.TextUtils;
import org.charky.aurora.core.Aurora;
import org.charky.aurora.features.system.WandManager;
import org.charky.aurora.features.system.commands.AuroraSubCommand;

import java.util.List;

/**
 * Safe zones, boxes where players can't hurt each other. PvP is on everywhere else
 */
public class SafeZoneSub implements AuroraSubCommand {
    private static final List<String> ACTIONS = List.of("create", "delete", "list");

    private final Aurora plugin;

    public SafeZoneSub(Aurora plugin) {
        this.plugin = plugin;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        SafeZoneManager zones = plugin.getSafeZoneManager();
        if (args.length == 0 || args[0].equalsIgnoreCase("list")) {
            TextUtils.display(sender, "<aqua><b>SAFE ZONES</b> <gray>no PvP inside, PvP everywhere else");
            zones.getZones().forEach((name, box) -> TextUtils.display(sender, " <dark_gray>» <white>" + name
                    + " <gray>" + box.world() + " " + box.x1() + " " + box.y1() + " " + box.z1()
                    + " to " + box.x2() + " " + box.y2() + " " + box.z2()));
            return;
        }
        if (args.length < 2 || !ACTIONS.contains(args[0].toLowerCase())) {
            TextUtils.usage(sender, getFullUsage());
            return;
        }

        String name = args[1];
        if (args[0].equalsIgnoreCase("delete")) {
            if (zones.delete(name)) TextUtils.success(sender, "Safe zone '" + name + "' deleted, PvP is on there again");
            else TextUtils.error(sender, "No safe zone called '" + name + "'");
            return;
        }

        if (!(sender instanceof Player player)) {
            TextUtils.error(sender, "Select the corners with the wand in game");
            return;
        }
        WandManager wand = plugin.getWandManager();
        if (!wand.hasBothPositions(player.getUniqueId())) {
            TextUtils.error(sender, "Select two corners with the wand first (/aurora wand)");
            return;
        }
        String problem = zones.create(name, wand.getPos1(player.getUniqueId()), wand.getPos2(player.getUniqueId()));
        if (problem != null) {
            TextUtils.error(sender, problem);
            return;
        }
        Box box = zones.getZones().get(name.toLowerCase());
        TextUtils.success(sender, "Safe zone '" + name.toLowerCase() + "' created, " + box.volume() + " blocks with no PvP");
    }

    @Override
    public List<String> tabComplete(Player player, String[] args) {
        if (args.length == 1) return TextUtils.getCompletions(args[0], ACTIONS);
        if (args.length == 2 && args[0].equalsIgnoreCase("delete")) {
            return TextUtils.getCompletions(args[1], plugin.getSafeZoneManager().getZones().keySet());
        }
        return List.of();
    }

    @Override
    public String getUsage() {
        return "<create|delete|list> [name]";
    }

    @Override
    public String getName() {
        return "safezone";
    }

    @Override
    public String getDescription() {
        return "Boxes with no PvP, made from the wand selection. PvP is on everywhere else";
    }
}
