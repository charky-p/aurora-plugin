package org.charky.aurora.features.combat;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
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
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;
import org.charky.aurora.common.util.DataKeys;
import org.charky.aurora.common.util.Enchants;
import org.charky.aurora.core.Aurora;
import org.charky.aurora.core.DataRegistry;
import org.charky.aurora.features.spawner.MobData;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public class CombatListener implements Listener {
    // Ticks the floating damage number lives for
    private static final int INDICATOR_LIFETIME = 16;

    // Mob hits are dealt in real hearts, set by how many tiers the mob's attack
    // level sits above the player's protection level. Each zone spans this many
    // protection levels. Kept in step with scripts/balance/balance_sim.py
    private static final double LEVELS_PER_TIER = 4.0;
    private static final double LOG_A = 1 / Math.log(1.15);
    private static final double LOG_B = 0.15;

    private static final double BASE_DAMAGE = 10.0;
    private static final int BRACKET_SIZE = 5;
    private static final int[] BRACKET_BONUS = {1, 3, 5, 10, 15, 20, 25, 30, 35, 40};
    private static final int BRACKET_STEP = 5;

    // Bloodlust: each kill adds a stack for a few seconds, each stack is worth
    // BONUS per enchant level, so Bloodlust 2 at full stacks is +30%
    private static final int BLOODLUST_MAX_STACKS = 3;
    private static final long BLOODLUST_MS = 6000L;
    private static final double BLOODLUST_BONUS = 0.05;

    // Thorns hands back a share of the wearer's own base hit per level of average thorns
    private static final double THORNS_SHARE = 0.25;

    // Defence on a carried shield counts as this many extra protection levels each
    private static final double DEFENCE_LEVELS = 1.0;

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

        MobData mob = mobOf(source);
        if (mob == null || mob.attack() <= 0) {
            // Passive mobs, returning here would let vanilla damage through
            event.setDamage(0.0);
            return;
        }

        // Slow heavy hitters swing every attack, only one per cooldown lands
        if (onCooldown(source, mob.attackCooldown())) {
            event.setCancelled(true);
            return;
        }

        // One heart is two vanilla health points
        event.setDamage(heartsPerHit(mob.attack(), playerDefence(player)) * 2.0);

        // TODO: deflection, shield blocks still use vanilla and stop the hit fully
        reflectThorns(player, source);
    }

    @EventHandler
    public void onKill(EntityDeathEvent event) {
        LivingEntity victim = event.getEntity();
        Player killer = victim.getKiller();
        if (killer == null || !CustomHealth.isTracked(victim)) return;

        int bloodlust = Enchants.level(killer.getInventory().getItemInMainHand(), "bloodlust");
        if (bloodlust <= 0) return;

        PersistentDataContainer pdc = killer.getPersistentDataContainer();
        int stacks = Math.min(BLOODLUST_MAX_STACKS, activeBloodlustStacks(killer) + 1);
        pdc.set(DataKeys.BLOODLUST_STACKS_KEY, PersistentDataType.INTEGER, stacks);
        pdc.set(DataKeys.BLOODLUST_UNTIL_KEY, PersistentDataType.LONG, System.currentTimeMillis() + BLOODLUST_MS);
    }

    // Swing timings are per player, drop them when the player leaves
    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        lastAttack.remove(event.getPlayer().getUniqueId());
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

    private void reflectThorns(Player player, LivingEntity source) {
        double thorns = Enchants.average(player.getInventory().getArmorContents(), "thorns");
        if (thorns <= 0) return;

        int sharpness = Enchants.level(player.getInventory().getItemInMainHand(), "sharpness");
        double reflected = Math.round((BASE_DAMAGE + sharpnessBonus(sharpness)) * THORNS_SHARE * thorns);
        if (reflected <= 0) return;

        CustomHealth.apply(source, reflected, player, registry);
        spawnDamageIndicator(source, reflected);
    }

    // The last landed hit is kept on the mob itself, so nothing leaks when it dies
    private static boolean onCooldown(LivingEntity mob, double cooldownSeconds) {
        if (cooldownSeconds <= 0) return false;

        PersistentDataContainer pdc = mob.getPersistentDataContainer();
        long now = System.currentTimeMillis();
        long last = pdc.getOrDefault(DataKeys.LAST_ATTACK_KEY, PersistentDataType.LONG, 0L);
        if (now - last < cooldownSeconds * 1000) return true;

        pdc.set(DataKeys.LAST_ATTACK_KEY, PersistentDataType.LONG, now);
        return false;
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

    private MobData mobOf(LivingEntity mob) {
        String mobId = mob.getPersistentDataContainer()
                .get(DataKeys.MOB_KEY, PersistentDataType.STRING);

        if (mobId == null || !registry.hasMob(mobId)) return null;
        return registry.getMob(mobId);
    }

    private double defenceOf(LivingEntity victim) {
        MobData mob = mobOf(victim);
        return mob == null ? 0.0 : mob.defence();
    }

    // Average protection over the four slots, plus Defence from the best shield
    // carried anywhere, held or not
    private double playerDefence(Player player) {
        double protection = Enchants.average(player.getInventory().getArmorContents(), "protection");
        return protection + shieldDefence(player) * DEFENCE_LEVELS;
    }

    private static int shieldDefence(Player player) {
        int best = Enchants.level(player.getInventory().getItemInOffHand(), "defence");
        for (ItemStack item : player.getInventory().getStorageContents()) {
            if (item != null && item.getType() == Material.SHIELD) {
                best = Math.max(best, Enchants.level(item, "defence"));
            }
        }
        return best;
    }

    // TODO: move to its own class once bleed and percentage damage land
    public static double calculate(Player attacker, ItemStack weapon, LivingEntity victim) {
        int sharpness = Enchants.level(weapon, "sharpness");
        double raw = BASE_DAMAGE + sharpnessBonus(sharpness);

        int bloodlust = Enchants.level(weapon, "bloodlust");
        double frenzy = 1 + activeBloodlustStacks(attacker) * BLOODLUST_BONUS * bloodlust;

        return raw * frenzy * chargeMultiplier(attacker);
    }

    private static int activeBloodlustStacks(Player player) {
        PersistentDataContainer pdc = player.getPersistentDataContainer();
        long until = pdc.getOrDefault(DataKeys.BLOODLUST_UNTIL_KEY, PersistentDataType.LONG, 0L);
        if (System.currentTimeMillis() > until) return 0;
        return pdc.getOrDefault(DataKeys.BLOODLUST_STACKS_KEY, PersistentDataType.INTEGER, 0);
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

    // Same tier is one heart. Above it grows like a log, 2, 2.9, 3.7, 4.4, 5, so
    // big hits break through armour. Below it tapers to half a heart, never less.
    // Both sides meet at exactly 1 so there is no jump at gap 0
    public static double heartsPerHit(double attackLevel, double protectionLevel) {
        double gap = (attackLevel - protectionLevel) / LEVELS_PER_TIER;
        if (gap >= 0) return 1 + LOG_A * Math.log(1 + LOG_B * gap);
        return 0.5 + 0.5 * Math.exp(2 * gap);
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

        return chargeMultiplier(now - previous);
    }

    static double chargeMultiplier(long sinceLastSwingMs) {
        double charge = Math.min(1.0, sinceLastSwingMs / FULL_CHARGE_MS);
        return 0.2 + 0.8 * charge * charge;
    }
}
