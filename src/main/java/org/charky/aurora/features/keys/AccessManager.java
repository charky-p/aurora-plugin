package org.charky.aurora.features.keys;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.charky.aurora.core.DataRegistry;
import org.charky.aurora.core.database.AccessStore;
import org.charky.aurora.core.database.Database;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

// Where each player may go: a pass like the Gormak Pass opens an access id for good,
// and a portal or warp locked to that id lets only those players through. Kept in
// Aurora's own database, so nothing outside the plugin can hand it out or take it away
public class AccessManager implements Listener {
    private final Database db;
    private final AccessStore store;
    private final DataRegistry registry;
    private final Map<UUID, Set<String>> access = new ConcurrentHashMap<>();

    public AccessManager(Database db, AccessStore store, DataRegistry registry) {
        this.db = db;
        this.store = store;
        this.registry = registry;
        store.loadAll().forEach((uuid, ids) -> {
            Set<String> set = ConcurrentHashMap.newKeySet();
            set.addAll(ids);
            access.put(uuid, set);
        });
    }

    public boolean has(Player player, String id) {
        Set<String> ids = access.get(player.getUniqueId());
        return ids != null && ids.contains(id.toLowerCase());
    }

    // True if it was not open before
    public boolean grant(UUID uuid, String id) {
        String key = id.toLowerCase();
        if (!access.computeIfAbsent(uuid, k -> ConcurrentHashMap.newKeySet()).add(key)) return false;
        db.async(() -> store.add(uuid, key));
        return true;
    }

    public boolean revoke(UUID uuid, String id) {
        Set<String> ids = access.get(uuid);
        String key = id.toLowerCase();
        if (ids == null || !ids.remove(key)) return false;
        db.async(() -> store.remove(uuid, key));
        return true;
    }

    public Set<String> of(UUID uuid) {
        Set<String> ids = access.get(uuid);
        return ids == null ? Collections.emptySet() : new TreeSet<>(ids);
    }

    public void reset(UUID uuid) {
        access.remove(uuid);
        db.async(() -> store.deleteAll(uuid));
    }

    // Access used to be vanilla scoreboard tags. A player who still has one gets it
    // moved over here once, and the tag is taken off
    @EventHandler(priority = EventPriority.LOWEST)
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        for (String id : registry.getUnlockTags()) {
            if (player.getScoreboardTags().contains(id)) {
                grant(player.getUniqueId(), id);
                player.removeScoreboardTag(id);
            }
        }
    }
}
