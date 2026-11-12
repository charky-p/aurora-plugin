package org.charky.aurora.features.trader;

import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.persistence.PersistentDataType;
import org.charky.aurora.common.util.DataKeys;

public class Trader {
    private final String traderId;
    private final Component displayName;
    private final EntityType type;
    private final TraderMenu menu;

    public Trader(String traderId, Component displayName, EntityType type, TraderMenu menu) {
        this.traderId = traderId;
        this.displayName = displayName;
        this.type = type;
        this.menu = menu;
    }

    public String getMenuId() {
        return this.menu.getId();
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
