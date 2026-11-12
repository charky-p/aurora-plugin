package org.charky.aurora.features.world;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.charky.aurora.common.util.TextUtils;
import org.charky.aurora.core.Aurora;
import org.charky.aurora.features.system.commands.AuroraSubCommand;

import java.util.*;

/**
 * World commands: create a void world that loads on every startup, list the
 * loaded worlds, teleport to one, or stop one loading
 */
public class WorldCommand implements AuroraSubCommand {
    private static final Map<String, String> SUBS = new LinkedHashMap<>();

    static {
        SUBS.put("create", "<name> - Create an empty world, or load an existing folder, every startup");
        SUBS.put("list", "- List the loaded worlds");
        SUBS.put("tp", "<world> [player] - Teleport to a world's spawn");
        SUBS.put("remove", "<name> - Stop loading a world on startup, its files are kept");
    }

    private final Aurora plugin;
    private final WorldManager worlds;

    public WorldCommand(Aurora plugin) {
        this.plugin = plugin;
        this.worlds = plugin.getWorldManager();
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length == 0 || !SUBS.containsKey(args[0].toLowerCase())) {
            if (args.length > 0) TextUtils.error(sender, "Unknown world command: " + args[0]);
            TextUtils.display(sender, "<aqua><b>WORLD COMMANDS</b>");
            SUBS.forEach((name, text) -> TextUtils.display(sender,
                    " <dark_gray>» <aqua>/aurora world " + name + " <gray>" + text));
            return;
        }

        String[] rest = Arrays.copyOfRange(args, 1, args.length);
        switch (args[0].toLowerCase()) {
            case "create" -> create(sender, rest);
            case "list" -> list(sender);
            case "tp" -> tp(sender, rest);
            case "remove" -> remove(sender, rest);
        }
    }

    private void create(CommandSender sender, String[] args) {
        if (args.length < 1) {
            TextUtils.usage(sender, "/aurora world create <name>");
            return;
        }

        String name = args[0];
        if (!WorldManager.isValidName(name)) {
            TextUtils.error(sender, "World names are lowercase letters, numbers and _ only");
            return;
        }
        if (worlds.isManaged(name)) {
            TextUtils.error(sender, "World '" + name + "' already exists");
            return;
        }

        World world = worlds.create(name);
        if (world == null) {
            TextUtils.error(sender, "Could not create world '" + name + "', check the console");
            return;
        }
        TextUtils.success(sender, "World '" + name + "' ready, it loads on every startup. Go there with /aurora world tp " + name);
    }

    private void list(CommandSender sender) {
        TextUtils.display(sender, "<aqua><b>WORLDS</b>");
        for (World world : Bukkit.getWorlds()) {
            String tag = worlds.isManaged(world.getName()) ? " <gray>(created by Aurora)" : "";
            String tp = "<click:run_command:'/aurora world tp " + world.getName() + "'><hover:show_text:'<green>Teleport'><green>➹</green></hover></click>";
            TextUtils.display(sender, " <dark_gray>» <white>" + world.getName() + tag
                    + " <gray>" + world.getPlayers().size() + " online " + tp);
        }
    }

    private void tp(CommandSender sender, String[] args) {
        if (args.length < 1) {
            TextUtils.usage(sender, "/aurora world tp <world> [player]");
            return;
        }

        World world = Bukkit.getWorld(args[0]);
        if (world == null) {
            TextUtils.error(sender, "World '" + args[0] + "' is not loaded");
            return;
        }

        Player target;
        if (args.length >= 2) {
            target = Bukkit.getPlayer(args[1]);
            if (target == null) {
                TextUtils.error(sender, "Player '" + args[1] + "' is not online");
                return;
            }
        } else if (sender instanceof Player player) {
            target = player;
        } else {
            TextUtils.error(sender, "Console must specify a player to teleport");
            return;
        }

        // The spawn set with /aurora setspawn, or the world's own
        Location spawn = plugin.getSpawn(world);
        target.teleport(spawn != null ? spawn : world.getSpawnLocation());
        TextUtils.success(sender, "Teleported " + target.getName() + " to " + world.getName());
    }

    private void remove(CommandSender sender, String[] args) {
        if (args.length < 1) {
            TextUtils.usage(sender, "/aurora world remove <name>");
            return;
        }
        if (!worlds.remove(args[0])) {
            TextUtils.error(sender, "World '" + args[0] + "' was not created by Aurora");
            return;
        }
        TextUtils.success(sender, "World '" + args[0] + "' will not load on the next startup, its files are kept");
    }

    @Override
    public List<String> tabComplete(Player player, String[] args) {
        if (args.length == 1) return TextUtils.getCompletions(args[0], SUBS.keySet());
        if (args.length == 2) {
            return switch (args[0].toLowerCase()) {
                case "tp" -> TextUtils.getCompletions(args[1], Bukkit.getWorlds().stream().map(World::getName).toList());
                case "remove" -> TextUtils.getCompletions(args[1], worlds.getManaged());
                default -> Collections.emptyList();
            };
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("tp")) {
            return TextUtils.getCompletions(args[2], Bukkit.getOnlinePlayers().stream().map(Player::getName).toList());
        }
        return Collections.emptyList();
    }

    @Override
    public String getUsage() {
        return "<create|list|tp|remove>";
    }

    @Override
    public String getName() {
        return "world";
    }

    @Override
    public String getDescription() {
        return "Create and visit the void worlds regions live in";
    }
}
