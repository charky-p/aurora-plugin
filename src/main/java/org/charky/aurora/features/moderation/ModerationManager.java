package org.charky.aurora.features.moderation;

import io.papermc.paper.event.player.AsyncChatEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.charky.aurora.common.util.TextUtils;
import org.charky.aurora.core.database.Database;
import org.charky.aurora.core.database.PunishmentStore;
import org.charky.aurora.features.moderation.Punishment.Type;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

// Mutes, bans and kicks. Every one is kept, ended or not, as a player's history; the
// ones still in force block chat (and private messages) or joining. Read from chat
// and login threads, so the list is safe to read from any thread
public class ModerationManager implements Listener {
    // Talking to other players: blocked while muted, like chat
    private static final Set<String> CHAT_COMMANDS = Set.of("msg", "tell", "w", "whisper", "me", "say", "r",
            "reply", "teammsg", "tm", "suggest");

    private final Database db;
    private final PunishmentStore store;
    private final List<Punishment> all;

    public ModerationManager(Database db, PunishmentStore store) {
        this.db = db;
        this.store = store;
        this.all = new CopyOnWriteArrayList<>(store.loadAll());
    }

    // The punishment of this kind still in force, null when there is none
    public Punishment active(UUID uuid, Type type) {
        long now = System.currentTimeMillis();
        for (Punishment punishment : all) {
            if (punishment.uuid().equals(uuid) && punishment.type() == type && punishment.activeAt(now)) return punishment;
        }
        return null;
    }

    public List<Punishment> history(UUID uuid) {
        List<Punishment> found = new ArrayList<>();
        for (Punishment punishment : all) {
            if (punishment.uuid().equals(uuid)) found.add(punishment);
        }
        return found;
    }

    public int mutesOf(UUID uuid) {
        return (int) all.stream().filter(p -> p.uuid().equals(uuid) && p.type() == Type.MUTE).count();
    }

    // Records it and returns it. A new mute or ban replaces one still in force
    public Punishment give(UUID uuid, String name, Type type, String reason, String by, long length) {
        if (type != Type.KICK) lift(uuid, type);
        long now = System.currentTimeMillis();
        long until = type == Type.KICK || length == Durations.FOREVER ? 0 : now + length;
        Punishment punishment = new Punishment(0, uuid, name, type, reason, by, now, until, false);
        all.add(punishment);
        db.async(() -> store.add(punishment));
        return punishment;
    }

    // Ends the mute or ban in force. False if there was none
    public boolean lift(UUID uuid, Type type) {
        boolean any = false;
        long now = System.currentTimeMillis();
        for (int i = 0; i < all.size(); i++) {
            Punishment p = all.get(i);
            if (p.uuid().equals(uuid) && p.type() == type && !p.lifted()) {
                if (p.activeAt(now)) any = true;
                all.set(i, new Punishment(p.id(), p.uuid(), p.name(), p.type(), p.reason(), p.by(), p.created(), p.until(), true));
            }
        }
        db.async(() -> store.lift(uuid, type));
        return any;
    }

    // What the player is told, and the reason and time left
    public static String describe(Punishment punishment) {
        String length = punishment.forever() ? "forever"
                : "for " + Durations.describe(Math.max(0, punishment.until() - System.currentTimeMillis()));
        return length + (punishment.reason().isBlank() ? "" : ": " + punishment.reason());
    }

    // Staff online are told what was done
    public static void tellStaff(String message) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.hasPermission(StaffPermissions.MOD)) TextUtils.send(player, "<dark_gray>[<red>Staff</red>]</dark_gray> " + message);
        }
        Bukkit.getConsoleSender().sendMessage(TextUtils.parse("<gray>[Staff] " + message));
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onChat(AsyncChatEvent event) {
        Punishment mute = active(event.getPlayer().getUniqueId(), Type.MUTE);
        if (mute == null) return;
        event.setCancelled(true);
        TextUtils.error(event.getPlayer(), "You are muted " + describe(mute));
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        String label = event.getMessage().substring(1).split(" ", 2)[0].toLowerCase(Locale.ROOT);
        int colon = label.indexOf(':');
        if (colon >= 0) label = label.substring(colon + 1);
        if (!CHAT_COMMANDS.contains(label)) return;
        Punishment mute = active(event.getPlayer().getUniqueId(), Type.MUTE);
        if (mute == null) return;
        event.setCancelled(true);
        TextUtils.error(event.getPlayer(), "You are muted " + describe(mute));
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onLogin(AsyncPlayerPreLoginEvent event) {
        Punishment ban = active(event.getUniqueId(), Type.BAN);
        if (ban == null) return;
        event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_BANNED,
                TextUtils.parse("<red><b>You are banned</b>\n<gray>" + describe(ban) + "\n\n<dark_gray>Appeal on the Discord"));
    }
}
