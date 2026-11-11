package org.charky.aurora.features.player;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import org.charky.aurora.common.util.TextUtils;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

// Draws each player's sidebar from their profile in PlayerManager
public class ScoreboardManager {
    private final Map<UUID, Scoreboard> boards = new ConcurrentHashMap<>();
    private final PlayerManager players;

    public ScoreboardManager(PlayerManager players) {
        this.players = players;
    }

    public void setupBoard(Player player) {
        Scoreboard board = Bukkit.getScoreboardManager().getNewScoreboard();
        Objective obj = board.registerNewObjective("aurora", Criteria.DUMMY, TextUtils.getServer());
        obj.setDisplaySlot(DisplaySlot.SIDEBAR);

        renderLines(board, obj, lines(player));

        player.setScoreboard(board);
        boards.put(player.getUniqueId(), board);
    }

    public void updateBoard(Player player) {
        Scoreboard board = boards.get(player.getUniqueId());
        if (board == null) return;

        Objective obj = board.getObjective("aurora");
        if (obj == null) return;

        renderLines(board, obj, lines(player));
    }

    public void removeBoard(Player player) {
        boards.remove(player.getUniqueId());
    }

    // One team per line, its prefix is the text, so lines update without flicker
    private void renderLines(Scoreboard board, Objective obj, List<Component> lines) {
        for (int i = 0; i < lines.size(); i++) {
            // Unique identifier, color codes have no display
            String entryId = "§" + Integer.toHexString(i) + "§r";

            Team team = board.getTeam("line_" + i);
            if (team == null) team = board.registerNewTeam("line_" + i);

            if (!team.hasEntry(entryId)) team.addEntry(entryId);

            team.prefix(lines.get(i));
            obj.getScore(entryId).setScore(lines.size() - i);
        }
    }

    private List<Component> lines(Player player) {
        PlayerData data = players.get(player);
        // Profile still loading
        if (data == null) return List.of();

        return List.of(
                TextUtils.parse("<gradient:#7F7FD5:#91EAE4><b>PROFILE</b></gradient>"),
                TextUtils.parse("<gray>• RANK: <white>" + data.getRank()),
                TextUtils.parse("<gray>• LEVEL: <white>" + data.getLevel()),
                Component.empty(),
                TextUtils.parse("<gradient:#7F7FD5:#91EAE4><b>STATS</b></gradient>"),
                TextUtils.parse("<gray>• KILLS: <red>" + data.getKills()),
                TextUtils.parse("<gray>• MINED: <green>" + data.getMined()),
                TextUtils.parse("<gray>• ESSENCE: <gold>" + data.getEssence()),
                Component.empty(),
                TextUtils.parse("<light_purple>🎮 <gray>EVENT: DEVELOPMENT"),
                TextUtils.parse("<aqua>ᴀᴜʀᴏʀᴀᴍɪɴᴇs.ᴍɪɴᴇʜᴜᴛ.ɢɢ")
        );
    }
}
