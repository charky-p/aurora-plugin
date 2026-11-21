package org.charky.aurora.features.portal.commands;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.charky.aurora.common.util.TextUtils;
import org.charky.aurora.core.Aurora;
import org.charky.aurora.features.portal.PadManager;
import org.charky.aurora.features.portal.PortalManager;
import org.charky.aurora.features.portal.TeleportPad;
import org.charky.aurora.features.system.WandManager;
import org.charky.aurora.features.system.commands.AuroraSubCommand;

import java.util.List;

/**
 * Teleport pads: the block a player stands on, unlocked for good by stepping on it.
 * point makes a portal from the wand selection, at spawn say, that leads to the pad and
 * only takes players who have unlocked it
 */
public class PadSub implements AuroraSubCommand {
    private static final List<String> ACTIONS = List.of("create", "point", "delete", "list", "tp");

    private final Aurora plugin;

    public PadSub(Aurora plugin) {
        this.plugin = plugin;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        PadManager pads = plugin.getPadManager();
        if (args.length == 0 || args[0].equalsIgnoreCase("list")) {
            TextUtils.display(sender, "<aqua><b>TELEPORT PADS</b> <gray>step on one to unlock it");
            pads.getPads().values().forEach(pad -> TextUtils.display(sender, " <dark_gray>» <white>" + pad.name()
                    + " <gray>" + pad.world() + " " + pad.x() + " " + pad.y() + " " + pad.z() + " <dark_gray>access " + pad.access()));
            return;
        }
        if (args.length < 2 || !ACTIONS.contains(args[0].toLowerCase())) {
            TextUtils.usage(sender, getFullUsage());
            return;
        }
        String action = args[0].toLowerCase();
        String name = args[1].toLowerCase();

        if (action.equals("delete")) {
            if (pads.delete(name)) TextUtils.success(sender, "Teleport pad '" + name + "' deleted, a portal leading to it stays");
            else TextUtils.error(sender, "No teleport pad called " + name);
            return;
        }
        if (!(sender instanceof Player player)) {
            TextUtils.error(sender, "Only players can do that");
            return;
        }
        switch (action) {
            case "create" -> create(player, pads, name);
            case "point" -> point(player, pads, name);
            default -> tp(player, pads, name);
        }
    }

    private void create(Player player, PadManager pads, String name) {
        if (!name.matches("[a-z0-9_]+")) {
            TextUtils.error(player, "Pad names are lowercase letters, numbers and _ only");
            return;
        }
        Block block = player.getLocation().getBlock().getRelative(BlockFace.DOWN);
        String error = pads.create(name, block);
        if (error != null) {
            TextUtils.error(player, error);
            return;
        }
        TextUtils.success(player, "Teleport pad '" + name + "' is the " + block.getType().name().toLowerCase()
                + " under you. Select a spot at spawn with the wand, then /aurora pad point " + name);
        if (block.getType() != Material.DIAMOND_BLOCK) {
            TextUtils.send(player, "<yellow>It is not a diamond block, players may not see it is a pad");
        }
    }

    // A portal from the wand selection to the pad, locked to players who unlocked it
    private void point(Player player, PadManager pads, String name) {
        TeleportPad pad = pads.get(name);
        Location to = pad == null ? null : pad.top();
        if (to == null) {
            TextUtils.error(player, pad == null ? "No teleport pad called " + name : "The pad's world is not loaded");
            return;
        }
        WandManager wand = plugin.getWandManager();
        if (!wand.hasBothPositions(player.getUniqueId())) {
            TextUtils.error(player, "Select two corners with the wand first (/aurora wand)");
            return;
        }
        PortalManager portals = plugin.getPortalManager();
        String portal = "pad_" + name;
        if (portals.exists(portal)) {
            TextUtils.error(player, "Portal '" + portal + "' already exists, /aurora portal delete " + portal + " first");
            return;
        }
        String error = portals.create(portal, wand.getPos1(player.getUniqueId()), wand.getPos2(player.getUniqueId()));
        if (error != null) {
            TextUtils.error(player, error);
            return;
        }
        portals.setDestination(portal, to);
        portals.setRequiredTag(portal, pad.access());
        TextUtils.success(player, "Portal '" + portal + "' leads to the pad '" + name + "', for players who have stepped on it");
    }

    private void tp(Player player, PadManager pads, String name) {
        TeleportPad pad = pads.get(name);
        Location to = pad == null ? null : pad.top();
        if (to == null) {
            TextUtils.error(player, pad == null ? "No teleport pad called " + name : "The pad's world is not loaded");
            return;
        }
        player.teleport(to);
    }

    @Override
    public List<String> tabComplete(Player player, String[] args) {
        if (args.length == 1) return TextUtils.getCompletions(args[0], ACTIONS);
        if (args.length == 2 && !args[0].equalsIgnoreCase("create")) {
            return TextUtils.getCompletions(args[1], List.copyOf(plugin.getPadManager().getPads().keySet()));
        }
        return List.of();
    }

    @Override
    public String getUsage() {
        return "<create|point|delete|list|tp> <pad>";
    }

    @Override
    public String getName() {
        return "pad";
    }

    @Override
    public String getDescription() {
        return "Teleport pads players unlock by stepping on, and portals at spawn that lead to them";
    }
}
