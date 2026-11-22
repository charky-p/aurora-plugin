package org.charky.aurora.features.system;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.charky.aurora.common.util.TextUtils;

import java.util.List;

// A tip in chat for everyone online every five minutes, in order, round and round. Only
// about what is released: Origin Isles, Gormak and Verdant Groves
public final class Tips {
    private static final long EVERY_TICKS = 5 * 60 * 20L;
    private static final String PREFIX = "<gradient:#7F7FD5:#91EAE4><b>TIP</b></gradient> <dark_gray>» <gray>";

    private static final List<String> TIPS = List.of(
            "Stuck? Gormak and Verdant Groves add their Fortune and levels to the same tools. Playing both makes each one faster.",
            "Gormak is where the sword, armour and shield get strong. Verdant Groves is where the pick and axe do. Keep both going.",
            "A block drops nothing until your Fortune reaches its density. Buy the zone's Fortune upgrades first.",
            "\"Requires Looting\" on a kill means the mob drops nothing yet. Upgrade your sword's Looting.",
            "Wait for a full swing: a spammed hit does a fifth of the damage. Jump as you hit for a 1.5x crit.",
            "Hold up your shield to block hits. Deflection upgrades let it stop stronger mobs.",
            "Mob gear drops are unlocked for good. Take a copy out any time from the Gear tab in <white>/collections<gray>.",
            "Collections and levels pay Essence. Spend it in <white>/shop<gray> and <white>/customise<gray>.",
            "Max out a zone to earn its suffix, shown in <white>/customise<gray>.",
            "In the Royal Tomb, a Tomb Key summons Gormak the Eternal. His Ancient Hearts pay for the Tomb's last upgrades.",
            "Have an idea? Send it with <white>/suggest<gray>, or join us on <white>/discord<gray>.");

    private int next;

    private Tips(Plugin plugin) {
        Bukkit.getScheduler().runTaskTimer(plugin, this::broadcast, EVERY_TICKS, EVERY_TICKS);
    }

    public static void start(Plugin plugin) {
        new Tips(plugin);
    }

    private void broadcast() {
        if (Bukkit.getOnlinePlayers().isEmpty()) return;
        String tip = PREFIX + TIPS.get(next);
        next = (next + 1) % TIPS.size();
        for (Player player : Bukkit.getOnlinePlayers()) player.sendMessage(TextUtils.parse(tip));
    }
}
