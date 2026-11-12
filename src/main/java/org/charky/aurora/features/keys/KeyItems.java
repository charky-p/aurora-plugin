package org.charky.aurora.features.keys;

import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.charky.aurora.common.util.TextUtils;
import org.charky.aurora.core.Aurora;
import org.charky.aurora.core.DataRegistry;
import org.charky.aurora.features.spawner.MobData;
import org.charky.aurora.features.spawner.MobSpawner;
import org.charky.aurora.features.spawner.SpawnerManager;

import java.util.List;

// Key items from resources_special.csv, used up by right clicking them in the backpack
// or the hand. An unlocks key gives the player a tag, portals locked to that tag let
// them through. Tags are vanilla scoreboard tags, so `/tag <player> add <tag>` works.
// A summons key wakes its mob at that mob's spawner, which never spawns it by itself
public final class KeyItems {
    // How close to the spawner a summon key must be used
    private static final double SUMMON_RANGE = 24.0;
    // Who hears that a boss was summoned
    private static final double ANNOUNCE_RANGE = 64.0;

    private KeyItems() {
    }

    public static boolean has(Player player, String tag) {
        return player.getScoreboardTags().contains(tag);
    }

    // True when the key was used and one should be taken away
    public static boolean use(Player player, DataRegistry registry, String itemId) {
        if (registry.getUnlock(itemId) != null) return unlock(player, registry, itemId);
        if (registry.getSummon(itemId) != null) return summon(player, registry, itemId);
        return false;
    }

    private static boolean unlock(Player player, DataRegistry registry, String itemId) {
        String tag = registry.getUnlock(itemId);
        Component name = itemName(registry, itemId);
        if (has(player, tag)) {
            player.sendActionBar(TextUtils.parse("<gray>You have already used a ").append(name));
            return false;
        }

        player.addScoreboardTag(tag);
        player.sendMessage(TextUtils.parse("<green>Used ").append(name).append(TextUtils.parse("<green>, a new portal is open to you")));
        player.playSound(player.getLocation(), Sound.BLOCK_VAULT_OPEN_SHUTTER, 1f, 1f);
        return true;
    }

    private static boolean summon(Player player, DataRegistry registry, String itemId) {
        String mobId = registry.getSummon(itemId);
        MobData mob = registry.getMob(mobId);
        if (mob == null) return false;

        MobSpawner spawner = nearestSpawner(player, mobId);
        if (spawner == null) {
            player.sendActionBar(TextUtils.parse("<red>Nothing stirs here, find where ").append(mob.displayName())
                    .append(TextUtils.parse("<red> rests")));
            return false;
        }

        String refused = spawner.summon();
        if (refused != null) {
            player.sendActionBar(TextUtils.parse("<red>" + refused));
            return false;
        }

        Location at = spawner.getLocation();
        Component message = TextUtils.parse("<gold>" + player.getName() + " <gray>has awoken ").append(mob.displayName());
        for (Player nearby : at.getWorld().getPlayers()) {
            if (nearby.getLocation().distanceSquared(at) > ANNOUNCE_RANGE * ANNOUNCE_RANGE) continue;
            nearby.sendMessage(message);
            nearby.playSound(nearby.getLocation(), Sound.ENTITY_WITHER_SPAWN, 0.6f, 1f);
        }
        return true;
    }

    // The closest spawner of this mob in the player's world, within SUMMON_RANGE
    private static MobSpawner nearestSpawner(Player player, String mobId) {
        SpawnerManager spawners = JavaPlugin.getPlugin(Aurora.class).getMobManager();
        Location here = player.getLocation();
        MobSpawner nearest = null;
        double best = SUMMON_RANGE * SUMMON_RANGE;

        for (String name : spawners.getSpawners()) {
            MobSpawner spawner = spawners.getSpawner(name);
            if (spawner == null || !spawner.getMobName().equalsIgnoreCase(mobId)) continue;
            if (!spawner.getWorld().equals(here.getWorld().getName())) continue;

            double distance = spawner.getLocation().distanceSquared(here);
            if (distance <= best) {
                best = distance;
                nearest = spawner;
            }
        }
        return nearest;
    }

    // What to tell a player turned away, the item that opens it or the bare tag
    public static Component missing(DataRegistry registry, String tag) {
        List<String> keys = registry.getKeysFor(tag);
        if (keys.isEmpty()) return TextUtils.parse("<red>This portal is locked");
        return TextUtils.parse("<red>You need a ").append(itemName(registry, keys.getFirst()))
                .append(TextUtils.parse("<red> to enter"));
    }

    private static Component itemName(DataRegistry registry, String itemId) {
        ItemStack item = registry.getAllItems().get(itemId);
        Component name = item == null ? null : item.getItemMeta().displayName();
        return name == null ? Component.text(itemId) : name;
    }
}
