package org.charky.aurora.features.tutorial;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.charky.aurora.common.util.TextUtils;
import org.charky.aurora.core.Aurora;
import org.charky.aurora.features.system.commands.AuroraSubCommand;

import java.util.List;

/**
 * Aurora admin command for the first-join tutorial: where it starts, and sending a
 * player through it again or out of it
 */
public class TutorialSub implements AuroraSubCommand {
    private static final List<String> ACTIONS = List.of("setstart", "start", "skip", "tp");

    private final Aurora plugin;

    public TutorialSub(Aurora plugin) {
        this.plugin = plugin;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length < 1 || !ACTIONS.contains(args[0].toLowerCase())) {
            TextUtils.usage(sender, getFullUsage());
            return;
        }
        TutorialManager tutorial = plugin.getTutorialManager();

        switch (args[0].toLowerCase()) {
            case "setstart" -> {
                if (!(sender instanceof Player player)) {
                    TextUtils.error(sender, "Stand where the tutorial starts, from in game");
                    return;
                }
                tutorial.setStart(player.getLocation());
                TextUtils.success(sender, "New players start the tutorial here");
            }
            case "start" -> {
                Player target = AuroraSubCommand.targetPlayer(sender, args, 1);
                if (target == null) return;
                tutorial.begin(target);
                TextUtils.success(sender, target.getName() + " is in the tutorial from the first step");
            }
            case "skip" -> {
                Player target = AuroraSubCommand.targetPlayer(sender, args, 1);
                if (target == null) return;
                if (tutorial.skip(target)) TextUtils.success(sender, target.getName() + " is out of the tutorial");
                else TextUtils.error(sender, target.getName() + " is not in the tutorial");
            }
            case "tp" -> {
                if (!(sender instanceof Player player)) return;
                if (tutorial.getStart() == null) TextUtils.error(sender, "No start set, use /aurora tutorial setstart");
                else player.teleport(tutorial.getStart());
            }
        }
    }

    @Override
    public List<String> tabComplete(Player player, String[] args) {
        if (args.length == 1) return TextUtils.getCompletions(args[0], ACTIONS);
        if (args.length == 2 && List.of("start", "skip").contains(args[0].toLowerCase())) {
            return TextUtils.playerCompletions(args[1]);
        }
        return List.of();
    }

    @Override
    public String getUsage() {
        return "<setstart|start|skip|tp> [player]";
    }

    @Override
    public String getName() {
        return "tutorial";
    }

    @Override
    public String getDescription() {
        return "The first-join tutorial: set where it starts, put a player through it or take them out";
    }

    @Override
    public List<String> getInfo() {
        return List.of(
                "setstart - new players start the tutorial where you stand, by the Cobblestone Quarry",
                "start [player] - put a player through it again from the first step",
                "skip [player] - take a player out of it, no reward",
                "tp - go to where it starts",
                "New players get it on their first join. Finishing pays " + TutorialManager.ESSENCE_REWARD
                        + " Essence and the Newcomer suffix");
    }
}
