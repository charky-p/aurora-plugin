package org.charky.aurora.listeners;

import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.persistence.PersistentDataType;
import org.charky.aurora.Aurora;
import org.charky.aurora.MobManager;
import org.charky.aurora.MobSpawner;

public class MobDeathListener implements Listener {
    private final MobManager mobManager;

    public MobDeathListener(MobManager mobManager) {
        this.mobManager = mobManager;
    }

    @EventHandler
    public void onMobDeath(EntityDeathEvent event) {
        LivingEntity entity = event.getEntity();

        // Spawner key implies it was spawned by a custom spawner
        String spawnerName = entity.getPersistentDataContainer().get(
                Aurora.SPAWNER_ID_KEY,
                PersistentDataType.STRING
        );

        if (spawnerName == null) return;
        mobManager.handleMobDeath(spawnerName);
    }
}