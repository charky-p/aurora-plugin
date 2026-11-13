package org.charky.aurora.features.boss;

import net.kyori.adventure.bossbar.BossBar;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;
import org.charky.aurora.common.util.DataKeys;
import org.charky.aurora.features.combat.CustomHealth;
import org.charky.aurora.features.spawner.MobData;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;

// One living boss: its bar, who can see it, and its abilities
public class BossFight {
    // Players this close see the bar, about as far as the boss can be seen
    private static final double BAR_RANGE = 48.0;

    private final LivingEntity boss;
    private final BossAbilities abilities;
    private final BossBar bar;
    private final Set<UUID> viewers = new HashSet<>();

    public BossFight(LivingEntity boss, MobData mob, BossAbilities abilities) {
        this.boss = boss;
        this.abilities = abilities;
        this.bar = BossBar.bossBar(mob.displayName(), 1f, BossBar.Color.YELLOW, BossBar.Overlay.NOTCHED_10);
    }

    // False once the boss is dead or gone, the fight is over
    boolean tick() {
        if (!boss.isValid() || boss.isDead()) return false;

        bar.progress((float) healthRatio());
        updateViewers();
        abilities.tick(this);
        return true;
    }

    boolean lastStand() {
        return abilities.lastStand(this);
    }

    void end() {
        for (UUID uuid : viewers) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) player.hideBossBar(bar);
        }
        viewers.clear();
        abilities.end(this);
    }

    private void updateViewers() {
        List<Player> near = playersWithin(BAR_RANGE);
        Set<UUID> nearIds = new HashSet<>();
        for (Player player : near) {
            nearIds.add(player.getUniqueId());
            if (viewers.add(player.getUniqueId())) player.showBossBar(bar);
        }

        for (Iterator<UUID> it = viewers.iterator(); it.hasNext(); ) {
            UUID uuid = it.next();
            if (nearIds.contains(uuid)) continue;

            Player player = Bukkit.getPlayer(uuid);
            if (player != null) player.hideBossBar(bar);
            it.remove();
        }
    }

    public LivingEntity boss() {
        return boss;
    }

    public BossBar bar() {
        return bar;
    }

    // The spawner the boss was summoned at, its minions are spawned as its own
    public String spawnerName() {
        return boss.getPersistentDataContainer().get(DataKeys.SPAWNER_KEY, PersistentDataType.STRING);
    }

    // 1 at full health down to 0
    public double healthRatio() {
        double max = CustomHealth.max(boss);
        return max <= 0 ? 0 : Math.clamp(CustomHealth.current(boss) / max, 0.0, 1.0);
    }

    // Players in the fight, those in creative or spectator are left alone
    public List<Player> playersWithin(double range) {
        Location centre = boss.getLocation();
        List<Player> players = new ArrayList<>();
        for (Player player : boss.getWorld().getPlayers()) {
            if (player.getGameMode() == GameMode.SPECTATOR || player.getGameMode() == GameMode.CREATIVE) continue;
            if (player.getLocation().distanceSquared(centre) <= range * range) players.add(player);
        }
        return players;
    }
}
