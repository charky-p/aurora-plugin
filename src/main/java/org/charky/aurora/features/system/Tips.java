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

    // Progress tips and command tips take turns
    private static final List<String> TIPS = List.of(
            "Stuck? Gormak and Verdant Groves add their Fortune and levels to the same tools. Playing both makes each one faster.",
            "<white>/a<gray> opens the Aurora menu: How to Play, your backpack, collections, the shop and more.",
            "Gormak is where the sword, armour and shield get strong. Verdant Groves is where the pick and axe do. Keep both going.",
            "Everything you mine and loot goes to your backpack. Open it with <white>/bp<gray>.",
            "A block drops nothing until your Fortune reaches its density. Buy the zone's Fortune upgrades first.",
            "<white>/collections<gray> shows everything you have gathered and the Essence the next tier pays.",
            "\"Requires Looting\" on a kill means the mob drops nothing yet. Upgrade your sword's Looting.",
            "Lost? <white>/spawn<gray> takes you back to this world's spawn.",
            "Wait for a full swing: a spammed hit does a fifth of the damage. Jump as you hit for a 1.5x crit.",
            "Type <white>[item]<gray> in chat to show what you hold, or <white>[inv]<gray> to share your inventory.",
            "Hold up your shield to block hits. Deflection upgrades let it stop stronger mobs.",
            "<white>/leaderboard<gray> shows the top ten by level, blocks mined, kills, Essence and playtime.",
            "Mob gear drops are unlocked for good. Take a copy out any time from the Gear tab in <white>/collections<gray>.",
            "You can't lose your tools or gear for good. Lost or trashed something? Take it out again from the Armory, in the Gear tab of <white>/collections<gray>.",
            "<white>/customise<gray> changes your suffix, colours, trail, kill effect, armour trim and tool looks.",
            "Collections and levels pay Essence. Spend it on perks like Dash and mounts in <white>/shop<gray>.",
            "Too much junk? <white>/trash<gray> opens a bin. Tools, the backpack and keys are always kept.",
            "Max out a zone to earn its suffix, shown in <white>/customise<gray>.",
            "<white>/ranks<gray> picks which of your ranks shows by your name.",
            "In the Royal Tomb, a Tomb Key summons Gormak the Eternal. His Ancient Hearts pay for the Tomb's last upgrades.",
            "<white>/gauntlet<gray> is a tower of mob floors with its own gear. Climb as high as you can and spend the Shards you earn in the forge.",
            "The Gauntlet has a PvP zone and 1v1 duels with <white>/gauntlet duel<gray>. Your main gear stays safe, everyone fights with Gauntlet gear.",
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
