package org.charky.aurora.features.player;

import org.bukkit.entity.Player;
import org.charky.aurora.common.util.TextUtils;

import java.util.concurrent.ThreadLocalRandom;

// One Essence roll for both mining and mob kills, so Luck means the same on a
// pickaxe and a sword. Chance = 0.1% + (0.1% * Luck), 1-3 Essence when it hits
public final class Essence {
    private static final double CHANCE = 0.001;
    private static final int MIN_AMOUNT = 1;
    private static final int MAX_AMOUNT = 3;

    private Essence() {
    }

    static double chance(int luck) {
        return CHANCE + CHANCE * luck;
    }

    public static boolean roll(Player player, int luck, ScoreboardManager scoreboard) {
        if (Math.random() >= chance(luck)) return false;

        scoreboard.addEssence(player, ThreadLocalRandom.current().nextInt(MIN_AMOUNT, MAX_AMOUNT + 1));
        TextUtils.send(player, "<gradient:light_purple:dark_purple><b>RARE!</b></gradient> <gray>You found <white>Essence<gray>!");
        return true;
    }
}
