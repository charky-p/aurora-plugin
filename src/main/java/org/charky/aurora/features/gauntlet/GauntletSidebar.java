package org.charky.aurora.features.gauntlet;

import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.charky.aurora.common.util.TextUtils;

import java.util.ArrayList;
import java.util.List;

// In the Gauntlet's world the sidebar shows its Shards, Cores and best floor, then what
// the player is doing: watching someone, a duel, the PvP zone, a run with the party's
// health, or in the lobby their runs and party. Elsewhere (null) the main game's
final class GauntletSidebar {
    private static final String HEADER = "<gradient:#FF416C:#FF4B2B><b>%s</b></gradient>";

    private final GauntletManager manager;

    GauntletSidebar(GauntletManager manager) {
        this.manager = manager;
    }

    List<Component> lines(Player player) {
        if (!manager.isHere(player)) return null;
        GauntletProfile profile = manager.profile(player);
        List<Component> lines = new ArrayList<>(List.of(
                header("GAUNTLET"),
                TextUtils.parse("<gray>• SHARDS: <aqua>" + profile.shards),
                TextUtils.parse("<gray>• CORES: <light_purple>" + profile.cores),
                TextUtils.parse("<gray>• BEST FLOOR: <white>" + profile.best)));
        GauntletSpectate spectate = manager.spectate();
        GauntletDuels duels = manager.duels();
        GauntletPvp pvp = manager.pvp();
        GauntletRuns runs = manager.runs();
        Player watched = spectate == null ? null : spectate.target(player);
        Player opponent = duels == null ? null : duels.opponent(player);
        if (watched != null) {
            lines.add(Component.empty());
            lines.add(header("SPECTATING"));
            lines.add(TextUtils.parse("<gray>• PLAYER: <white>" + watched.getName()));
            lines.add(TextUtils.parse(runs != null && runs.inRun(watched)
                    ? "<gray>• FLOOR: <white>" + runs.floorOf(watched) + " <gray>SHARDS: <aqua>" + runs.shardsOf(watched)
                    : "<gray>• IN A DUEL"));
        } else if (opponent != null) {
            lines.add(Component.empty());
            lines.add(header("DUEL"));
            lines.add(TextUtils.parse("<gray>• VS: <white>" + opponent.getName()));
            lines.add(TextUtils.parse("<gray>• THEIR HEALTH: <red>" + Math.round(opponent.getHealth()) / 2.0 + " ❤"));
        } else if (pvp != null && pvp.inZone(player)) {
            lines.add(Component.empty());
            lines.add(header("PVP"));
            lines.add(TextUtils.parse("<gray>• KILLS: <red>" + profile.stat(GauntletPvp.KILLS)
                    + " <gray>DEATHS: <white>" + profile.stat(GauntletPvp.DEATHS)));
            lines.add(TextUtils.parse("<gray>• STREAK: <gold>" + pvp.streak(player)
                    + " <gray>BEST: <white>" + profile.stat(GauntletPvp.BEST_STREAK)));
        } else if (runs != null && runs.inRun(player)) {
            run(lines, player, runs);
        } else {
            lobby(lines, player, profile);
        }
        return lines;
    }

    // The floor, what the run has earned, and the party's health
    private static void run(List<Component> lines, Player player, GauntletRuns runs) {
        lines.add(Component.empty());
        lines.add(header("THIS RUN"));
        lines.add(TextUtils.parse("<gray>• FLOOR: <white>" + runs.floorOf(player)));
        lines.add(TextUtils.parse("<gray>• EARNED: <aqua>" + runs.shardsOf(player) + " Shards"
                + (runs.coresOf(player) > 0 ? " <light_purple>" + runs.coresOf(player) + " Cores" : "")));
        List<String> team = runs.teamLines(player);
        if (team.isEmpty()) return;
        lines.add(Component.empty());
        lines.add(header("PARTY"));
        for (String line : team) lines.add(TextUtils.parse(line));
    }

    private void lobby(List<Component> lines, Player player, GauntletProfile profile) {
        lines.add(TextUtils.parse("<gray>• RUNS: <white>" + profile.runs));
        GauntletParty party = manager.party();
        if (party == null || !party.inParty(player)) return;
        List<Player> team = party.team(player);
        lines.add(TextUtils.parse("<gray>• PARTY: <aqua>" + team.size() + "/" + party.max()
                + " <gray>led by <white>" + team.getFirst().getName()));
    }

    private static Component header(String title) {
        return TextUtils.parse(HEADER.formatted(title));
    }
}
