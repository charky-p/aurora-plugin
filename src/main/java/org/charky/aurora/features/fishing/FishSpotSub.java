package org.charky.aurora.features.fishing;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.charky.aurora.common.util.TextUtils;
import org.charky.aurora.core.Aurora;
import org.charky.aurora.features.system.commands.AuroraSubCommand;

import java.util.ArrayList;
import java.util.List;

/**
 * Fishing areas: the water of a fishing spot, a box the hook has to land in. create
 * takes the wand selection, a spot can have as many areas as it needs
 */
public class FishSpotSub implements AuroraSubCommand {
    private static final List<String> ACTIONS = List.of("create", "delete", "list");

    private final Aurora plugin;

    public FishSpotSub(Aurora plugin) {
        this.plugin = plugin;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        FishingManager fishing = plugin.getFishingManager();
        if (args.length == 0 || args[0].equalsIgnoreCase("list")) {
            TextUtils.display(sender, "<aqua><b>FISHING AREAS</b> <gray>spots: " + String.join(", ", fishing.spots().keySet()));
            fishing.areas().values().forEach(area -> TextUtils.display(sender, " <dark_gray>» <white>" + area.name()
                    + " <gray>" + area.spot() + " in " + area.world() + " " + area.x1() + " " + area.y1() + " " + area.z1()
                    + " to " + area.x2() + " " + area.y2() + " " + area.z2()));
            return;
        }
        String action = args[0].toLowerCase();
        if (!ACTIONS.contains(action) || args.length < 2) {
            TextUtils.usage(sender, getFullUsage());
            return;
        }
        String name = args[1].toLowerCase();
        if (action.equals("delete")) {
            if (fishing.deleteArea(name)) TextUtils.success(sender, "Fishing area '" + name + "' deleted");
            else TextUtils.error(sender, "No fishing area called " + name);
            return;
        }
        if (!(sender instanceof Player player)) {
            TextUtils.error(sender, "Only players can select an area");
            return;
        }
        if (args.length < 3) {
            TextUtils.usage(sender, getFullUsage());
            return;
        }
        String error = fishing.createArea(name, args[2].toLowerCase(),
                plugin.getWandManager().getPos1(player.getUniqueId()), plugin.getWandManager().getPos2(player.getUniqueId()));
        if (error != null) TextUtils.error(player, error);
        else TextUtils.success(player, "Fishing area '" + name + "' made from the wand selection, for " + args[2].toLowerCase());
    }

    @Override
    public List<String> tabComplete(Player player, String[] args) {
        if (args.length == 1) return TextUtils.getCompletions(args[0], ACTIONS);
        FishingManager fishing = plugin.getFishingManager();
        if (args.length == 2 && args[0].equalsIgnoreCase("delete")) {
            return TextUtils.getCompletions(args[1], new ArrayList<>(fishing.areas().keySet()));
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("create")) {
            return TextUtils.getCompletions(args[2], new ArrayList<>(fishing.spots().keySet()));
        }
        return List.of();
    }

    @Override
    public String getUsage() {
        return "create <name> <spot> | delete <name> | list";
    }

    @Override
    public String getName() {
        return "fishspot";
    }

    @Override
    public String getDescription() {
        return "Mark a fishing spot's water from the wand selection";
    }
}
