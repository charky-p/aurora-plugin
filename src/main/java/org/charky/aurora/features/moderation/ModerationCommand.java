package org.charky.aurora.features.moderation;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.charky.aurora.common.util.TextUtils;
import org.charky.aurora.features.moderation.Punishment.Type;
import org.jetbrains.annotations.NotNull;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

// /mute, /unmute, /kick, /ban, /unban and /history.
//  - mods: /mute <player> [reason] with the escalating ladder (15m, 1h, 1d, 3d, then a
//    week each time, never longer), /unmute, /kick and /history
//  - admins: also /mute <player> <length|perm> [reason], /ban <player> [length|perm]
//    [reason] (forever without a length) and /unban
public class ModerationCommand implements TabExecutor {
    private static final SimpleDateFormat DATE = new SimpleDateFormat("yyyy-MM-dd HH:mm");

    private final ModerationManager moderation;

    public ModerationCommand(ModerationManager moderation) {
        this.moderation = moderation;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String @NotNull [] args) {
        String name = command.getName().toLowerCase();
        boolean admin = sender.hasPermission(StaffPermissions.ADMIN);
        boolean mod = admin || sender.hasPermission(StaffPermissions.MOD);
        boolean adminOnly = name.equals("ban") || name.equals("unban");
        if (adminOnly ? !admin : !mod) {
            TextUtils.error(sender, "You can't do that");
            return true;
        }
        if (args.length == 0) {
            TextUtils.usage(sender, command.getUsage());
            return true;
        }
        OfflinePlayer target = find(args[0]);
        if (target == null) {
            TextUtils.error(sender, "No player called " + args[0] + " has been on the server");
            return true;
        }
        String who = target.getName() == null ? args[0] : target.getName();
        String by = sender.getName();
        String[] rest = Arrays.copyOfRange(args, 1, args.length);

        switch (name) {
            case "mute" -> mute(sender, target, who, by, rest, admin);
            case "unmute" -> {
                if (moderation.lift(target.getUniqueId(), Type.MUTE)) ModerationManager.tellStaff(by + " unmuted " + who);
                else TextUtils.error(sender, who + " is not muted");
            }
            case "kick" -> {
                Player online = target.getPlayer();
                if (online == null) {
                    TextUtils.error(sender, who + " is not online");
                    return true;
                }
                String reason = String.join(" ", rest);
                moderation.give(target.getUniqueId(), who, Type.KICK, reason, by, 0);
                online.kick(TextUtils.parse("<red><b>Kicked</b>" + (reason.isBlank() ? "" : "\n<gray>" + reason)));
                ModerationManager.tellStaff(by + " kicked " + who + (reason.isBlank() ? "" : ": " + reason));
            }
            case "ban" -> ban(target, who, by, rest);
            case "unban" -> {
                if (moderation.lift(target.getUniqueId(), Type.BAN)) ModerationManager.tellStaff(by + " unbanned " + who);
                else TextUtils.error(sender, who + " is not banned");
            }
            default -> history(sender, target, who);
        }
        return true;
    }

    private void mute(CommandSender sender, OfflinePlayer target, String who, String by, String[] rest, boolean admin) {
        long length;
        String reason;
        long asked = rest.length > 0 ? Durations.parse(rest[0]) : -1;
        if (asked >= 0) {
            if (!admin) {
                TextUtils.error(sender, "Mods can't pick the length, each mute of a player is longer than the last, up to a week");
                return;
            }
            length = asked;
            reason = String.join(" ", Arrays.copyOfRange(rest, 1, rest.length));
        } else {
            length = Durations.modMute(moderation.mutesOf(target.getUniqueId()));
            reason = String.join(" ", rest);
        }
        Punishment mute = moderation.give(target.getUniqueId(), who, Type.MUTE, reason, by, length);
        Player online = target.getPlayer();
        if (online != null) TextUtils.error(online, "You have been muted " + ModerationManager.describe(mute));
        ModerationManager.tellStaff(by + " muted " + who + " " + ModerationManager.describe(mute));
    }

    private void ban(OfflinePlayer target, String who, String by, String[] rest) {
        long asked = rest.length > 0 ? Durations.parse(rest[0]) : -1;
        long length = asked >= 0 ? asked : Durations.FOREVER;
        String reason = String.join(" ", Arrays.copyOfRange(rest, asked >= 0 ? 1 : 0, rest.length));
        Punishment ban = moderation.give(target.getUniqueId(), who, Type.BAN, reason, by, length);
        Player online = target.getPlayer();
        if (online != null) {
            online.kick(TextUtils.parse("<red><b>You are banned</b>\n<gray>" + ModerationManager.describe(ban)
                    + "\n\n<dark_gray>Appeal on the Discord"));
        }
        ModerationManager.tellStaff(by + " banned " + who + " " + ModerationManager.describe(ban));
    }

    private void history(CommandSender sender, OfflinePlayer target, String who) {
        List<Punishment> history = moderation.history(target.getUniqueId());
        TextUtils.display(sender, "<aqua><b>HISTORY</b> <white>" + who + " <gray>(" + history.size() + ")");
        long now = System.currentTimeMillis();
        for (Punishment p : history) {
            String state = p.type() == Type.KICK ? "" : p.activeAt(now) ? " <red>in force" : p.lifted() ? " <gray>lifted" : " <gray>over";
            String length = p.type() == Type.KICK ? "" : " " + (p.forever() ? "forever" : Durations.describe(p.until() - p.created()));
            TextUtils.display(sender, " <dark_gray>» <white>" + p.type().name().toLowerCase() + length + state
                    + " <dark_gray>" + DATE.format(new Date(p.created())) + " by " + p.by()
                    + (p.reason().isBlank() ? "" : " <gray>" + p.reason()));
        }
    }

    // Online by name, or anyone who has been on before
    private static OfflinePlayer find(String name) {
        Player online = Bukkit.getPlayerExact(name);
        if (online != null) return online;
        return Bukkit.getOfflinePlayerIfCached(name);
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                      @NotNull String alias, @NotNull String @NotNull [] args) {
        if (args.length == 1) return TextUtils.playerCompletions(args[0]);
        boolean lengths = args.length == 2 && sender.hasPermission(StaffPermissions.ADMIN)
                && (command.getName().equals("mute") || command.getName().equals("ban"));
        return lengths ? TextUtils.getCompletions(args[1], List.of("30m", "1h", "1d", "1w", "perm")) : List.of();
    }
}
