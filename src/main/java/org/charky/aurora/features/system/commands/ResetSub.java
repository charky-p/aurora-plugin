package org.charky.aurora.features.system.commands;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.charky.aurora.common.util.TextUtils;
import org.charky.aurora.core.Aurora;
import org.charky.aurora.features.player.PlayerData;
import org.charky.aurora.features.player.StarterKit;

import java.util.List;

/**
 * Aurora admin command to send a player back to the start, for testing a run from
 * nothing. "tools" resets only the tool levels. "all" also empties the backpack and
 * inventory, sets level, XP, kills and blocks mined to 0, locks the portals again,
 * hands out the starter kit and starts the tutorial, as on a first join. Essence, ranks, cosmetics, perks and collections are never touched
 */
public class ResetSub implements AuroraSubCommand {
    private static final List<String> SCOPES = List.of("tools", "all");

    private final Aurora plugin;

    public ResetSub(Aurora plugin) {
        this.plugin = plugin;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length < 2 || !SCOPES.contains(args[1].toLowerCase())) {
            TextUtils.usage(sender, getFullUsage());
            return;
        }

        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) {
            TextUtils.error(sender, "Player not found: " + args[0]);
            return;
        }

        boolean all = args[1].equalsIgnoreCase("all");
        if (all) {
            plugin.getBackpackManager().clear(target.getUniqueId());
            target.getInventory().clear();
            for (String tag : plugin.getRegistry().getUnlockTags()) target.removeScoreboardTag(tag);

            PlayerData data = plugin.getPlayerManager().get(target);
            if (data != null) {
                data.setProgress(0, 0);
                data.setKills(0);
                data.setMined(0);
                data.setEssence(0);
            }
            plugin.getPerkManager().reset(target);
            plugin.getCollectionManager().reset(target.getUniqueId());
            plugin.getArmoryManager().reset(target.getUniqueId());
            plugin.getCosmeticManager().reset(target);
            plugin.getRankManager().reset(target);
            plugin.getLevelManager().show(target);
            plugin.getScoreboardManager().updateBoard(target);
        }

        // Last, so the starter tools land in the emptied inventory
        plugin.getToolManager().reset(target);
        if (all) {
            StarterKit.give(target, plugin.getRegistry());
            // As on a first join, so the tutorial plays again
            plugin.getTutorialManager().begin(target);
        }
        TextUtils.success(sender, "Reset " + target.getName() + (all ? " to a fresh start" : "'s tools"));
    }

    @Override
    public List<String> tabComplete(Player player, String[] args) {
        if (args.length == 1) return TextUtils.playerCompletions(args[0]);
        if (args.length == 2) return TextUtils.getCompletions(args[1], SCOPES);
        return List.of();
    }

    @Override
    public String getUsage() {
        return "<player> <tools|all>";
    }

    @Override
    public String getName() {
        return "reset";
    }

    @Override
    public String getDescription() {
        return "Reset a player's tools, or everything they have as if they had just joined";
    }
}
