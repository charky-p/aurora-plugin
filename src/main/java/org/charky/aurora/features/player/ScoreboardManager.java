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
import org.charky.aurora.features.rank.Ranks;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

// Draws each player's sidebar from their profile in PlayerManager. A game mode can
// show its own lines instead, the Gauntlet does in its world
public class ScoreboardManager {
    // A sidebar never has more lines than this
    private static final int MAX_LINES = 15;

    private final Map<UUID, Scoreboard> boards = new ConcurrentHashMap<>();
    private final PlayerManager players;
    // Lines to show instead of the profile, null to show the profile
    private Function<Player, List<Component>> override = player -> null;

    public ScoreboardManager(PlayerManager players) {
        this.players = players;
    }

    public void setOverride(Function<Player, List<Component>> override) {
        this.override = override;
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

    // One team per line, its prefix is the text, so lines update without flicker. Lines
    // past the end, left from a longer sidebar, are taken off
    private void renderLines(Scoreboard board, Objective obj, List<Component> lines) {
        for (int i = lines.size(); i < MAX_LINES; i++) board.resetScores("§" + Integer.toHexString(i) + "§r");
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
        List<Component> instead = override.apply(player);
        if (instead != null) {
            List<Component> lines = new ArrayList<>(instead);
            lines.addAll(footer());
            return lines;
        }
        PlayerData data = players.get(player);
        // Profile still loading
        if (data == null) return List.of();

        List<Component> lines = new ArrayList<>(List.of(
                TextUtils.parse("<gradient:#7F7FD5:#91EAE4><b>PROFILE</b></gradient>"),
                TextUtils.parse("<gray>• RANK: ").append(Ranks.get(data.getRank()).component()),
                TextUtils.parse("<gray>• LEVEL: <white>" + data.getLevel()),
                Component.empty(),
                TextUtils.parse("<gradient:#7F7FD5:#91EAE4><b>STATS</b></gradient>"),
                TextUtils.parse("<gray>• KILLS: <red>" + data.getKills()),
                TextUtils.parse("<gray>• MINED: <green>" + data.getMined()),
                TextUtils.parse("<gray>• ESSENCE: <gold>" + data.getEssence())));
        lines.addAll(footer());
        return lines;
    }

    // The last lines of every sidebar
    private static List<Component> footer() {
        return List.of(
                Component.empty(),
                TextUtils.parse("<light_purple>🎮 <gray>EVENT: ALPHA"),
                TextUtils.parse("<aqua>ᴀᴜʀᴀᴍɪɴᴇs100.ᴍɪɴᴇꜰᴏʀᴛ.ᴄᴏᴍ"));
    }
}
