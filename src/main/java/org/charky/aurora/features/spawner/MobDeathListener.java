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
import org.charky.aurora.common.util.Enchants;
import org.charky.aurora.features.player.BackpackData;
import org.charky.aurora.features.player.BackpackManager;
import org.charky.aurora.features.player.Essence;
import org.charky.aurora.features.player.ScoreboardManager;

import java.util.List;

public class MobDeathListener implements Listener {
    private final SpawnerManager spawnerManager;
    private final InventoryManager inventoryManager;
    private final DataRegistry registry;
    private final BackpackManager backpackManager;
    private final ScoreboardManager scoreboard;

    public MobDeathListener(Aurora plugin) {
        this.spawnerManager = plugin.getMobManager();
        this.inventoryManager = plugin.getInventoryManager();
        this.registry = plugin.getRegistry();
        this.backpackManager = plugin.getBackpackManager();
        this.scoreboard = plugin.getScoreboardManager();
    }

    @EventHandler
    public void onMobDeath(EntityDeathEvent event) {
        LivingEntity victim = event.getEntity();

        // Clearing a player's drops deletes their whole inventory, vanilla handles them
        if (victim instanceof Player) return;

        // Clear drops for all mobs
        event.getDrops().clear();
        event.setDroppedExp(0);

        // Spawner key implies it was spawned by a custom spawner
        String spawnerName = victim.getPersistentDataContainer().get(
                DataKeys.SPAWNER_KEY,
                PersistentDataType.STRING
        );
        if (spawnerName == null) return;

        Player killer = victim.getKiller();
        if (killer == null) return;

        String mobId = victim.getPersistentDataContainer().get(DataKeys.MOB_KEY, PersistentDataType.STRING);

        if (mobId != null && registry.hasMob(mobId)) {
            scoreboard.incrementKills(killer);
            // Same roll as mining, with the sword's Luck
            Essence.roll(killer, Enchants.level(killer.getInventory().getItemInMainHand(), "luck"), scoreboard);
            scoreboard.updateBoard(killer);

            BackpackData backpack = backpackManager.get(killer.getUniqueId());
            int looting = Enchants.level(killer.getInventory().getItemInMainHand(), "looting");
            List<MenuItem> mobDrops = registry.getMobDrops(mobId, looting);
            backpack.addItems(mobDrops);
            if (!mobDrops.isEmpty()) killer.sendActionBar(mobDrops.getLast().getItem().displayName().append(Component.text(" " + backpack.getAmount(killer, mobDrops.getLast().getId()), NamedTextColor.GRAY, TextDecoration.BOLD)));
            // Transfer to backpack system
            // TODO: add resources to backpack, add other items to inventory
//            inventoryManager.addToInventory(victim.getKiller(), registry.getMobDrops(mobId));
        }
    }
}