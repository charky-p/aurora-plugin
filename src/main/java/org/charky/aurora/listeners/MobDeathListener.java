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
import org.charky.aurora.models.MenuItem;
import org.charky.aurora.models.MobPresets;

import java.util.ArrayList;
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
            addToInventory(victim.getKiller(), MobPresets.getDrops(mobId));
        }
    }

    // TODO: maybe put in mob manager, and message for full inventory
    public void addToInventory(Player player, List<MenuItem> drops) {
        if (player == null || drops == null || drops.isEmpty()) return;

        List<ItemStack> stacks = new ArrayList<>();
        for (MenuItem menuItem : drops) {
            stacks.addAll(menuItem.convertToStacks());
        }

        for (ItemStack item : stacks) {
            if (item == null || item.getType() == Material.AIR) continue;
            player.getInventory().addItem(item);
        }
    }
}