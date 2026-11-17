package org.charky.aurora.features.player;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextReplacementConfig;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.persistence.PersistentDataType;
import org.charky.aurora.common.util.DataKeys;
import org.charky.aurora.common.util.TextUtils;
import org.charky.aurora.core.DataRegistry;
import org.charky.aurora.features.cosmetic.CosmeticManager;
import org.charky.aurora.features.cosmetic.Cosmetics;
import org.charky.aurora.features.spawner.MobData;

// The server's own lines in chat: a first join with its number, quits and deaths.
// An ordinary join is the player's join message cosmetic, and how a death reads is
// their death message cosmetic, see CosmeticManager
public class ServerMessages implements Listener {
    private final DataRegistry registry;
    private final CosmeticManager cosmetics;

    public ServerMessages(DataRegistry registry, CosmeticManager cosmetics) {
        this.registry = registry;
        this.cosmetics = cosmetics;
    }

    private static Component tag(String symbol, NamedTextColor color) {
        return Component.text("[", NamedTextColor.DARK_GRAY)
                .append(Component.text(symbol, color))
                .append(Component.text("] ", NamedTextColor.DARK_GRAY));
    }

    private static Component name(String name, NamedTextColor color) {
        return Component.text(name, color, TextDecoration.BOLD);
    }

    // HIGHEST, a first join outranks whichever join message the player wears
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (player.hasPlayedBefore()) return;

        StarterKit.give(player, registry);
        // Everyone who has been here before, and this one
        int number = Bukkit.getOfflinePlayers().length + 1;
        event.joinMessage(tag("+", NamedTextColor.GREEN)
                .append(name(player.getName(), NamedTextColor.AQUA))
                .append(Component.text(" has joined Auroramines for the first time ", NamedTextColor.AQUA))
                .append(Component.text("[", NamedTextColor.DARK_GRAY))
                .append(Component.text("#" + number, NamedTextColor.AQUA))
                .append(Component.text("]", NamedTextColor.DARK_GRAY)));
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        event.quitMessage(tag("-", NamedTextColor.RED)
                .append(name(event.getPlayer().getName(), NamedTextColor.GRAY))
                .append(Component.text(" has quit", NamedTextColor.GRAY)));
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        EntityDamageEvent cause = victim.getLastDamageCause();
        Component killer = cause instanceof EntityDamageByEntityEvent hit ? killerName(hit.getDamager()) : null;

        // The void has one line for everyone, it is not a fight to dress up
        if (killer == null && cause != null && cause.getCause() == EntityDamageEvent.DamageCause.VOID) {
            event.deathMessage(tag("☠", NamedTextColor.RED).append(name(victim.getName(), NamedTextColor.GRAY))
                    .append(Component.text(" fell into the void", NamedTextColor.GRAY)));
            return;
        }

        String template = cosmetics.deathMessage(victim.getUniqueId(), killer != null);
        Component line = TextUtils.parse(template.replace(Cosmetics.PLAYER_TAG, victim.getName()));
        if (killer != null) {
            Component by = killer;
            line = line.replaceText(TextReplacementConfig.builder()
                    .matchLiteral(Cosmetics.KILLER_TAG).replacement(by).build());
        }
        event.deathMessage(tag("☠", NamedTextColor.RED).append(line));
    }

    // An arrow is its shooter. One of our mobs goes by its name in the data, without
    // the health shown over its head
    private Component killerName(Entity damager) {
        if (damager instanceof Projectile projectile) {
            if (!(projectile.getShooter() instanceof Entity shooter)) return null;
            damager = shooter;
        }
        if (damager instanceof Player player) return name(player.getName(), NamedTextColor.GRAY);

        String mobId = damager.getPersistentDataContainer().get(DataKeys.MOB_KEY, PersistentDataType.STRING);
        MobData mob = mobId == null ? null : registry.getMob(mobId);
        return mob != null ? mob.displayName() : Component.text(damager.getName(), NamedTextColor.GRAY);
    }
}
