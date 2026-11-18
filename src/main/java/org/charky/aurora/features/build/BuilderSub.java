package org.charky.aurora.features.build;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.charky.aurora.common.util.TextUtils;
import org.charky.aurora.core.Aurora;
import org.charky.aurora.features.system.commands.AuroraSubCommand;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Who may go to build worlds and build there in creative without OP
 */
public class BuilderSub implements AuroraSubCommand {
    private static final List<String> ACTIONS = List.of("add", "remove", "list");

    private final Aurora plugin;

    public BuilderSub(Aurora plugin) {
        this.plugin = plugin;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        BuildManager build = plugin.getBuildManager();
        if (args.length == 0 || args[0].equalsIgnoreCase("list")) {
            Map<UUID, String> builders = build.getBuilders();
            TextUtils.display(sender, "<aqua><b>BUILDERS</b> <gray>(" + builders.size() + ")");
            builders.values().stream().sorted(String.CASE_INSENSITIVE_ORDER)
                    .forEach(name -> TextUtils.display(sender, " <dark_gray>» <white>" + name));
            TextUtils.display(sender, "<gray>Build worlds: <white>"
                    + (build.getWorlds().isEmpty() ? "<gray>none, see /aurora world build" : String.join(", ", build.getWorlds())));
            return;
        }
        if (args.length < 2 || !ACTIONS.contains(args[0].toLowerCase())) {
            TextUtils.usage(sender, getFullUsage());
            return;
        }

        // Someone who has joined before, online or not
        OfflinePlayer target = Bukkit.getPlayerExact(args[1]);
        if (target == null) target = Bukkit.getOfflinePlayerIfCached(args[1]);
        if (target == null) {
            TextUtils.error(sender, "No player called " + args[1] + " has joined this server");
            return;
        }
        String name = target.getName() != null ? target.getName() : args[1];

        if (args[0].equalsIgnoreCase("add")) {
            if (build.addBuilder(target.getUniqueId(), name)) TextUtils.success(sender, name + " is now a builder");
            else TextUtils.error(sender, name + " is already a builder");
        } else {
            if (build.removeBuilder(target.getUniqueId())) TextUtils.success(sender, name + " is no longer a builder");
            else TextUtils.error(sender, name + " is not a builder");
        }
    }

    @Override
    public List<String> tabComplete(Player player, String[] args) {
        if (args.length == 1) return TextUtils.getCompletions(args[0], ACTIONS);
        if (args.length == 2 && args[0].equalsIgnoreCase("remove")) {
            return TextUtils.getCompletions(args[1], plugin.getBuildManager().getBuilders().values());
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("add")) return TextUtils.playerCompletions(args[1]);
        return List.of();
    }

    @Override
    public String getUsage() {
        return "<add|remove|list> [player]";
    }

    @Override
    public String getName() {
        return "builder";
    }

    @Override
    public String getDescription() {
        return "Who may go to build worlds and build there in creative without OP";
    }
}
