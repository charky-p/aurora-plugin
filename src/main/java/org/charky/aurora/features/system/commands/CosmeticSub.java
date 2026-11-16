package org.charky.aurora.features.system.commands;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.charky.aurora.common.util.TextUtils;
import org.charky.aurora.core.Aurora;
import org.charky.aurora.features.cosmetic.Cosmetic;
import org.charky.aurora.features.cosmetic.Cosmetics;
import org.charky.aurora.features.rank.Rank;
import org.charky.aurora.features.rank.Ranks;

import java.util.ArrayList;
import java.util.List;

/**
 * Aurora admin command to hand out or take back a rank or cosmetic. The only way
 * to get the staff and donor ranks, the donor suffixes and the second rank and
 * suffix slots
 */
public class CosmeticSub implements AuroraSubCommand {
    private static final List<String> MODES = List.of("give", "take");

    private final Aurora plugin;

    public CosmeticSub(Aurora plugin) {
        this.plugin = plugin;
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

        boolean give = args[1].equalsIgnoreCase("give");
        String id = args[2].toLowerCase();
        boolean changed;

        Rank rank = rankById(id);
        if (id.equalsIgnoreCase(Ranks.SECOND_SLOT)) {
            changed = plugin.getRankManager().setSecondSlot(target, give);
        } else if (rank != null) {
            changed = give ? plugin.getRankManager().grant(target, rank) : plugin.getRankManager().revoke(target, rank);
        } else if (id.equals(Cosmetics.SECOND_SUFFIX_SLOT) || Cosmetics.get(id) != null) {
            changed = give ? plugin.getCosmeticManager().give(target.getUniqueId(), id)
                    : plugin.getCosmeticManager().take(target.getUniqueId(), id);
        } else {
            TextUtils.error(sender, "No rank or cosmetic called " + args[2]);
            return;
        }

        if (!changed) {
            TextUtils.warn(sender, target.getName() + (give ? " already has " : " does not have ") + id);
            return;
        }
        TextUtils.success(sender, (give ? "Gave " : "Took ") + id + (give ? " to " : " from ") + target.getName());
    }

    // Only ranks that can be handed out: playtime ranks follow from hours played
    private static Rank rankById(String id) {
        for (Rank rank : Ranks.all()) {
            if (rank.id().equalsIgnoreCase(id) && grantable(rank)) return rank;
        }
        return null;
    }

    private static boolean grantable(Rank rank) {
        return rank.kind() == Rank.Kind.SHOP || rank.kind() == Rank.Kind.DONOR || rank.kind() == Rank.Kind.STAFF;
    }

    @Override
    public List<String> tabComplete(Player player, String[] args) {
        if (args.length == 1) return TextUtils.playerCompletions(args[0]);
        if (args.length == 2) return TextUtils.getCompletions(args[1], MODES);
        if (args.length == 3) {
            List<String> ids = new ArrayList<>();
            for (Rank rank : Ranks.all()) {
                if (grantable(rank)) ids.add(rank.id().toLowerCase());
            }
            for (Cosmetic cosmetic : Cosmetics.all()) ids.add(cosmetic.id());
            ids.add(Cosmetics.SECOND_SUFFIX_SLOT);
            ids.add(Ranks.SECOND_SLOT.toLowerCase());
            return TextUtils.searchCompletions(args[2], ids);
        }
        return List.of();
    }

    @Override
    public String getUsage() {
        return "<player> <give|take> <rank or cosmetic id>";
    }

    @Override
    public String getName() {
        return "cosmetic";
    }

    @Override
    public String getDescription() {
        return "Give or take a rank, a cosmetic or a second rank or suffix slot";
    }
}
