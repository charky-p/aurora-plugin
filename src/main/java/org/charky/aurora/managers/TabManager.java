package org.charky.aurora.managers;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.charky.aurora.Aurora;

public class TabManager {
    private final Aurora plugin;
    private final MiniMessage mm = MiniMessage.miniMessage();

    public TabManager(Aurora plugin) {
        this.plugin = plugin;
    }

    public void updateAll() {
        String headerStr = "<gradient:#7F7FD5:#91EAE4><bold>AURORAMINES</bold></gradient>\n" +
                            "<white>ᴏɴʟɪɴᴇ: <aqua>" + toSmallCaps(String.valueOf(Bukkit.getOnlinePlayers().size())) + "</white>";

        String footerStr = "<white>ɪᴘ: <aqua>ᴀᴜʀᴏʀᴀᴍɪɴᴇs.ɢɢ</white>";

        Component header = mm.deserialize(headerStr);
        Component footer = mm.deserialize(footerStr);

        for (Player player : Bukkit.getOnlinePlayers()) {
            player.sendPlayerListHeaderAndFooter(header, footer);
            player.playerListName(mm.deserialize("<gray>[MEMBER] <white>" + player.getName()));
        }
    }

    private String toSmallCaps(String input) {
        return input.replace('0', '₀')
                    .replace('1', '₁')
                    .replace('2', '₂')
                    .replace('3', '₃')
                    .replace('4', '₄')
                    .replace('5', '₅')
                    .replace('6', '₆')
                    .replace('7', '₇')
                    .replace('8', '₈')
                    .replace('9', '₉');
    }
}
