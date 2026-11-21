package org.charky.aurora.features.player;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.charky.aurora.features.rank.Ranks;

import java.util.function.Function;

// The tab list: a header with who is online, and each player under their rank
public class TabManager {
    private final MiniMessage mm = MiniMessage.miniMessage();
    private final PlayerManager players;
    // The name as the player chose to colour it, plain white until cosmetics load
    private Function<Player, Component> names = player -> Component.text(player.getName(), NamedTextColor.WHITE);

    // The rank, or ranks, before the name. MEMBER until ranks load
    private Function<Player, Component> ranks = player -> Ranks.MEMBER.component();

    public void setNames(Function<Player, Component> names) {
        this.names = names;
    }

    public void setRanks(Function<Player, Component> ranks) {
        this.ranks = ranks;
    }

    public TabManager(PlayerManager players) {
        this.players = players;
    }

    public void updateAll() {
        String headerStr = "<gradient:#7F7FD5:#91EAE4><bold>AURORAMINES</bold></gradient>\n" +
                            "<yellow>☀ ᴏɴʟɪɴᴇ: " + toSmallCaps(String.valueOf(Bukkit.getOnlinePlayers().size()));

        String footerStr = "<white>ɪᴘ: <aqua>ᴀᴜʀᴀᴍɪɴᴇs100.ᴍɪɴᴇʜᴜᴛ.ᴄᴏᴍ</white>";

        Component header = mm.deserialize(headerStr);
        Component footer = mm.deserialize(footerStr);

        for (Player player : Bukkit.getOnlinePlayers()) {
            player.sendPlayerListHeaderAndFooter(header, footer);

            player.playerListName(ranks.apply(player).append(Component.space()).append(names.apply(player)));
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
