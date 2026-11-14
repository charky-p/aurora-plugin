package org.charky.aurora.features.player;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.charky.aurora.features.rank.Ranks;

// The tab list: a header with who is online, and each player under their rank
public class TabManager {
    private final MiniMessage mm = MiniMessage.miniMessage();
    private final PlayerManager players;

    public TabManager(PlayerManager players) {
        this.players = players;
    }

    public void updateAll() {
        String headerStr = "<gradient:#7F7FD5:#91EAE4><bold>AURORAMINES</bold></gradient>\n" +
                            "<yellow>☀ ᴏɴʟɪɴᴇ: " + toSmallCaps(String.valueOf(Bukkit.getOnlinePlayers().size()));

        String footerStr = "<white>ɪᴘ: <aqua>ᴀᴜʀᴏʀᴀᴍɪɴᴇs.ɢɢ</white>";

        Component header = mm.deserialize(headerStr);
        Component footer = mm.deserialize(footerStr);

        for (Player player : Bukkit.getOnlinePlayers()) {
            player.sendPlayerListHeaderAndFooter(header, footer);

            // MEMBER while the profile is still loading
            PlayerData data = players.get(player);
            Component rank = Ranks.get(data == null ? null : data.getRank()).component();
            player.playerListName(rank.append(Component.text(" " + player.getName(), NamedTextColor.WHITE)));
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
