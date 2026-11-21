package org.charky.aurora.features.portal;

import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.charky.aurora.common.util.TextUtils;
import org.charky.aurora.core.database.Database;
import org.charky.aurora.core.database.PadStore;
import org.charky.aurora.features.keys.AccessManager;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

// Teleport pads: stepping on one unlocks it for good, on the loadout in play. What the
// unlock opens is an access id, so a portal at spawn locked to it with /aurora portal
// settag takes only the players who have been there
public class PadManager implements Listener {
    private final Database db;
    private final PadStore store;
    private final AccessManager access;
    private final Map<String, TeleportPad> pads = new ConcurrentHashMap<>();
    // By block, for the check on every step
    private final Map<String, TeleportPad> byBlock = new ConcurrentHashMap<>();

    public PadManager(Database db, PadStore store, AccessManager access) {
        this.db = db;
        this.store = store;
        this.access = access;
        store.loadAll().values().forEach(this::add);
    }

    private void add(TeleportPad pad) {
        pads.put(pad.name(), pad);
        byBlock.put(pad.key(), pad);
    }

    // Null when made, else why not. The block is the one stood on
    public String create(String name, Block block) {
        String key = name.toLowerCase();
        if (pads.containsKey(key)) return "Teleport pad '" + key + "' already exists";
        TeleportPad onBlock = byBlock.get(TeleportPad.key(block.getWorld().getName(), block.getX(), block.getY(), block.getZ()));
        if (onBlock != null) return "That block is already the pad '" + onBlock.name() + "'";
        TeleportPad pad = new TeleportPad(key, block.getWorld().getName(), block.getX(), block.getY(), block.getZ());
        add(pad);
        db.async(() -> store.save(pad));
        return null;
    }

    public boolean delete(String name) {
        TeleportPad pad = pads.remove(name.toLowerCase());
        if (pad == null) return false;
        byBlock.remove(pad.key());
        db.async(() -> store.delete(pad.name()));
        return true;
    }

    public TeleportPad get(String name) {
        return pads.get(name.toLowerCase());
    }

    public Map<String, TeleportPad> getPads() {
        return new LinkedHashMap<>(pads);
    }

    // Only when the player moves onto another block
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        if (event.getFrom().getBlockX() == event.getTo().getBlockX()
                && event.getFrom().getBlockY() == event.getTo().getBlockY()
                && event.getFrom().getBlockZ() == event.getTo().getBlockZ()) return;

        Block below = event.getTo().getBlock().getRelative(BlockFace.DOWN);
        TeleportPad pad = byBlock.get(TeleportPad.key(below.getWorld().getName(), below.getX(), below.getY(), below.getZ()));
        if (pad == null) return;

        Player player = event.getPlayer();
        if (!access.grant(player.getUniqueId(), pad.access())) return;
        TextUtils.send(player, "<aqua><b>Teleport pad unlocked</b> <gray>- you can come back here from spawn");
        player.playSound(player.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 0.8f, 1.4f);
    }
}
