package org.charky.aurora.features.player;

import org.bukkit.entity.Player;
import org.charky.aurora.common.util.TextUtils;

import java.util.concurrent.ThreadLocalRandom;

// One Essence roll for both mining and mob kills, so Luck means the same on a
// pickaxe and a sword. The chance is always 0.1%. Luck is how much it pays: 1-3
// Essence, times 1 + Luck
public final class Essence {
    private static final double CHANCE = 0.001;
    private static final int MIN_AMOUNT = 1;
    private static final int MAX_AMOUNT = 3;

    private Essence() {
    }

    // What a find of base Essence pays with this much Luck
    static int amount(int base, int luck) {
        return base * (1 + Math.max(0, luck));
    }

    public static boolean roll(Player player, int luck, PlayerManager players) {
        if (Math.random() >= CHANCE) return false;

        int found = amount(ThreadLocalRandom.current().nextInt(MIN_AMOUNT, MAX_AMOUNT + 1), luck);
        players.addEssence(player, found);
        TextUtils.send(player, "<gradient:light_purple:dark_purple><b>RARE!</b></gradient> <gray>You found <gold>" + found + " Essence<gray>!");
        return true;
    }
}
