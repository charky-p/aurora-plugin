package org.charky.aurora.features.portal;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPortalEvent;
import org.bukkit.event.player.*;
import org.charky.aurora.core.Aurora;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

// Walking into a portal sends you to its destination. A player standing in a portal
// only triggers it again after stepping out, so arriving inside the return portal
// does not bounce them straight back
public class PortalListener implements Listener {
    private final Aurora plugin;
    private final PortalManager portals;
    // The portal each player is standing in right now, missing for none
    private final Map<UUID, String> inside = new HashMap<>();

    public PortalListener(Aurora plugin) {
        this.plugin = plugin;
        this.portals = plugin.getPortalManager();
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        Location to = event.getTo();
        if (event.getFrom().getBlockX() == to.getBlockX()
                && event.getFrom().getBlockY() == to.getBlockY()
                && event.getFrom().getBlockZ() == to.getBlockZ()
                && event.getFrom().getWorld() == to.getWorld()) return;

        Player player = event.getPlayer();
        Portal portal = portals.portalAt(to);
        if (portal == null) {
            inside.remove(player.getUniqueId());
            return;
        }

        if (portal.getName().equals(inside.get(player.getUniqueId()))) return;
        inside.put(player.getUniqueId(), portal.getName());

        Location destination = portal.getDestination();
        if (destination == null) {
            player.sendActionBar(Component.text(portal.hasDestination()
                    ? "This portal's world is not loaded" : "This portal leads nowhere yet", NamedTextColor.RED));
            return;
        }

        // Next tick, teleporting inside a move event is unreliable
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (!player.isOnline()) return;
            player.teleport(destination);
            player.playSound(destination, Sound.ENTITY_ENDERMAN_TELEPORT, 0.7f, 1f);
        });
    }

    // Any arrival, by portal, /spawn or a command, counts as already standing in
    // whatever portal is there
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTeleport(PlayerTeleportEvent event) {
        track(event.getPlayer(), event.getTo());
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        track(event.getPlayer(), event.getPlayer().getLocation());
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        track(event.getPlayer(), event.getRespawnLocation());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        inside.remove(event.getPlayer().getUniqueId());
    }

    // Portals are often framed with nether portal blocks for the look, vanilla must
    // not send anyone to the nether from inside one
    @EventHandler(ignoreCancelled = true)
    public void onVanillaPortal(PlayerPortalEvent event) {
        if (portals.portalAt(event.getFrom()) != null) event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityVanillaPortal(EntityPortalEvent event) {
        if (portals.portalAt(event.getFrom()) != null) event.setCancelled(true);
    }

    private void track(Player player, Location location) {
        Portal portal = location == null ? null : portals.portalAt(location);
        if (portal == null) inside.remove(player.getUniqueId());
        else inside.put(player.getUniqueId(), portal.getName());
    }
}
