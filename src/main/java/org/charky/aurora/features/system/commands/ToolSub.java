package org.charky.aurora.features.system.commands;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.charky.aurora.common.util.TextUtils;
import org.charky.aurora.core.Aurora;
import org.charky.aurora.core.DataRegistry;
import org.charky.aurora.features.tool.Realms.Realm;
import org.charky.aurora.features.tool.ToolLevels;
import org.charky.aurora.features.tool.ToolManager;
import org.charky.aurora.features.tool.ToolType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Aurora admin command for the tools that live on the player: hand one over, set a
 * level in a realm, or read what a player has where they stand
 */
public class ToolSub implements AuroraSubCommand {
    private static final List<String> MODES = List.of("give", "set", "info");

    private final DataRegistry registry;
    private final ToolManager tools;

    public ToolSub(Aurora plugin) {
        this.registry = plugin.getRegistry();
        this.tools = plugin.getToolManager();
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length < 3 || !MODES.contains(args[1].toLowerCase())) {
            TextUtils.usage(sender, getFullUsage());
            return;
        }

        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) {
            TextUtils.error(sender, "Player not found: " + args[0]);
            return;
        }
        ToolType tool = registry.getToolType(args[2]);
        if (tool == null) {
            TextUtils.error(sender, "No tool called " + args[2] + ", there is " + String.join(", ", registry.getToolIds()));
            return;
        }

        switch (args[1].toLowerCase()) {
            case "give" -> {
                if (tools.give(target, tool)) TextUtils.success(sender, "Gave " + target.getName() + " their " + tool.id());
                else TextUtils.warn(sender, target.getName() + " already carries it, or has no room");
            }
            case "info" -> info(sender, target, tool);
            default -> set(sender, target, tool, args);
        }
    }

    private void info(CommandSender sender, Player target, ToolType tool) {
        Realm realm = tools.realmOf(target);
        if (realm == null) {
            TextUtils.error(sender, "There are no realms");
            return;
        }

        Map<String, Integer> stats = tools.effective(target.getUniqueId(), realm, tool.id());
        TextUtils.send(sender, "<yellow>" + target.getName() + "'s " + tool.id() + " in " + realm.id()
                + "<gray>: " + (stats.isEmpty() ? "nothing yet" : describe(tool, stats)));
        for (Realm link : registry.getRealms().chain(realm)) {
            if (link != realm) TextUtils.send(sender, "<dark_gray>builds on " + link.id());
        }
    }

    private static String describe(ToolType tool, Map<String, Integer> stats) {
        List<String> parts = new ArrayList<>();
        stats.forEach((stat, level) -> parts.add(stat.equals(ToolLevels.MATERIAL)
                ? ToolLevels.material(tool.materials(), level).toLowerCase() : stat + " " + level));
        return String.join(", ", parts);
    }

    // set <tool> <stat> <level> [realm], the material by its name
    private void set(CommandSender sender, Player target, ToolType tool, String[] args) {
        if (args.length < 5) {
            TextUtils.usage(sender, getFullUsage());
            return;
        }

        Realm realm = args.length > 5 ? registry.getRealms().get(args[5]) : tools.realmOf(target);
        if (realm == null) {
            TextUtils.error(sender, "No realm called " + (args.length > 5 ? args[5] : "that"));
            return;
        }

        String stat = args[3].toLowerCase();
        int level;
        if (stat.equals(ToolLevels.MATERIAL)) {
            level = ToolLevels.materialLevel(tool.materials(), args[4]);
            if (level < 0) {
                TextUtils.error(sender, "A " + tool.id() + " comes in " + String.join(", ", tool.materials()));
                return;
            }
        } else {
            if (!registry.hasEnchant(stat)) {
                TextUtils.error(sender, "No enchant called " + stat);
                return;
            }
            try {
                level = Integer.parseInt(args[4]);
            } catch (NumberFormatException e) {
                TextUtils.error(sender, "Invalid level: " + args[4]);
                return;
            }
            if (level < 0 || level > 1000) {
                TextUtils.error(sender, "Level must be 0 to 1000");
                return;
            }
        }

        // The level asked for is what the tool ends up with there, the realms it builds on included
        int total = tools.setTotal(target.getUniqueId(), realm, tool.id(), stat, level);
        String shown = stat.equals(ToolLevels.MATERIAL) ? ToolLevels.material(tool.materials(), total).toLowerCase()
                : String.valueOf(total);
        TextUtils.success(sender, target.getName() + "'s " + tool.id() + " " + stat + " in " + realm.id() + " is now " + shown);
        if (total != level) {
            TextUtils.error(sender, "Other realms it builds on already give more, set it there to go lower");
        }
    }

    @Override
    public List<String> tabComplete(Player player, String[] args) {
        if (args.length == 1) return TextUtils.playerCompletions(args[0]);
        if (args.length == 2) return TextUtils.getCompletions(args[1], MODES);
        if (args.length == 3) return TextUtils.getCompletions(args[2], registry.getToolIds());
        if (!args[1].equalsIgnoreCase("set")) return List.of();

        if (args.length == 4) {
            List<String> stats = new ArrayList<>(registry.getEnchantIds());
            stats.add(ToolLevels.MATERIAL);
            return TextUtils.getCompletions(args[3], stats);
        }
        if (args.length == 5 && args[3].equalsIgnoreCase(ToolLevels.MATERIAL)) {
            ToolType tool = registry.getToolType(args[2]);
            return tool == null ? List.of() : TextUtils.getCompletions(args[4], tool.materials());
        }
        if (args.length == 6) return TextUtils.getCompletions(args[5], registry.getRealms().ids());
        return List.of();
    }

    @Override
    public String getUsage() {
        return "<player> <give|info|set> <tool> [<stat> <level> [realm]]";
    }

    @Override
    public String getName() {
        return "tool";
    }

    @Override
    public String getDescription() {
        return "Give a player a tool that lives on them, or set its levels in a realm";
    }
}
