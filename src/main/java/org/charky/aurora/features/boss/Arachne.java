package org.charky.aurora.features.boss;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.charky.aurora.common.util.TextUtils;
import org.charky.aurora.core.Aurora;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

// Arachne's fight, from the legacy Skript:
//  - Rider: she wakes with a skeleton on her back that shoots from it, the way a vanilla
//    spider jockey does, and steers her. Killed, it stays dead for the rest of the fight
//  - Cobweb traps: while her rider lives she spits webs at the feet and head of everyone
//    near her. They only fill air and are gone after a few seconds
//  - Invisibility: once the rider is dead she vanishes every few seconds, faster while
//    hidden, after a hiss to warn of it
//  - Venom bite: her hits slow for a second. No poison, it would deal vanilla damage
//  - Brood: at two thirds and one third of her health she calls cave spiders
// Her damage is her row in mobs_peaks.csv like any other mob
public class Arachne implements BossAbilities {
    public static final String BOSS_ID = "arachne";
    public static final String RIDER_ID = "arachne_rider";
    public static final String BROOD_ID = "cave_spider";

    private static final double TARGET_RANGE = 48.0;

    // Webs: seconds between, how near she spits them and how long they last
    private static final int WEB_EVERY_TICKS = 120;
    private static final int WEB_JITTER_TICKS = 20;
    private static final double WEB_RANGE = 12.0;
    private static final int WEB_LASTS_TICKS = 100;

    // Invisibility: seconds between, the warning before and how long she stays hidden
    private static final int HIDE_EVERY_TICKS = 160;
    private static final int HIDE_WARN_TICKS = 20;
    private static final int HIDE_TICKS = 60;

    // Venom bite: how long a hit slows
    private static final int BITE_SLOW_TICKS = 20;

    // Brood: the health shares she calls them at, and how many each time
    private static final double[] BROOD_AT = {2.0 / 3.0, 1.0 / 3.0};
    private static final int BROOD_SIZE = 2;

    private final Aurora plugin;
    private UUID rider;
    private boolean mounted = false;
    private boolean riderDied = false;
    private int age = 0;

    private int nextWeb = WEB_EVERY_TICKS / 2;
    private final List<Web> webs = new ArrayList<>();

    private int nextHide = -1;
    private int hideStarts = -1;
    private int hideEnds = -1;

    private int broods = 0;

    private record Web(Block block, int gone) {
    }

    public Arachne(Aurora plugin) {
        this.plugin = plugin;
    }

    @Override
    public void tick(BossFight fight) {
        age += BossManager.TICKS;
        // Not in the constructor: she is still being added to the world then
        if (!mounted) {
            mounted = true;
            mountRider(fight);
        }

        if (!riderDied && !riderAlive()) {
            riderDied = true;
            nextHide = age + HIDE_EVERY_TICKS / 2;
        }
        if (riderDied) hide(fight);
        else webs(fight);
        clearWebs(false);
        brood(fight);
    }

    private void mountRider(BossFight fight) {
        String spawner = fight.spawnerName();
        if (spawner == null) return;

        LivingEntity boss = fight.boss();
        LivingEntity skeleton = plugin.getSpawnerManager().spawnMob(RIDER_ID, boss.getLocation().add(0, boss.getHeight(), 0), spawner);
        if (skeleton == null) {
            plugin.getLogger().warning("Arachne has no '" + RIDER_ID + "' mob in the registry to carry");
            return;
        }

        boss.addPassenger(skeleton);
        rider = skeleton.getUniqueId();
        fight.aim(skeleton, TARGET_RANGE);
        boss.getWorld().playSound(boss.getLocation(), Sound.ENTITY_SPIDER_AMBIENT, 1.5f, 0.5f);
    }

    private boolean riderAlive() {
        Entity skeleton = rider == null ? null : plugin.getServer().getEntity(rider);
        return skeleton != null && skeleton.isValid() && !skeleton.isDead();
    }

