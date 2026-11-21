package org.charky.aurora.features.loadout;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.charky.aurora.common.util.TextUtils;
import org.charky.aurora.core.Aurora;
import org.charky.aurora.features.system.commands.AuroraSubCommand;

import java.util.List;

/**
 * Switches an admin's loadout where they stand. The one picked is played in every world
 * until auto or leaving, instead of the one each world plays with. On its own it shows
 * which loadout is in play and whether it was picked
 */
public class LoadoutSub implements AuroraSubCommand {
    private static final String AUTO = "auto";

    private final Aurora plugin;

    public LoadoutSub(Aurora plugin) {
        this.plugin = plugin;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            TextUtils.error(sender, "Only players have a loadout");
            return;
        }
        LoadoutManager loadouts = plugin.getLoadoutManager();
        if (args.length == 0) {
            Loadout pick = loadouts.pinnedOf(player.getUniqueId());
            TextUtils.send(player, "<gray>Loadout in play: <white>" + loadouts.activeOf(player.getUniqueId()).id()
                    + (pick != null ? " <gray>(picked, every world until auto)" : " <gray>(the world's)"));
            return;
        }

        String choice = args[0].toLowerCase();
        if (choice.equals(AUTO)) {
            loadouts.pin(player, null);
            TextUtils.success(player, "Back to the loadout each world plays with");
            return;
        }
        Loadout loadout = choice.equals(Loadout.MAIN.id()) ? Loadout.MAIN
                : choice.equals(Loadout.EXPERIMENTAL.id()) ? Loadout.EXPERIMENTAL : null;
        if (loadout == null) {
            TextUtils.usage(sender, "/aurora loadout [main|experimental|auto]");
            return;
        }
        loadouts.pin(player, loadout);
        TextUtils.success(player, "Playing your " + loadout.id() + " loadout in every world until /a loadout auto or you leave");
    }

    @Override
    public List<String> tabComplete(Player player, String[] args) {
        if (args.length != 1) return List.of();
        return TextUtils.getCompletions(args[0], List.of(Loadout.MAIN.id(), Loadout.EXPERIMENTAL.id(), AUTO));
    }

    @Override
    public String getUsage() {
        return "[main|experimental|auto]";
    }

    @Override
    public String getName() {
        return "loadout";
    }

    @Override
    public String getDescription() {
        return "Switch your loadout here, kept in every world until auto or you leave";
    }
}
