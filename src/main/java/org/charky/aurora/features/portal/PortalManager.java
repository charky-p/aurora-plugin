package org.charky.aurora.features.portal;

import org.bukkit.Location;
import org.charky.aurora.core.database.PortalStore;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class PortalManager {
    // A portal is a doorway, not a region, this is plenty
    private static final int MAX_VOLUME = 4096;

    private final PortalStore store;
    // Case-insensitive like mines, store calls use the portal's own name
    private final Map<String, Portal> portals = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

    public PortalManager(PortalStore store) {
        this.store = store;
        for (Portal portal : store.loadAll()) {
            portals.put(portal.getName(), portal);
        }
    }

    // Null if fine, otherwise why not
    public String create(String name, Location a, Location b) {
        if (a.getWorld() == null || !a.getWorld().equals(b.getWorld())) return "Both corners must be in the same world";

        Portal portal = new Portal(name, a.getWorld().getName(),
                a.getBlockX(), a.getBlockY(), a.getBlockZ(), b.getBlockX(), b.getBlockY(), b.getBlockZ());
        if (portal.volume() > MAX_VOLUME) return "Portal is bigger than " + MAX_VOLUME + " blocks";

        // Overlapping portals would fight over who sends the player where
        for (Portal other : portals.values()) {
            if (overlaps(other, portal)) return "Overlaps portal '" + other.getName() + "'";
        }

        portals.put(name, portal);
        store.save(portal);
        return null;
    }

    private static boolean overlaps(Portal a, Portal b) {
        return a.getWorld().equals(b.getWorld())
                && a.getX1() <= b.getX2() && b.getX1() <= a.getX2()
                && a.getY1() <= b.getY2() && b.getY1() <= a.getY2()
                && a.getZ1() <= b.getZ2() && b.getZ1() <= a.getZ2();
    }

    public void setDestination(String name, Location destination) {
        Portal portal = portals.get(name);
        if (portal == null || destination.getWorld() == null) return;

        portal.setDestination(destination.getWorld().getName(), destination.getX(), destination.getY(),
                destination.getZ(), destination.getYaw(), destination.getPitch());
        store.save(portal);
    }

    // Null or blank opens it to everyone
    public void setRequiredTag(String name, String tag) {
        Portal portal = portals.get(name);
        if (portal == null) return;

        portal.setRequiredTag(tag);
        store.save(portal);
    }

    public void remove(String name) {
        Portal portal = portals.remove(name);
        if (portal != null) store.remove(portal.getName());
    }

    public void rename(String oldName, String newName) {
        Portal portal = portals.remove(oldName);
        if (portal == null) return;

        String savedName = portal.getName();
        portal.rename(newName);
        portals.put(newName, portal);
        store.rename(savedName, newName);
    }

    // The portal a block is in, null for none
    public Portal portalAt(Location location) {
        for (Portal portal : portals.values()) {
            if (portal.contains(location)) return portal;
        }
        return null;
    }

    public boolean exists(String name) {
        return portals.containsKey(name);
    }

    public Portal get(String name) {
        return portals.get(name);
    }

    public List<String> getNames() {
        return new ArrayList<>(portals.keySet());
    }
}
