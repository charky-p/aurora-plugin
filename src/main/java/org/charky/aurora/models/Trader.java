package org.charky.aurora.models;

import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.charky.aurora.managers.TraderManager;

public class Trader {
    private final TraderManager manager;
    private final String traderId;
    private final Component displayName;
    private final EntityType type;
    private final TraderMenu menu;

    public Trader(TraderManager manager, String traderId, Component displayName, EntityType type, TraderMenu menu) {
        this.manager = manager;
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
                    manager.getTraderKey(),
                    org.bukkit.persistence.PersistentDataType.STRING,
                    this.traderId
            );
        });
    }
}
