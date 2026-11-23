package org.charky.aurora.features.gauntlet;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;
import org.charky.aurora.common.util.Enchants;
import org.charky.aurora.common.util.TextUtils;
import org.charky.aurora.core.Aurora;
import org.charky.aurora.features.combat.BowListener;
import org.charky.aurora.features.combat.CombatFormulas;
import org.charky.aurora.features.combat.PlayerDamage;
import org.charky.aurora.features.combat.SwingTimer;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

// The Gauntlet's PvP zone holds whoever walks in: there is no walking or pearling
// out. /spawn, /gauntlet leave or the menu's Leave take them out after a countdown,
// and logging out inside counts as dying, to whoever hit them last. Hits are priced
// like vanilla's, the weapon's level against the armour's on top (GauntletPvpFormula). Kills pay Shards,
// more on a streak and for ending someone else's, heal the killer up, and are counted
// for the PvP board. A long enough streak is a bounty: its player glows in the zone
public class GauntletPvp implements Listener {
    public static final String KILLS = "pvp_kills";
    public static final String DEATHS = "pvp_deaths";
    public static final String BEST_STREAK = "pvp_best_streak";
    // Streaks announced to the world
    private static final Set<Integer> ANNOUNCED = Set.of(3, 5, 10, 15, 20);
    static final int COUNTDOWN_SECONDS = 5;
    // A hit this recent makes its player the killer of someone who logs out
    private static final long LAST_HIT_MS = 15_000;
    private static final Set<String> WAY_OUT = Set.of("/spawn", "/gauntlet leave", "/gauntlet");

    private final Aurora plugin;
    private final GauntletManager manager;
    private final Map<UUID, BukkitTask> leaving = new HashMap<>();
    private final Map<UUID, UUID> lastHitBy = new HashMap<>();
    private final Map<UUID, Long> lastHitAt = new HashMap<>();
    // Just come into the zone, so landing in it does no fall damage, until when
    private final Map<UUID, Long> droppingIn = new HashMap<>();
    private static final long DROP_IN_MS = 10_000;
    // What a death in the zone drops carries this
    private static final NamespacedKey LOOT_KEY = new NamespacedKey("aurora", "gauntlet_loot");
    // An item on the ground goes once it has lain this long (vanilla's five minutes)
    private static final int ITEM_LIFETIME_TICKS = 6000;
    // Strength adds this much a level to a hit, as it does in runs
    private static final double STRENGTH_PER_LEVEL = 0.3;
    // Swings are timed for their charge, as against mobs
    private final SwingTimer swings = new SwingTimer();
    // Each player's kills since they last died
    private final Map<UUID, Integer> streaks = new HashMap<>();
    // Glowing for a bounty right now
    private final Set<UUID> wanted = new HashSet<>();

    public GauntletPvp(Aurora plugin, GauntletManager manager) {
        this.plugin = plugin;
        this.manager = manager;
    }

    public boolean inZone(Player player) {
        return manager.isHere(player) && !manager.inRun(player) && manager.arena().inPvp(player.getLocation());
    }

