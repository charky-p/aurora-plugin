package org.charky.aurora.features.trader;

import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.EntityType;
import org.charky.aurora.common.util.DataKeys;

public class Trader {
    private final String traderId;
    private final Component displayName;
    private final EntityType type;
    private final TraderMenu menu;

    public Trader(NamespacedKey traderKey, String traderId, Component displayName, EntityType type, TraderMenu menu) {
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

    public void spawn(Location location) {
        if (location.getWorld() == null) return;

        location.getWorld().spawn(location, this.type.getEntityClass(), entity -> {
            entity.customName(displayName);
            entity.setCustomNameVisible(true);

            entity.setInvulnerable(true);
            entity.setPersistent(true);
            entity.setSilent(true);
            entity.setGravity(false);

            if (entity instanceof org.bukkit.entity.LivingEntity living) {
                living.setAI(false);
                living.setCollidable(false);
            }

            entity.getPersistentDataContainer().set(
                    DataKeys.TRADER_KEY,
                    org.bukkit.persistence.PersistentDataType.STRING,
                    this.traderId
            );
        });
    }
}
