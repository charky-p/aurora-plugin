package org.charky.aurora.features.loadout;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.charky.aurora.common.util.TextUtils;
import org.charky.aurora.core.Aurora;
import org.charky.aurora.features.system.commands.AuroraSubCommand;

import java.util.List;

/**
 * Switches an admin's loadout where they stand: slot 1 (main), 2 or 3, or experimental.
 * The one picked is played in every world until auto or leaving, instead of the one each
 * world plays with. On its own it shows which loadout is in play and whether it was picked
 */
public class LoadoutSub implements AuroraSubCommand {
    private static final String AUTO = "auto";
    private static final String USAGE = "[1|2|3|experimental|auto]";

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
            TextUtils.send(player, "<gray>Loadout in play: <white>" + loadouts.activeOf(player.getUniqueId()).label()
                    + (pick != null ? " <gray>(picked, every world until auto)" : " <gray>(the world's)"));
            return;
        }

        String choice = args[0].toLowerCase();
        if (choice.equals(AUTO)) {
            loadouts.pin(player, null);
            TextUtils.success(player, "Back to the loadout each world plays with");
            return;
        }
        // main still works for slot 1
        Loadout loadout = Loadout.byName(choice);
        if (loadout == null) {
            TextUtils.usage(sender, "/aurora loadout " + USAGE);
            return;
        }
        loadouts.pin(player, loadout);
        TextUtils.success(player, "Playing loadout " + loadout.label() + " in every world until /a loadout auto or you leave");
    }

    @Override
    public List<String> tabComplete(Player player, String[] args) {
        if (args.length != 1) return List.of();
        return TextUtils.getCompletions(args[0], List.of(Loadout.MAIN.label(), Loadout.SLOT_2.label(),
                Loadout.SLOT_3.label(), Loadout.EXPERIMENTAL.label(), AUTO));
    }

    @Override
    public String getUsage() {
        return USAGE;
    }

    @Override
    public String getName() {
        return "loadout";
    }

    @Override
    public String getDescription() {
        return "Switch to loadout slot 1, 2, 3 or experimental, kept in every world until auto or you leave";
    }
}
