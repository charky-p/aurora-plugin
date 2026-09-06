package org.charky.aurora.features.combat;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.entity.Display;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.TextDisplay;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;
import org.charky.aurora.common.util.DataKeys;
import org.charky.aurora.core.Aurora;
import org.charky.aurora.core.DataRegistry;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CombatListener implements Listener {
    // Ticks the floating damage number lives for
    private static final int INDICATOR_LIFETIME = 16;

    // Player health on the custom scale. Not a real pool, just the divisor that
    // converts mob attack into vanilla's 20 points, so one hit costs
    // attack / PLAYER_MAX_HEALTH of the heart bar.
    // TODO: derive from gear and rank
    private static final double PLAYER_MAX_HEALTH = 50.0;

    private static final double BASE_DAMAGE = 10.0;
    private static final int BRACKET_SIZE = 5;
    private static final int[] BRACKET_BONUS = {1, 3, 5, 10, 15, 20, 25, 30, 35, 40};
    private static final int BRACKET_STEP = 5;

    // Vanilla resets the attack cooldown before firing the damage event, so
    // getAttackCooldown() reads 0 in here and every swing looks uncharged.
    // Time the swings ourselves instead.
    private static final Map<UUID, Long> lastAttack = new HashMap<>();
    private static final double FULL_CHARGE_MS = 625.0;

    private final Aurora plugin;
    private final DataRegistry registry;

    public CombatListener(Aurora plugin) {
        this.plugin = plugin;
        this.registry = plugin.getRegistry();
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onAttack(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof LivingEntity victim)) return;
        if (!CustomHealth.isTracked(victim)) return;

        // Our own killing blow, let vanilla apply it so getKiller() gets set
        if (CustomHealth.isFinishing(victim)) return;

        Player attacker = resolveAttacker(event);
        if (attacker == null) {
            // Mob on mob, or a projectile with no player behind it
            event.setCancelled(true);
            return;
        }

        ItemStack weapon = attacker.getInventory().getItemInMainHand();
        double raw = calculate(attacker, weapon, victim);

        // Never negative, or the mob heals past its max and cannot be killed
        double dealt = Math.max(0.0, Math.round(raw - defenceOf(victim)));

        event.setDamage(CustomHealth.NOMINAL_DAMAGE);
        CustomHealth.apply(victim, dealt, attacker, registry);

        spawnDamageIndicator(victim, dealt);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onMobAttack(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;

        LivingEntity source = resolveMobAttacker(event);
        if (source == null || !CustomHealth.isTracked(source)) return;

        double dealt = Math.max(0.0, attackOf(source) - playerDefence(player));

        // Scale into vanilla's 20 points so the heart bar reads as a percentage
        // gauge, ten hearts however large the real numbers get
        event.setDamage(dealt / PLAYER_MAX_HEALTH * 20.0);
    }

    // Terrain damage would move vanilla health without moving the nameplate, so
    // it is refused. KILL and CUSTOM stay through or the mobs survive /kill and
    // there is no way to clear them. VOID stays through so a mob that clips out
    // of the world does not hold a spawner slot forever.
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEnvironmental(EntityDamageEvent event) {
        if (event instanceof EntityDamageByEntityEvent) return;
        if (!(event.getEntity() instanceof LivingEntity victim)) return;
        if (!CustomHealth.isTracked(victim)) return;
        if (CustomHealth.isFinishing(victim)) return;

        EntityDamageEvent.DamageCause cause = event.getCause();
        if (cause == EntityDamageEvent.DamageCause.VOID
                || cause == EntityDamageEvent.DamageCause.KILL
                || cause == EntityDamageEvent.DamageCause.CUSTOM) return;

        event.setCancelled(true);
    }

    private void spawnDamageIndicator(LivingEntity victim, double amount) {
        ThreadLocalRandom random = ThreadLocalRandom.current();

        // Jitter so rapid hits do not stack into an unreadable blob
        Location location = victim.getEyeLocation().add(
                random.nextDouble(-0.4, 0.4),
                random.nextDouble(-0.2, 0.3),
                random.nextDouble(-0.4, 0.4));

        TextDisplay display = victim.getWorld().spawn(location, TextDisplay.class, text -> {
            NamedTextColor colour = amount > 0 ? NamedTextColor.RED : NamedTextColor.GRAY;

            text.text(Component.text("-" + (long) amount, colour, TextDecoration.BOLD));
            text.setBillboard(Display.Billboard.CENTER);
            text.setDefaultBackground(false);
            text.setBackgroundColor(Color.fromARGB(0, 0, 0, 0));
            text.setShadowed(true);
            text.setViewRange(0.4f);

            // Never persistent, or a stop mid-animation writes these into the
            // chunk, and they accumulate the way the husks did
            text.setPersistent(false);
        });

        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (ticks++ >= INDICATOR_LIFETIME || !display.isValid()) {
                    display.remove();
                    cancel();
                    return;
                }

                display.teleport(display.getLocation().add(0, 0.04, 0));
            }
        }.runTaskTimer(plugin, 1L, 1L);
    }

    private Player resolveAttacker(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player player) return player;
        if (event.getDamager() instanceof Projectile projectile
                && projectile.getShooter() instanceof Player shooter) return shooter;
        return null;
    }

    private LivingEntity resolveMobAttacker(EntityDamageByEntityEvent event) {
        return switch (event.getDamager()) {
            case Player ignored -> null;
            case LivingEntity living -> living;
            case Projectile projectile when projectile.getShooter() instanceof LivingEntity shooter -> shooter;
            default -> null;
        };
    }

    private double defenceOf(LivingEntity victim) {
        String mobId = victim.getPersistentDataContainer()
                .get(DataKeys.MOB_KEY, PersistentDataType.STRING);

        if (mobId == null || !registry.hasMob(mobId)) return 0.0;
        return registry.getMob(mobId).defence();
    }

    private double attackOf(LivingEntity mob) {
        String mobId = mob.getPersistentDataContainer()
                .get(DataKeys.MOB_KEY, PersistentDataType.STRING);

        if (mobId == null || !registry.hasMob(mobId)) return 0.0;
        return registry.getMob(mobId).attack();
    }

    // TODO: sum a defence value off the six armour slots once gear carries one
    private double playerDefence(Player player) {
        double total = 0;
        for (ItemStack piece : player.getInventory().getArmorContents()) {
            total += loreLevel(piece, "protection");
        }
        return total;
    }

    // TODO: move to its own class once bleed and percentage damage land
    public static double calculate(Player attacker, ItemStack weapon, LivingEntity victim) {
        int sharpness = loreLevel(weapon, "sharpness");
        double raw = BASE_DAMAGE + sharpnessBonus(sharpness);

        return raw * chargeMultiplier(attacker);
    }

    // Levels 1-5 give 1 each, 6-10 give 3 each, 11-15 give 5, and so on. Past
    // the table the increment keeps rising by BRACKET_STEP so there is no cliff.
    public static double sharpnessBonus(int sharpness) {
        if (sharpness <= 0) return 0.0;

        double total = 0.0;
        int remaining = sharpness;
        int bracket = 0;

        while (remaining > 0) {
            int levels = Math.min(remaining, BRACKET_SIZE);
            total += (double) levels * bonusForBracket(bracket);
            remaining -= levels;
            bracket++;
        }

        return total;
    }

    private static int bonusForBracket(int bracket) {
        if (bracket < BRACKET_BONUS.length) return BRACKET_BONUS[bracket];
        return BRACKET_BONUS[BRACKET_BONUS.length - 1]
                + (bracket - BRACKET_BONUS.length + 1) * BRACKET_STEP;
    }

    // 20% damage on a spammed swing rising to 100% on a timed one. Squared so
    // half charge pays 40%, not 60%.
    private static double chargeMultiplier(Player attacker) {
        long now = System.currentTimeMillis();
        Long previous = lastAttack.put(attacker.getUniqueId(), now);
        if (previous == null) return 1.0;

        double charge = Math.min(1.0, (now - previous) / FULL_CHARGE_MS);
        return 0.2 + 0.8 * charge * charge;
    }

    // TODO: temporary, lore is display text so an anvil rename zeroes it and a
    // player can name an item "Sharpness 999". Replace with a PDC read.
    private static int loreLevel(ItemStack item, String enchantId) {
        if (item == null || item.getType().isAir() || !item.hasItemMeta()) return 0;

        List<Component> lore = item.getItemMeta().lore();
        if (lore == null) return 0;

        Pattern pattern = Pattern.compile(
                "\\b" + Pattern.quote(enchantId) + "\\b\\s+([0-9]+)",
                Pattern.CASE_INSENSITIVE);

        for (Component line : lore) {
            Matcher matcher = pattern.matcher(
                    PlainTextComponentSerializer.plainText().serialize(line));
            if (matcher.find()) return Integer.parseInt(matcher.group(1));
        }

        return 0;
    }
}