package org.charky.aurora.features.pvp;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.charky.aurora.common.util.Box;
import org.charky.aurora.common.util.TextUtils;
import org.charky.aurora.core.database.Database;
import org.charky.aurora.core.database.SafeZoneStore;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

// PvP is on everywhere except in safe zones, boxes made with the wand. A hit counts
// as in a safe zone when either player stands in one, so nobody can hit out of one.
// Dying to a player costs nothing: the inventory, levels and xp are all kept
public class SafeZoneManager implements Listener {
    private final Database db;
    private final SafeZoneStore store;
    private final Map<String, Box> zones = new ConcurrentHashMap<>();

    public SafeZoneManager(Database db, SafeZoneStore store) {
        this.db = db;
        this.store = store;
        zones.putAll(store.loadAll());
    }

    public boolean exists(String name) {
        return zones.containsKey(name.toLowerCase());
    }

    // Null when made, else why not
    public String create(String name, Location a, Location b) {
        String key = name.toLowerCase();
        if (zones.containsKey(key)) return "Safe zone '" + key + "' already exists";
        if (a.getWorld() == null || !a.getWorld().equals(b.getWorld())) return "Both corners have to be in the same world";
        Box box = Box.between(a, b);
        zones.put(key, box);
        db.async(() -> store.save(key, box));
        return null;
    }

    public boolean delete(String name) {
        String key = name.toLowerCase();
        if (zones.remove(key) == null) return false;
        db.async(() -> store.delete(key));
        return true;
    }

    public Map<String, Box> getZones() {
        return new LinkedHashMap<>(zones);
    }

    public boolean isSafe(Location location) {
        for (Box box : zones.values()) {
            if (box.contains(location)) return true;
        }
        return false;
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onPvp(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim)) return;
        Player attacker = switch (event.getDamager()) {
            case Player player -> player;
            case Projectile projectile when projectile.getShooter() instanceof Player shooter -> shooter;
            default -> null;
        };
        if (attacker == null || attacker.equals(victim)) return;
        if (isSafe(victim.getLocation()) || isSafe(attacker.getLocation())) {
            event.setCancelled(true);
            TextUtils.error(attacker, "No PvP in a safe zone");
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPvpDeath(PlayerDeathEvent event) {
        Player killer = event.getEntity().getKiller();
        if (killer == null || killer.equals(event.getEntity())) return;
        event.setKeepInventory(true);
        event.getDrops().clear();
        event.setKeepLevel(true);
        event.setDroppedExp(0);
    }
}
