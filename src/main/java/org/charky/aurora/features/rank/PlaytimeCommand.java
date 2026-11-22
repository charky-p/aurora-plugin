package org.charky.aurora.features.rank;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.charky.aurora.common.util.TextUtils;
import org.jetbrains.annotations.NotNull;

import java.util.List;

// /playtime [player], how long someone has played and how long until their next
// playtime rank. Only online players, playtime lives on the loaded profile. Ranks
// not out yet are skipped, past the last released one it says so
public class PlaytimeCommand implements TabExecutor {
    private final RankManager ranks;

    public PlaytimeCommand(RankManager ranks) {
        this.ranks = ranks;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String @NotNull [] args) {
        Player target;
        if (args.length > 0) {
            target = Bukkit.getPlayerExact(args[0]);
            if (target == null) {
                TextUtils.error(sender, args[0] + " is not online");
                return true;
            }
        } else if (sender instanceof Player player) {
            target = player;
        } else {
            TextUtils.usage(sender, "/playtime <player>");
            return true;
        }

        boolean self = target == sender;
        long played = ranks.playtimeOf(target);
        TextUtils.send(sender, "<gray>" + (self ? "You have" : "<white>" + target.getName() + "</white> has")
                + " played <white>" + duration(played));

        Rank next = next(played);
        if (next == null) {
            TextUtils.send(sender, "<gray>" + (self ? "You have" : "They have") + " every playtime rank there is for now");
        } else {
            TextUtils.send(sender, "<gray>Next rank " + next.display() + "<gray> in <white>"
                    + duration(next.requirement() - played));
        }
        return true;
    }

    // The first released playtime rank not reached yet, null past the last
    private static Rank next(long played) {
        for (Rank rank : Ranks.ofKind(Rank.Kind.PLAYTIME)) {
            if (rank.requirement() > played && Ranks.isReleased(rank)) return rank;
        }
        return null;
    }

    // 3h 25m, or 12m under an hour
    private static String duration(long seconds) {
        long hours = seconds / 3600;
        long minutes = seconds % 3600 / 60;
        return hours > 0 ? hours + "h " + minutes + "m" : Math.max(minutes, 0) + "m";
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                      @NotNull String label, @NotNull String @NotNull [] args) {
        return args.length == 1 ? TextUtils.playerCompletions(args[0]) : List.of();
    }
}
