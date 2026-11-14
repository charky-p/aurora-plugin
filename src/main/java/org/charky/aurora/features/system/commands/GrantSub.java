package org.charky.aurora.features.system.commands;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.charky.aurora.common.util.TextUtils;
import org.charky.aurora.core.Aurora;
import org.charky.aurora.features.player.PlayerData;

import java.util.List;

/**
 * Aurora admin command to give a player Essence, XP or playtime, for testing the
 * shop, levels and playtime ranks without earning them
 */
public class GrantSub implements AuroraSubCommand {
    private static final List<String> KINDS = List.of("essence", "xp", "hours");

    private final Aurora plugin;

    public GrantSub(Aurora plugin) {
        this.plugin = plugin;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length < 3) {
            TextUtils.usage(sender, getFullUsage());
            return;
        }

        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) {
            TextUtils.error(sender, "Player not found: " + args[0]);
            return;
        }

        PlayerData data = plugin.getPlayerManager().get(target);
        if (data == null) {
            TextUtils.error(sender, target.getName() + "'s profile is still loading");
            return;
        }

        String kind = args[1].toLowerCase();
        long amount;
        try {
            amount = Long.parseLong(args[2]);
        } catch (NumberFormatException e) {
            TextUtils.error(sender, "Invalid amount: " + args[2]);
            return;
        }
        if (amount < 1 || amount > 1_000_000) {
            TextUtils.error(sender, "Amount must be 1 to 1000000");
            return;
        }

        switch (kind) {
            case "essence" -> data.addEssence((int) amount);
            case "xp" -> plugin.getLevelManager().addXp(target, amount);
            // Counted by the playtime check within a minute, which hands out the ranks
            case "hours" -> data.addPlaytime(amount * 3600);
            default -> {
                TextUtils.usage(sender, getFullUsage());
                return;
            }
        }

        plugin.getScoreboardManager().updateBoard(target);
        TextUtils.success(sender, "Gave " + amount + " " + kind + " to " + target.getName());
    }

    @Override
    public List<String> tabComplete(Player player, String[] args) {
        if (args.length == 1) return TextUtils.playerCompletions(args[0]);
        if (args.length == 2) return TextUtils.getCompletions(args[1], KINDS);
        if (args.length == 3) return List.of("10", "100", "1000");
        return List.of();
    }

    @Override
    public String getUsage() {
        return "<player> <essence|xp|hours> <amount>";
    }

    @Override
    public String getName() {
        return "grant";
    }

    @Override
    public String getDescription() {
        return "Give a player Essence, XP or hours of playtime, for testing";
    }
}
