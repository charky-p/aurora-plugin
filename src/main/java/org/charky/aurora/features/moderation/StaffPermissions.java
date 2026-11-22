package org.charky.aurora.features.moderation;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.permissions.PermissionAttachment;
import org.bukkit.plugin.Plugin;
import org.charky.aurora.features.rank.Rank;
import org.charky.aurora.features.rank.RankManager;
import org.charky.aurora.features.rank.Ranks;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

// What a staff rank lets someone do. Granting or taking a staff rank, joining and
// switching loadout all redraw it, so the rank an admin hands out is the permission:
//  - mods (JUNIOR_MOD to HEAD_MOD): aurora.mod, mute with the escalating ladder, kick
//  - builders and devs: aurora.builder, the game setup commands, Axiom and WorldEdit
//  - admins (JUNIOR_ADMIN, ADMIN): aurora.admin, everything, as an op has
public class StaffPermissions implements Listener {
    public static final String MOD = "aurora.mod";
    public static final String BUILDER = "aurora.builder";
    public static final String ADMIN = "aurora.admin";

    private static final Set<String> MOD_RANKS = Set.of("JUNIOR_MOD", "MOD", "SENIOR_MOD", "HEAD_MOD");
    private static final Set<String> BUILDER_RANKS = Set.of("BUILDER", "SENIOR_BUILDER", "HEAD_BUILDER",
            "JUNIOR_DEV", "DEV", "SENIOR_DEV", "HEAD_DEV");
    private static final Set<String> ADMIN_RANKS = Set.of("JUNIOR_ADMIN", "ADMIN");
    // What a builder gets from the build plugins on top of aurora.builder
    private static final List<String> BUILD_TOOLS = List.of("axiom.*", "worldedit.*");

    private final Plugin plugin;
    private final RankManager ranks;
    private final Map<UUID, PermissionAttachment> attachments = new HashMap<>();

    public StaffPermissions(Plugin plugin, RankManager ranks) {
        this.plugin = plugin;
        this.ranks = ranks;
    }

    // The permissions this player's staff ranks give, empty for everyone else
    Set<String> granted(Player player) {
        Set<String> owned = new LinkedHashSet<>();
        for (Rank rank : Ranks.ofKind(Rank.Kind.STAFF)) {
            if (ranks.owns(player, rank)) owned.add(rank.id());
        }
        return permissionsFor(owned);
    }

    static Set<String> permissionsFor(Set<String> staffRanks) {
        Set<String> permissions = new LinkedHashSet<>();
        for (String rank : staffRanks) {
            if (ADMIN_RANKS.contains(rank)) permissions.add(ADMIN);
            if (MOD_RANKS.contains(rank)) permissions.add(MOD);
            if (BUILDER_RANKS.contains(rank)) {
                permissions.add(BUILDER);
                permissions.addAll(BUILD_TOOLS);
            }
        }
        return permissions;
    }

    public void update(Player player) {
        PermissionAttachment old = attachments.remove(player.getUniqueId());
        if (old != null) player.removeAttachment(old);

        Set<String> permissions = granted(player);
        if (!permissions.isEmpty()) {
            PermissionAttachment attachment = player.addAttachment(plugin);
            permissions.forEach(permission -> attachment.setPermission(permission, true));
            attachments.put(player.getUniqueId(), attachment);
        }
        // The commands the client may tab complete follow what they may run
        player.updateCommands();
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        update(event.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        attachments.remove(event.getPlayer().getUniqueId());
    }
}
