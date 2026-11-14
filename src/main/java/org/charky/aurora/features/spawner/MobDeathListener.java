package org.charky.aurora.features.spawner;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.charky.aurora.common.loot.MenuItem;
import org.charky.aurora.core.Aurora;
import org.charky.aurora.core.DataRegistry;
import org.charky.aurora.common.util.DataKeys;
import org.charky.aurora.common.util.Enchants;
import org.charky.aurora.features.armory.ArmoryManager;
import org.charky.aurora.features.player.BackpackManager;
import org.charky.aurora.features.collection.CollectionManager;
import org.charky.aurora.features.player.Essence;
import org.charky.aurora.features.player.LevelManager;
import org.charky.aurora.features.player.PlayerManager;
import org.charky.aurora.features.player.ScoreboardManager;

import java.util.ArrayList;
import java.util.List;

public class MobDeathListener implements Listener {
    private static final long MIN_KILL_XP = 10;

    private final SpawnerManager spawnerManager;
    private final DataRegistry registry;
    private final BackpackManager backpackManager;
    private final ArmoryManager armoryManager;
    private final PlayerManager players;
    private final ScoreboardManager scoreboard;
    private final LevelManager levels;
    private final CollectionManager collections;

    public MobDeathListener(Aurora plugin) {
        this.spawnerManager = plugin.getSpawnerManager();
        this.registry = plugin.getRegistry();
        this.backpackManager = plugin.getBackpackManager();
        this.armoryManager = plugin.getArmoryManager();
        this.players = plugin.getPlayerManager();
        this.scoreboard = plugin.getScoreboardManager();
        this.levels = plugin.getLevelManager();
        this.collections = plugin.getCollectionManager();
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
        if (mobId == null || !registry.hasMob(mobId)) return;

        ItemStack weapon = killer.getInventory().getItemInMainHand();
        players.addKill(killer);
        // A kill is worth the mob's health in XP, far more than a block
        levels.addXp(killer, Math.max(MIN_KILL_XP, Math.round(registry.getMob(mobId).health())));
        // Same roll as mining, with the sword's Luck
        Essence.roll(killer, Enchants.level(weapon, "luck"), players);
        scoreboard.updateBoard(killer);

        // Resources stack in the backpack, gear is unlocked in the Armory
        List<MenuItem> mobDrops = new ArrayList<>();
        for (MenuItem drop : registry.getMobDrops(mobId, Enchants.level(weapon, "looting"))) {
            if (registry.isResource(drop.getId())) mobDrops.add(drop);
            else armoryManager.handleDrop(killer, drop.getId());
        }

        backpackManager.get(killer.getUniqueId()).collect(killer, mobDrops);
        collections.add(killer, mobDrops);
    }
}