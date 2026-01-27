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
import org.charky.aurora.managers.MobManager;
import org.charky.aurora.models.MobPresets;

import java.util.List;
import java.util.Map;

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

        LivingEntity victim = event.getEntity();
        event.getDrops().clear();
        if (victim.getKiller() == null) return;

        String mobId = victim.getPersistentDataContainer().get(Aurora.MOB_ID_KEY, PersistentDataType.STRING);

        if (mobId != null && MobPresets.hasMob(mobId)) {
            event.getDrops().addAll(MobPresets.getDrops(mobId));
        }

        addToInventory(victim.getKiller(), event.getDrops());
        event.getDrops().clear();
        event.setDroppedExp(0);
    }

    // TODO: maybe put in mob manager, and message for full inventory
    public void addToInventory(Player player, List<ItemStack> drops) {
        if (player == null || drops == null || drops.isEmpty()) return;

        for (ItemStack item : drops) {
            if (item == null || item.getType() == Material.AIR) continue;
            player.getInventory().addItem(item);
        }
    }
}