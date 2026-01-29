package org.charky.aurora.features.trader;

import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.EntityType;
import org.bukkit.plugin.java.JavaPlugin;
import org.charky.aurora.Aurora;
import org.charky.aurora.managers.TraderManager;

public class Trader {
    public static final String TRADER_KEY_NAME = "trader_id";
    public static final NamespacedKey TRADER_KEY = new NamespacedKey(JavaPlugin.getPlugin(Aurora.class), TRADER_KEY_NAME);
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
                    TRADER_KEY,
                    org.bukkit.persistence.PersistentDataType.STRING,
                    this.traderId
            );
        });
    }
}
