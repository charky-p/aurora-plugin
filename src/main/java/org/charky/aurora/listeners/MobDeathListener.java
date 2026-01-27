package org.charky.aurora.listeners;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.charky.aurora.Aurora;
import org.charky.aurora.managers.InventoryManager;
import org.charky.aurora.managers.MobManager;
import org.charky.aurora.models.MenuItem;
import org.charky.aurora.models.MobPresets;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MobDeathListener implements Listener {
    private final MobManager mobManager;
    private final InventoryManager inventoryManager;

    public MobDeathListener(MobManager mobManager, InventoryManager inventoryManager) {
        this.mobManager = mobManager;
        this.inventoryManager = inventoryManager;
    }

    @EventHandler
    public void onMobDeath(EntityDeathEvent event) {
        LivingEntity entity = event.getEntity();
        event.getDrops().clear();
        event.setDroppedExp(0);

        // Spawner key implies it was spawned by a custom spawner
        String spawnerName = entity.getPersistentDataContainer().get(
                Aurora.SPAWNER_ID_KEY,
                PersistentDataType.STRING
        );

        // Clear drops for all entities
        if (spawnerName == null) return;
        mobManager.handleMobDeath(spawnerName);

        LivingEntity victim = event.getEntity();
        if (victim.getKiller() == null) return;

        String mobId = victim.getPersistentDataContainer().get(Aurora.MOB_ID_KEY, PersistentDataType.STRING);

        if (mobId != null && MobPresets.hasMob(mobId)) {
            inventoryManager.addToInventory(victim.getKiller(), MobPresets.getDrops(mobId));
        }
    }
}