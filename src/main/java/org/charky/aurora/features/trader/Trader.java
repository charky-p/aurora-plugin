package org.charky.aurora.features.trader;

import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Villager;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;
import org.charky.aurora.common.util.DataKeys;

import java.util.function.Consumer;

public class Trader {
    private final String traderId;
    private final Component displayName;
    private final EntityType type;
    // What clicking the trader does: opens a menu, or a zone's upgrades
    private final Consumer<Player> opens;
    // How a villager trader looks, either can be null for vanilla's default
    private final Villager.Profession profession;
    private final Villager.Type biome;

    public Trader(String traderId, Component displayName, EntityType type, Consumer<Player> opens,
                  Villager.Profession profession, Villager.Type biome) {
        this.traderId = traderId;
        this.displayName = displayName;
        this.type = type;
        this.opens = opens;
        this.profession = profession;
        this.biome = biome;
    }

    public void open(Player player) {
        opens.accept(player);
    }

    public String getId() {
        return traderId;
    }

    // Never saved with the chunk, the traders table is what keeps them, so a trader
    // is respawned at its spot instead of being lost or duplicated
    public Entity spawn(Location location, int spotId) {
        if (location.getWorld() == null) return null;

        return location.getWorld().spawn(location, this.type.getEntityClass(), entity -> {
            entity.customName(displayName);
            entity.setCustomNameVisible(true);

            entity.setInvulnerable(true);
            entity.setPersistent(false);
            entity.setSilent(true);
            entity.setGravity(false);

            if (entity instanceof LivingEntity living) {
                living.setAI(false);
                living.setCollidable(false);
            }
            // Dressed for the job, with the master's badge so the outfit shows in full
            if (entity instanceof Villager villager) {
                if (profession != null) villager.setProfession(profession);
                if (biome != null) villager.setVillagerType(biome);
                villager.setVillagerLevel(5);
            }

            entity.getPersistentDataContainer().set(
                    DataKeys.TRADER_KEY,
                    PersistentDataType.STRING,
                    this.traderId
            );
            entity.getPersistentDataContainer().set(
                    DataKeys.TRADER_SPOT_KEY,
                    PersistentDataType.INTEGER,
                    spotId
            );
        });
    }
}
