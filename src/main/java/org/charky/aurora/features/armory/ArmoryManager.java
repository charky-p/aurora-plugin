package org.charky.aurora.features.armory;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.charky.aurora.common.util.TextUtils;
import org.charky.aurora.core.DataRegistry;
import org.charky.aurora.core.database.Database;
import org.charky.aurora.core.database.UnlockStore;
import org.charky.aurora.features.player.BackpackManager;
import org.charky.aurora.features.tool.ToolManager;

import java.util.*;
import java.util.function.Predicate;

// Gear dropped by mobs is unlocked once, then any copy can be taken from the Armory.
// Only mob drops, shop gear is consumed by upgrades so it can never be unlimited
public class ArmoryManager {
    private final DataRegistry registry;
    private Map<UUID, Set<String>> unlocks;
    private Database db;
    private UnlockStore store;
    // Who has bought the Keyring perk, nobody until it is set
    private Predicate<Player> keyring = player -> false;
    private ToolManager tools;

    public ArmoryManager(DataRegistry registry) {
        this.registry = registry;
    }

    public void load(Database db, UnlockStore store) {
        this.unlocks = store.loadAll();
        this.db = db;
        this.store = store;
    }

    public void setTools(ToolManager tools) {
        this.tools = tools;
    }

    // Null until the plugin has finished starting
    public ToolManager tools() {
        return tools;
    }

    public void setKeyring(Predicate<Player> keyring) {
        this.keyring = keyring;
    }

    public boolean hasKeyring(Player player) {
        return keyring.test(player);
    }

    // Keys are remembered for everyone, the Keyring perk is what hands them back
    public void rememberKey(UUID uuid, String itemId) {
        unlock(uuid, itemId);
    }

    public void open(Player player) {
        new ArmoryMenu(this, registry).open(player);
    }

    public boolean isUnlocked(UUID uuid, String itemId) {
        return unlocks.getOrDefault(uuid, Set.of()).contains(itemId);
    }

    public int unlockedCount(UUID uuid) {
        Set<String> owned = unlocks.getOrDefault(uuid, Set.of());
        return (int) registry.getArmory().keySet().stream().filter(owned::contains).count();
    }

    // First drop unlocks it and hands over a copy, later drops only say so
    public void handleDrop(Player player, String itemId) {
        ItemStack item = registry.getAllItems().get(itemId);
        if (item == null) return;
        Component name = item.effectiveName();

        if (!unlock(player.getUniqueId(), itemId)) {
            player.sendMessage(TextUtils.parse("<gray>Already unlocked: ").append(name));
            return;
        }

        player.sendMessage(TextUtils.parse("<gold><b>UNLOCKED</b> ").append(name)
                .append(Component.text(" - it is kept in the Armory", NamedTextColor.GRAY)));
        player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.6f, 1f);

        if (player.getInventory().firstEmpty() != -1) player.getInventory().addItem(item.clone());
        else TextUtils.warn(player, "Inventory full - take it from the Armory when you have room");
    }

    // Gives one copy, a second is pointless as gear never breaks
    public void retrieve(Player player, String itemId) {
        if (!isUnlocked(player.getUniqueId(), itemId)) return;
        if (registry.isKey(itemId) && !hasKeyring(player)) {
            TextUtils.error(player, "Buy the Keyring perk in /shop to take keys.");
            return;
        }

        if (carries(player, itemId)) {
            TextUtils.error(player, "You already have one.");
            return;
        }
        if (player.getInventory().firstEmpty() == -1) {
            TextUtils.error(player, "Your inventory is full.");
            return;
        }

        player.getInventory().addItem(registry.getAllItems().get(itemId).clone());
        player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 0.6f, 1f);
    }

    public boolean carries(Player player, String itemId) {
        for (ItemStack slot : player.getInventory().getContents()) {
            if (itemId.equals(DataRegistry.itemId(slot))) return true;
        }
        return false;
    }

    // Before the Armory, gear drops went into the backpack, those become unlocks
    public void migrateFrom(BackpackManager backpacks) {
        backpacks.forEachItem((uuid, items) -> items.entrySet().removeIf(entry -> {
            if (!registry.getArmory().containsKey(entry.getKey())) return false;
            if (entry.getValue() > 0) unlock(uuid, entry.getKey());
            return true;
        }));
    }

    // True only the first time
    private boolean unlock(UUID uuid, String itemId) {
        if (!unlocks.computeIfAbsent(uuid, k -> new HashSet<>()).add(itemId)) return false;
        db.async(() -> store.add(uuid, itemId));
        return true;
    }

    // Every piece of gear and key the player has found is forgotten
    public void reset(UUID uuid) {
        unlocks.remove(uuid);
        db.async(() -> store.deleteAll(uuid));
    }
}
