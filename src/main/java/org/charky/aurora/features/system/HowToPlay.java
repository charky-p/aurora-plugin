package org.charky.aurora.features.system;

import org.bukkit.entity.Player;
import org.charky.aurora.common.util.TextUtils;

import java.util.List;

// How to Play in the Aurora menu: the game in a few lines of chat, then every
// command a player has. Staff commands are not listed
public final class HowToPlay {
    private static final List<String> LINES = List.of(
            "",
            "<gradient:#7F7FD5:#91EAE4><b>HOW TO PLAY</b></gradient>",
            "<yellow>1. <white>Mine. <gray>Break blocks in a mine, what they drop goes straight to your backpack.",
            "<yellow>2. <white>Upgrade. <gray>Click a zone's trader and spend what you mined on your tools. They are never replaced, only made better.",
            "<yellow>3. <white>Fight. <gray>Some upgrades also cost what mobs drop. Each zone has its own.",
            "<yellow>4. <white>Move on. <gray>A zone's last upgrades need the next mine, so follow the islands. Max a zone out to earn its suffix.",
            "<yellow>5. <white>Level up. <gray>Mining and kills give XP, levels and collections pay Essence, spent in <white>/shop<gray> and <white>/customise<gray>.",
            "",
            "<gradient:#7F7FD5:#91EAE4><b>COMMANDS</b></gradient>",
            "<white>/a <dark_gray>- <gray>this menu",
            "<white>/backpack<gray>, <white>/bp <dark_gray>- <gray>everything you have mined and looted",
            "<white>/shop <dark_gray>- <gray>spend Essence on perks and ranks",
            "<white>/customise <dark_gray>- <gray>suffixes, colours, trails, trims and more",
            "<white>/ranks <dark_gray>- <gray>the rank shown by your name",
            "<white>/collections <dark_gray>- <gray>what you have gathered and what the next tier pays",
            "<white>/top <dark_gray>- <gray>the ten highest levels",
            "<white>/spawn <dark_gray>- <gray>back to this world's spawn",
            "<gray>In chat, <white>[item]<gray> shows what you hold and <white>[inv]<gray> shares your inventory.",
            "");

    private HowToPlay() {
    }

    public static void send(Player player) {
        player.closeInventory();
        for (String line : LINES) player.sendMessage(TextUtils.parse(line));
    }
}
