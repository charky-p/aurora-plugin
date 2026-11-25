package org.charky.aurora.features.gauntlet;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Breeze;
import org.bukkit.util.Vector;
import org.charky.aurora.features.combat.CustomHealth;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

// A vanilla breeze leaps every couple of seconds, and Paper has no event for it (it sets
// its own velocity, no jump). So every tick the Gauntlet's breezes, the Gales, are
// watched: one that just left the ground going up has leapt, and a leap within
// gale_jump_seconds (settings.csv) of its last one is stopped where it started
public class GaleJumps {
    // Going up faster than this off the ground is a leap, not a step or a knock
    private static final double LEAP_SPEED = 0.3;

    private final GauntletManager manager;
    private final Set<UUID> grounded = new HashSet<>();
    private final Map<UUID, Long> lastLeap = new HashMap<>();

    public GaleJumps(GauntletManager manager) {
        this.manager = manager;
    }

    public void tick() {
        long now = System.currentTimeMillis();
        long gap = Math.round(manager.data().setting("gale_jump_seconds", 8) * 1000);
        Set<UUID> seen = new HashSet<>();
        for (World world : Bukkit.getWorlds()) {
            if (!manager.arena().isGauntletWorld(world)) continue;
            for (Breeze breeze : world.getEntitiesByClass(Breeze.class)) {
                if (!CustomHealth.isTracked(breeze)) continue;
                UUID id = breeze.getUniqueId();
                seen.add(id);
                if (breeze.isOnGround()) {
                    grounded.add(id);
                    continue;
                }
                // Only the tick it leaves the ground
                if (!grounded.remove(id) || breeze.getVelocity().getY() <= LEAP_SPEED) continue;

                Long last = lastLeap.get(id);
                if (last != null && now - last < gap) breeze.setVelocity(new Vector(0, 0, 0));
                else lastLeap.put(id, now);
            }
        }
        grounded.retainAll(seen);
        lastLeap.keySet().retainAll(seen);
    }
}