    // Out of the zone after the countdown: to the lobby, or out of the Gauntlet with
    // leaveWorld. Outside the zone it happens at once. Null if it started
    public String leave(Player player, boolean leaveWorld) {
        if (!inZone(player)) {
            go(player, leaveWorld);
            return null;
        }
        if (leaving.containsKey(player.getUniqueId())) return "You are already on your way out";
        TextUtils.send(player, "<gray>Leaving the PvP zone in <white>" + COUNTDOWN_SECONDS + " seconds<gray>...");
        int[] left = {COUNTDOWN_SECONDS};
        BukkitTask task = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            if (!player.isOnline() || !inZone(player)) {
                stop(player);
                return;
            }
            if (left[0] <= 0) {
                stop(player);
                go(player, leaveWorld);
                return;
            }
            player.sendActionBar(TextUtils.parse("<red>Leaving the PvP zone in <white>" + left[0] + "s"));
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_HAT, 0.6f, 1.4f);
            left[0]--;
        }, 0L, 20L);
        leaving.put(player.getUniqueId(), task);
        return null;
    }

    private void stop(Player player) {
        BukkitTask task = leaving.remove(player.getUniqueId());
        if (task != null) task.cancel();
    }

    private void go(Player player, boolean leaveWorld) {
        if (leaveWorld) {
            manager.leave(player);
            return;
        }
        Location lobby = manager.arena().lobby();
        if (lobby != null) player.teleport(lobby);
    }

    // Walking out is refused: back to where they stood
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        Location from = event.getFrom(), to = event.getTo();
        if (from.getBlockX() == to.getBlockX() && from.getBlockY() == to.getBlockY() && from.getBlockZ() == to.getBlockZ()) return;
        Player player = event.getPlayer();
        if (!inZone(player)) {
            // Coming in, a jump or a drop from the lobby lands without fall damage
            if (manager.isHere(player) && !manager.inRun(player) && manager.arena().inPvp(to)) {
                droppingIn.put(player.getUniqueId(), System.currentTimeMillis() + DROP_IN_MS);
            }
            return;
        }
        if (manager.arena().inPvp(to)) return;
        event.setTo(from.clone().setDirection(to.getDirection()));
        player.sendActionBar(TextUtils.parse("<red>You can't walk out of the PvP zone. <white>/spawn</white> leaves in "
                + COUNTDOWN_SECONDS + " seconds"));
    }

    // Ender pearls and chorus fruit can't carry anyone out either
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPearl(PlayerTeleportEvent event) {
        PlayerTeleportEvent.TeleportCause cause = event.getCause();
        if (cause != PlayerTeleportEvent.TeleportCause.ENDER_PEARL && cause != PlayerTeleportEvent.TeleportCause.CONSUMABLE_EFFECT
                && cause != PlayerTeleportEvent.TeleportCause.CHORUS_FRUIT) return;
        if (inZone(event.getPlayer()) && !manager.arena().inPvp(event.getTo())) {
            event.setCancelled(true);
            event.getPlayer().sendActionBar(TextUtils.parse("<red>Nothing carries you out of the PvP zone"));
        }
    }

    // /spawn anywhere in the Gauntlet's world goes to its lobby: at once from the lobby,
    // ending the run from a run, after the countdown from the PvP zone. In the zone
    // /gauntlet leave counts down too, and /gauntlet's menu waits
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        if (!manager.isHere(player)) return;
        String command = event.getMessage().toLowerCase().trim().replaceAll("\\s+", " ");
        boolean spawn = command.equals("/spawn") || command.startsWith("/spawn ");
        if (spawn && !inZone(player)) {
            event.setCancelled(true);
            if (manager.inRun(player)) manager.endRun(player);
            else go(player, false);
            return;
        }
        if (!inZone(player)) return;
        if (!WAY_OUT.contains(command) && !spawn) return;
        event.setCancelled(true);
        if (command.equals("/gauntlet")) {
            TextUtils.error(player, "Not in the PvP zone. /spawn leaves it in " + COUNTDOWN_SECONDS + " seconds");
            return;
        }
        String problem = leave(player, command.equals("/gauntlet leave"));
        if (problem != null) TextUtils.error(player, problem);
    }

    // Every hit between players in the zone or a duel, once GauntletWorld has let it
    // through, on vanilla's scale whatever the gear (GauntletPvpFormula): the weapon's
    // share and level, how good the hit was, against the armour's level. A raised shield
    // still blocks it, as in vanilla
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPvpHit(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim) || !manager.isHere(victim)) return;
        boolean dueling = manager.inDuel(victim);
        if (!dueling && !inZone(victim)) return;
        BowListener.Shot shot = BowListener.shotOf(event.getDamager());
        Player attacker = shot != null && event.getDamager() instanceof Projectile projectile && projectile.getShooter() instanceof Player shooter
                ? shooter : event.getDamager() instanceof Player hitter ? hitter : null;
        if (attacker == null || attacker.equals(victim)) return;
        if (dueling && (manager.duels() == null || !manager.duels().areDueling(victim, attacker))) return;

        GauntletData data = manager.data();
        double share, level, quality;
        if (shot != null) {
            share = shot.share();
            level = shot.power() / GauntletPvpFormula.STAT_PER_LEVEL;
            quality = shot.draw();
        } else {
            ItemStack weapon = attacker.getInventory().getItemInMainHand();
            GauntletData.Gear gear = data.gear().get(GauntletGear.gearId(weapon));
            share = gear == null ? GauntletPvpFormula.BARE_SHARE : gear.damage() / 100.0;
            level = Enchants.level(weapon, "sharpness") / GauntletPvpFormula.STAT_PER_LEVEL;
            // A sweep is a splash of the swing that already landed, not a swing of its own
            quality = event.getCause() == EntityDamageEvent.DamageCause.ENTITY_SWEEP_ATTACK ? GauntletPvpFormula.SWEEP_SHARE
                    : swings.swing(attacker) * (event.isCritical() ? CombatFormulas.CRIT_MULTIPLIER : 1)
                    * (weapon.getType() == Material.MACE ? Math.min(data.setting("pvp_smash_max", 2), PlayerDamage.smash(attacker.getFallDistance())) : 1);
        }
        PotionEffect strength = attacker.getPotionEffect(PotionEffectType.STRENGTH);
        if (strength != null) quality *= 1 + STRENGTH_PER_LEVEL * (strength.getAmplifier() + 1);
        double armour = Enchants.average(victim.getInventory().getArmorContents(), "protection") / GauntletPvpFormula.STAT_PER_LEVEL;
        event.setDamage(GauntletPvpFormula.damage(data.setting("pvp_base_damage", 2), share, quality, level, armour,
                data.setting("pvp_level_effect", 3) / 100));
    }

    // What someone dying in the zone had on them drops for anyone in it to pick up:
    // copies, as they come back with their own setup. Loot is only borrowed, gone when
    // whoever took it dies or walks out (their own setup put back then). It lies
    // pvp_loot_seconds
    private void dropLoot(Player victim) {
        Location at = victim.getLocation();
        int lies = (int) Math.round(manager.data().setting("pvp_loot_seconds", 60) * 20);
        for (ItemStack item : victim.getInventory().getContents()) {
            if (item == null || item.getType().isAir() || GauntletSetup.isTool(item)) continue;
            ItemStack loot = item.clone();
            loot.editMeta(meta -> meta.getPersistentDataContainer().set(LOOT_KEY, PersistentDataType.BYTE, (byte) 1));
            at.getWorld().dropItemNaturally(at, loot, drop -> drop.setTicksLived(Math.max(1, ITEM_LIFETIME_TICKS - lies)));
        }
    }

    static boolean isLoot(ItemStack item) {
        return item != null && item.hasItemMeta() && item.getItemMeta().getPersistentDataContainer().has(LOOT_KEY, PersistentDataType.BYTE);
    }

    // Loot is for the zone: nobody outside it picks it up, nor any mob
    @EventHandler(ignoreCancelled = true)
    public void onPickup(EntityPickupItemEvent event) {
        ItemStack item = event.getItem().getItemStack();
        if (!isLoot(item)) return;
        if (!(event.getEntity() instanceof Player player) || !inZone(player)) event.setCancelled(true);
    }

    // A death in the zone: the killer is paid, more on a streak and for ending a streak
    // of 3 or more, and both are counted. The anti-farming rule (KillStreaks) already
    // stops the same pair trading kills. What the dead had on them drops as loot
    @EventHandler(priority = EventPriority.MONITOR)
    public void onDeath(PlayerDeathEvent event) {
        Player victim = event.getPlayer();
        if (!manager.isHere(victim) || manager.inRun(victim) || !manager.arena().inPvp(victim.getLocation())) return;
        dropLoot(victim);
        Integer ended = streaks.remove(victim.getUniqueId());
        manager.addStat(victim.getUniqueId(), DEATHS, 1);
        Player killer = victim.getKiller();
        if (killer == null || killer.equals(victim) || !manager.isHere(killer)) return;

        GauntletData data = manager.data();
        int streak = streaks.merge(killer.getUniqueId(), 1, Integer::sum);
        manager.addStat(killer.getUniqueId(), KILLS, 1);
        if (streak > manager.profile(killer).stat(BEST_STREAK)) manager.setStat(killer.getUniqueId(), BEST_STREAK, streak);
        int shutdown = ended == null ? 0 : shutdownPay(ended);
        int pay = (int) Math.round(data.setting("pvp_kill_shards", 5)
                + data.setting("pvp_streak_shards", 2) * Math.min(streak - 1, 5)) + shutdown;
        manager.giveShards(killer, pay);
        // A kill heals the killer up, ready for the next fight
        double share = data.setting("pvp_kill_heal", 100) / 100;
        GauntletPlayers.heal(killer, GauntletPlayers.maxHealth(killer) * share);
        killer.setFoodLevel(20);
        killer.setSaturation(10);
        killer.getWorld().spawnParticle(Particle.HEART, killer.getLocation().add(0, 2, 0), 5, 0.4, 0.2, 0.4, 0);
        manager.challenges().add(killer, "pvp_kills", 1);
        manager.weekly().pvpWin(killer.getUniqueId());
        killer.playSound(killer.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.6f, 1.6f);
        TextUtils.send(killer, "<red>You killed " + victim.getName() + " <aqua>+" + pay + " Shards"
                + (streak > 1 ? " <gray>(streak <white>" + streak + "<gray>)" : ""));

        if (shutdown > 0) manager.announce("<gold>" + killer.getName() + " <gray>ended <gold>" + victim.getName()
                + "<gray>'s <white>" + ended + "</white> kill streak" + (ended >= bountyStreak() ? " <gold>and claims the bounty" : "")
                + " <aqua>+" + shutdown + " Shards");
        if (streak == bountyStreak()) {
            manager.announce("<gold><b>BOUNTY</b> <red>" + killer.getName() + " <gray>is on a <white>" + streak
                    + "</white> kill streak and glows in the PvP zone: ending it pays <aqua>" + shutdownPay(streak) + "+ Shards");
        } else if (ANNOUNCED.contains(streak)) {
            manager.announce("<red><b>" + killer.getName() + "</b> <gray>is on a <white>" + streak + "</white> kill streak!");
        }
    }

    // What ending a streak this long pays, nothing below 3 kills
    private int shutdownPay(int streak) {
        return streak >= 3 ? (int) Math.round(streak * manager.data().setting("pvp_shutdown_shards", 3)) : 0;
    }

    private int bountyStreak() {
        return Math.max(2, (int) manager.data().setting("bounty_streak", 5));
    }

    // Every second: whoever is on a bounty streak glows while in the zone, and stops when
    // they leave it or their streak ends
    void markBounties() {
        int bounty = bountyStreak();
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            boolean now = manager.isHere(player) && inZone(player) && streak(player) >= bounty;
            if (now && wanted.add(player.getUniqueId())) player.setGlowing(true);
            else if (!now && wanted.remove(player.getUniqueId())) player.setGlowing(false);
        }
    }

    // A streak ends with the player leaving the server, after their logout death is counted
    @EventHandler(priority = EventPriority.MONITOR)
    public void onLeaveServer(PlayerQuitEvent event) {
        streaks.remove(event.getPlayer().getUniqueId());
        if (wanted.remove(event.getPlayer().getUniqueId())) event.getPlayer().setGlowing(false);
    }

    // A player's streak, 0 outside a streak
    public int streak(Player player) {
        return streaks.getOrDefault(player.getUniqueId(), 0);
    }

    // The first landing after coming into the zone is free
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onFall(EntityDamageEvent event) {
        if (event.getCause() != EntityDamageEvent.DamageCause.FALL || !(event.getEntity() instanceof Player player)) return;
        Long until = droppingIn.remove(player.getUniqueId());
        if (until != null && System.currentTimeMillis() < until && inZone(player)) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onHit(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim) || !inZone(victim)) return;
        Player attacker = event.getDamager() instanceof Player p ? p
                : event.getDamager() instanceof Projectile projectile && projectile.getShooter() instanceof Player shooter ? shooter : null;
        if (attacker == null || attacker.equals(victim)) return;
        lastHitBy.put(victim.getUniqueId(), attacker.getUniqueId());
        lastHitAt.put(victim.getUniqueId(), System.currentTimeMillis());
    }

    // Logging out inside dies, the kill going to whoever hit them in the last 15 seconds.
    // They come back in the lobby with their setup, like any death here
    @EventHandler(priority = EventPriority.LOWEST)
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        stop(player);
        swings.forget(player);
        droppingIn.remove(uuid);
        UUID by = lastHitBy.remove(uuid);
        Long at = lastHitAt.remove(uuid);
        if (!inZone(player)) return;
        Player killer = by != null && at != null && System.currentTimeMillis() - at < LAST_HIT_MS ? plugin.getServer().getPlayer(by) : null;
        for (Player other : player.getWorld().getPlayers()) {
            if (!other.equals(player)) TextUtils.send(other, "<red>" + player.getName() + " logged out in the PvP zone and died");
        }
        if (killer != null) player.damage(10_000, killer);
        // No killer to credit, or the hit was refused (the killer stepped out), or a totem saved them
        if (!player.isDead()) player.setHealth(0);
    }
}
