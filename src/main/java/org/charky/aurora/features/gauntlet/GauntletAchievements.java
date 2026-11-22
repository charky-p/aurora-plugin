package org.charky.aurora.features.gauntlet;

import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.charky.aurora.common.util.TextUtils;

import java.util.UUID;

// Achievements (achievements.csv): long goals on a counted stat (kills, elites, bosses,
// Shard Hoarders, fast clears, the best combo, runs, the best floor, PvP and duel wins),
// each paid once the moment it is reached: Essence into the main game, Shards, and a
// cosmetic for some. Only checked while the player is online, so they are paid at once
public class GauntletAchievements {
    static final String DONE = "ach:";

    private final GauntletManager manager;

    public GauntletAchievements(GauntletManager manager) {
        this.manager = manager;
    }

    // Where a stat stands for an achievement
    public int value(GauntletProfile profile, String stat) {
        return switch (stat) {
            case "best" -> profile.best;
            case "runs" -> profile.runs;
            default -> profile.stat(stat);
        };
    }

    public boolean done(GauntletProfile profile, GauntletData.Achievement achievement) {
        return profile.stat(DONE + achievement.id()) > 0;
    }

    public int doneCount(GauntletProfile profile) {
        int done = 0;
        for (GauntletData.Achievement achievement : manager.data().achievements()) if (done(profile, achievement)) done++;
        return done;
    }

    // Pays every achievement newly reached
    public void check(UUID uuid) {
        Player player = Bukkit.getPlayer(uuid);
        if (player == null) return;
        GauntletProfile profile = manager.profile(uuid);
        for (GauntletData.Achievement achievement : manager.data().achievements()) {
            if (done(profile, achievement) || value(profile, achievement.stat()) < achievement.goal()) continue;
            manager.setStat(uuid, DONE + achievement.id(), 1);
            manager.reward(player, achievement.essence(), achievement.shards(), achievement.cosmetic());
            player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.8f, 1.1f);
            TextUtils.send(player, "<gold><b>ACHIEVEMENT</b> <white>" + achievement.name() + " <gray>" + achievement.describe()
                    + (achievement.essence() > 0 ? " <aqua>+" + achievement.essence() + " Essence" : "")
                    + (achievement.shards() > 0 ? " <aqua>+" + achievement.shards() + " Shards" : "")
                    + (achievement.cosmetic().isBlank() ? "" : " <light_purple>+ a cosmetic"));
        }
    }
}
