package org.charky.aurora.features.combat;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.charky.aurora.common.util.DataKeys;
import org.charky.aurora.core.DataRegistry;
import org.charky.aurora.features.spawner.MobData;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

// Custom health is stored on the entity itself in its PDC, so it survives
// chunk unload and server restart with no save or load code.
public final class CustomHealth {
    // Vanilla health decoupled, this is the real health
    private static final double VANILLA_HEALTH = 1024.0;

    // Vanilla damage per hit
    static final double NOMINAL_DAMAGE = 0.5;

    private static final Set<UUID> finishing = ConcurrentHashMap.newKeySet();

    public static void initialise(LivingEntity entity, MobData data) {
        PersistentDataContainer pdc = entity.getPersistentDataContainer();
        pdc.set(DataKeys.MAX_HEALTH_KEY, PersistentDataType.DOUBLE, data.health());
        pdc.set(DataKeys.HEALTH_KEY, PersistentDataType.DOUBLE, data.health());

        AttributeInstance maxHealth = entity.getAttribute(Attribute.MAX_HEALTH);
        if (maxHealth != null) {
            maxHealth.setBaseValue(VANILLA_HEALTH);
            entity.setHealth(VANILLA_HEALTH);
        }

        entity.setCustomNameVisible(true);
        refreshName(entity, data.displayName());
    }

    public static boolean isTracked(LivingEntity entity) {
        return entity.getPersistentDataContainer()
                .has(DataKeys.HEALTH_KEY, PersistentDataType.DOUBLE);
    }

    static boolean isFinishing(LivingEntity entity) {
        return finishing.contains(entity.getUniqueId());
    }

    public static double current(LivingEntity entity) {
        return entity.getPersistentDataContainer()
                .getOrDefault(DataKeys.HEALTH_KEY, PersistentDataType.DOUBLE, 0.0);
    }

    public static double max(LivingEntity entity) {
        return entity.getPersistentDataContainer()
                .getOrDefault(DataKeys.MAX_HEALTH_KEY, PersistentDataType.DOUBLE, 1.0);
    }

    // Takes damage that already has defence subtracted, returns what is left
    public static double apply(LivingEntity entity, double amount, Player source, DataRegistry registry) {
        double remaining = Math.max(0.0, current(entity) - amount);

        entity.getPersistentDataContainer()
                .set(DataKeys.HEALTH_KEY, PersistentDataType.DOUBLE, remaining);
        refreshName(entity, baseName(entity, registry));

        if (remaining <= 0.0) finish(entity, source);
        return remaining;
    }

    public static void heal(LivingEntity entity, double amount, DataRegistry registry) {
        double healed = Math.min(max(entity), current(entity) + amount);

        entity.getPersistentDataContainer()
                .set(DataKeys.HEALTH_KEY, PersistentDataType.DOUBLE, healed);
        refreshName(entity, baseName(entity, registry));
    }

    private static void finish(LivingEntity entity, Player source) {
        UUID id = entity.getUniqueId();
        finishing.add(id);

        // Kill with damage() not setHealth(0) for MobDeathListener
        try {
            if (source == null) {
                entity.setHealth(0.0);
            } else {
                entity.setHealth(0.5);
                // Damage is scaled down to reduce heart effect
                entity.damage(8.0, source);
            }
        } finally {
            finishing.remove(id);
        }
    }

    private static Component baseName(LivingEntity entity, DataRegistry registry) {
        String mobId = entity.getPersistentDataContainer()
                .get(DataKeys.MOB_KEY, PersistentDataType.STRING);

        if (mobId != null && registry.hasMob(mobId)) return registry.getMob(mobId).displayName();
        return Component.text(entity.getType().name());
    }

    private static void refreshName(LivingEntity entity, Component base) {
        double current = current(entity);
        double max = max(entity);
        double ratio = max <= 0 ? 0 : current / max;

        NamedTextColor colour = ratio > 0.5 ? NamedTextColor.GREEN
                : ratio > 0.25 ? NamedTextColor.YELLOW
                : NamedTextColor.RED;

        entity.customName(base
                .append(Component.space())
                .append(Component.text(format(current), colour))
                .append(Component.text("/", NamedTextColor.DARK_GRAY))
                .append(Component.text(format(max), NamedTextColor.GRAY))
                .append(Component.text(" ❤", NamedTextColor.RED)));
    }

    private static String format(double value) {
        // 1 decimal point, or remove .0
        return value == Math.floor(value)
                ? String.valueOf((long) value)
                : String.format("%.1f", value);
    }
}