    private void webs(BossFight fight) {
        if (age < nextWeb) return;
        nextWeb = age + WEB_EVERY_TICKS + ThreadLocalRandom.current().nextInt(-WEB_JITTER_TICKS, WEB_JITTER_TICKS + 1);

        LivingEntity boss = fight.boss();
        List<Player> near = fight.playersWithin(WEB_RANGE);
        if (near.isEmpty()) return;
        boss.getWorld().playSound(boss.getLocation(), Sound.ENTITY_SPIDER_HURT, 1.5f, 0.6f);
        for (Player player : near) {
            Location at = player.getLocation();
            web(at.getBlock());
            web(at.getBlock().getRelative(0, 1, 0));
            player.getWorld().spawnParticle(Particle.ITEM_COBWEB, at.clone().add(0, 1, 0), 15, 0.3, 0.6, 0.3, 0.05);
            player.sendActionBar(TextUtils.parse("<gray>Arachne spat a web at you!"));
        }
    }

    // Only ever into air, so nothing built is replaced
    private void web(Block block) {
        if (!block.getType().isAir()) return;
        block.setType(Material.COBWEB, false);
        webs.add(new Web(block, age + WEB_LASTS_TICKS));
    }

    // The webs whose time is up, or all of them. One a player broke is left alone
    private void clearWebs(boolean all) {
        for (Iterator<Web> it = webs.iterator(); it.hasNext(); ) {
            Web web = it.next();
            if (!all && age < web.gone()) continue;
            if (web.block().getType() == Material.COBWEB) web.block().setType(Material.AIR, false);
            it.remove();
        }
    }

    private void hide(BossFight fight) {
        LivingEntity boss = fight.boss();
        if (hideStarts < 0 && hideEnds < 0 && age >= nextHide) {
            hideStarts = age + HIDE_WARN_TICKS;
            boss.getWorld().playSound(boss.getLocation(), Sound.ENTITY_SPIDER_AMBIENT, 2f, 0.4f);
            for (Player player : fight.playersWithin(TARGET_RANGE)) {
                player.sendActionBar(TextUtils.parse("<dark_red>Arachne is about to vanish!"));
            }
        }
        if (hideStarts >= 0 && age >= hideStarts) {
            hideStarts = -1;
            hideEnds = age + HIDE_TICKS;
            boss.setGlowing(false);
            boss.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, HIDE_TICKS, 0, false, false, false));
            boss.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, HIDE_TICKS, 1, false, false, false));
            boss.getWorld().spawnParticle(Particle.SMOKE, boss.getLocation().add(0, 0.5, 0), 30, 0.8, 0.4, 0.8, 0.02);
        }
        if (hideEnds >= 0 && age >= hideEnds) {
            hideEnds = -1;
            unhide(boss);
            nextHide = age + HIDE_EVERY_TICKS;
        }
    }

    private static void unhide(LivingEntity boss) {
        boss.removePotionEffect(PotionEffectType.INVISIBILITY);
        boss.removePotionEffect(PotionEffectType.SPEED);
        boss.setGlowing(true);
    }

    @Override
    public void onHit(BossFight fight, Player victim) {
        victim.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, BITE_SLOW_TICKS, 0, false, false, true));
    }

    private void brood(BossFight fight) {
        if (broods >= BROOD_AT.length || fight.healthRatio() > BROOD_AT[broods]) return;
        broods++;

        String spawner = fight.spawnerName();
        if (spawner == null) return;
        LivingEntity boss = fight.boss();
        for (int i = 0; i < BROOD_SIZE; i++) {
            Location at = boss.getLocation().add(ThreadLocalRandom.current().nextDouble(-2, 2), 0, ThreadLocalRandom.current().nextDouble(-2, 2));
            LivingEntity spider = plugin.getSpawnerManager().spawnMob(BROOD_ID, at, spawner);
            if (spider != null) fight.aim(spider, TARGET_RANGE);
        }
        boss.getWorld().playSound(boss.getLocation(), Sound.ENTITY_SPIDER_AMBIENT, 2f, 1.4f);
        for (Player player : fight.playersWithin(TARGET_RANGE)) {
            player.sendActionBar(TextUtils.parse("<dark_green>Arachne calls her brood!"));
        }
    }

    @Override
    public void end(BossFight fight) {
        clearWebs(true);
        if (rider == null) return;

        Entity skeleton = plugin.getServer().getEntity(rider);
        if (skeleton != null && skeleton.isValid()) {
            skeleton.getWorld().spawnParticle(Particle.POOF, skeleton.getLocation().add(0, 1, 0), 10, 0.3, 0.6, 0.3, 0.02);
            skeleton.remove();
        }
        rider = null;
    }
}
