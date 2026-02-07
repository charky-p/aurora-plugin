package org.charky.aurora.features.spawner;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.persistence.PersistentDataType;
import org.charky.aurora.common.loot.MenuItem;
import org.charky.aurora.core.Aurora;
import org.charky.aurora.core.DataRegistry;
import org.charky.aurora.common.loot.InventoryManager;
import org.charky.aurora.common.util.DataKeys;
import org.charky.aurora.features.player.BackpackData;
import org.charky.aurora.features.player.BackpackManager;

import java.util.List;

public class MobDeathListener implements Listener {
    private final SpawnerManager spawnerManager;
    private final InventoryManager inventoryManager;
    private final DataRegistry registry;
    private final BackpackManager backpackManager;

    public MobDeathListener(Aurora plugin) {
        this.spawnerManager = plugin.getMobManager();
        this.inventoryManager = plugin.getInventoryManager();
        this.registry = plugin.getRegistry();
        this.backpackManager = plugin.getBackpackManager();
    }

    @EventHandler
    public void onMobDeath(EntityDeathEvent event) {
        LivingEntity entity = event.getEntity();
        event.getDrops().clear();
        event.setDroppedExp(0);

        // Spawner key implies it was spawned by a custom spawner
        String spawnerName = entity.getPersistentDataContainer().get(
                DataKeys.SPAWNER_KEY,
                PersistentDataType.STRING
        );

        // Clear drops for all entities
        if (spawnerName == null) return;
        spawnerManager.handleMobDeath(spawnerName);

        LivingEntity victim = event.getEntity();
        if (victim.getKiller() == null) return;

        String mobId = victim.getPersistentDataContainer().get(DataKeys.MOB_KEY, PersistentDataType.STRING);

        if (mobId != null && registry.hasMob(mobId)) {
            if (victim.getKiller() == null) {
                return;
            }

            BackpackData backpack = backpackManager.get(victim.getKiller().getUniqueId());
            List<MenuItem> mobDrops = registry.getMobDrops(mobId);
            backpack.addItems(mobDrops);
            if (!mobDrops.isEmpty()) victim.getKiller().sendActionBar(Component.text(mobDrops.getLast().getId().toUpperCase() + " " + backpack.getAmount(victim.getKiller(), mobDrops.getLast().getId()), NamedTextColor.GRAY, TextDecoration.BOLD));
            // Transfer to backpack system
            // TODO: add resources to backpack, add other items to inventory
            inventoryManager.addToInventory(victim.getKiller(), registry.getMobDrops(mobId));
        }
    }
}