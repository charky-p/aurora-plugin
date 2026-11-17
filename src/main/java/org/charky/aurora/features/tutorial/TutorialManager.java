package org.charky.aurora.features.tutorial;

import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.charky.aurora.common.loot.MenuItem;
import org.charky.aurora.common.util.TextUtils;
import org.charky.aurora.core.Aurora;
import org.charky.aurora.core.database.Database;
import org.charky.aurora.core.database.TutorialStore;
import org.charky.aurora.features.cosmetic.CosmeticManager;
import org.charky.aurora.features.player.PlayerManager;
import org.charky.aurora.features.player.ScoreboardManager;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

// The first-join tutorial: a new player starts by the Cobblestone Quarry and is walked
// through mining, the backpack, the trader and a first upgrade. The step shows in a
// boss bar, each one ends by itself when it is done. Finishing pays a little Essence and
// the Newcomer suffix. Players who joined before it existed never get it
public class TutorialManager implements Listener {
    public static final int ESSENCE_REWARD = 50;
    public static final String SUFFIX_REWARD = "suffix_newcomer";
    private static final String MINED = "cobblestone";

    private final Aurora plugin;
    private final Database db;
    private final TutorialStore store;
    private final PlayerManager players;
    private final ScoreboardManager scoreboard;
    private final CosmeticManager cosmetics;

    // Players still in it and their step. Progress on a step is not saved, a rejoin starts the step over
    private final Map<UUID, TutorialStep> steps;
    private final Map<UUID, Integer> progress = new HashMap<>();
    private final Map<UUID, BossBar> bars = new HashMap<>();
    private Location start;

    public TutorialManager(Aurora plugin, Database db, TutorialStore store, PlayerManager players,
                           ScoreboardManager scoreboard, CosmeticManager cosmetics) {
        this.plugin = plugin;
        this.db = db;
        this.store = store;
        this.players = players;
        this.scoreboard = scoreboard;
        this.cosmetics = cosmetics;
        this.steps = new HashMap<>();
        store.loadAll().forEach((uuid, name) -> steps.put(uuid, stepOr(name)));
        this.start = store.loadStart();
    }

    private static TutorialStep stepOr(String name) {
        TutorialStep step = TutorialStep.of(name);
        return step != null ? step : TutorialStep.values()[0];
    }

    public boolean isIn(Player player) {
        return steps.containsKey(player.getUniqueId());
    }

    public Location getStart() {
        return start;
    }

    public void setStart(Location location) {
        start = location.clone();
        db.async(() -> store.saveStart(start));
    }

    // HIGHEST, after the join spawn, so a player in the tutorial lands at the quarry instead
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (!player.hasPlayedBefore() && !isIn(player)) {
            begin(player);
            return;
        }
        if (!isIn(player)) return;

        teleportToStart(player);
        showLater(player);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        progress.remove(uuid);
        BossBar bar = bars.remove(uuid);
        if (bar != null) event.getPlayer().hideBossBar(bar);
    }

    // From the start, for a new player or one an admin sends back through it
    public void begin(Player player) {
        UUID uuid = player.getUniqueId();
        TutorialStep first = TutorialStep.values()[0];
        steps.put(uuid, first);
        progress.remove(uuid);
        db.async(() -> store.save(uuid, first.name()));

        teleportToStart(player);
        player.sendMessage(TextUtils.parse(""));
        player.sendMessage(TextUtils.parse("<gradient:#7F7FD5:#91EAE4><b>WELCOME TO AURORA</b></gradient>"));
        player.sendMessage(TextUtils.parse("<gray>Follow the bar at the top of your screen to learn the basics. "
                + "<click:run_command:'/tutorial skip'><hover:show_text:'<red>Skip the tutorial, no reward'>"
                + "<dark_gray>[<red>Skip</red>]</hover></click>"));
        player.sendMessage(TextUtils.parse(""));
        showLater(player);
    }

    // A second after joining, so the bar and message are not lost in the join
    private void showLater(Player player) {
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline()) show(player);
        }, 20L);
    }

    // Ends it without the reward
    public boolean skip(Player player) {
        if (!isIn(player)) return false;
        end(player);
        player.sendMessage(TextUtils.parse("<gray>Tutorial skipped. <white>/a</white> opens the menu, How to Play explains the rest."));
        return true;
    }

    // Each mined block, with what it dropped
    public void mined(Player player, List<MenuItem> drops) {
        if (step(player) != TutorialStep.MINE) return;
        int amount = drops.stream().filter(drop -> MINED.equals(drop.getId())).mapToInt(MenuItem::getAmount).sum();
        if (amount > 0) addProgress(player, amount);
    }

    public void openedBackpack(Player player) {
        if (step(player) == TutorialStep.BACKPACK) advance(player);
    }

    public void openedUpgrades(Player player, String zoneId) {
        if (step(player) == TutorialStep.TRADER) advance(player);
    }

    public void bought(Player player, String zoneId) {
        if (step(player) == TutorialStep.UPGRADE) advance(player);
    }

    public void shutdown() {
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            BossBar bar = bars.get(player.getUniqueId());
            if (bar != null) player.hideBossBar(bar);
        }
        bars.clear();
    }

    private TutorialStep step(Player player) {
        return steps.get(player.getUniqueId());
    }

    private void addProgress(Player player, int amount) {
        TutorialStep step = step(player);
        int done = progress.merge(player.getUniqueId(), amount, Integer::sum);
        if (done >= step.goal()) advance(player);
        else updateBar(player);
    }

    private void advance(Player player) {
        UUID uuid = player.getUniqueId();
        TutorialStep next = step(player).next();
        progress.remove(uuid);
        if (next == null) {
            finish(player);
            return;
        }
        steps.put(uuid, next);
        db.async(() -> store.save(uuid, next.name()));
        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.7f, 1.2f);
        show(player);
    }

    private void finish(Player player) {
        end(player);
        players.addEssence(player, ESSENCE_REWARD);
        scoreboard.updateBoard(player);
        cosmetics.give(player.getUniqueId(), SUFFIX_REWARD);

        player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.7f, 1f);
        player.sendMessage(TextUtils.parse(""));
        player.sendMessage(TextUtils.parse("<gradient:#7F7FD5:#91EAE4><b>TUTORIAL COMPLETE</b></gradient>"));
        player.sendMessage(TextUtils.parse("<yellow>» <gray>Keep mining and upgrading here, the Coal Seam comes next."));
        player.sendMessage(TextUtils.parse("<yellow>» <white>/a <gray>opens the menu, with How to Play, warps and more."));
        player.sendMessage(TextUtils.parse("<yellow>» <white>/spawn <gray>takes you to the main spawn."));
        player.sendMessage(TextUtils.parse("<yellow>» <gray>Wear your new suffix in <white>/customise<gray>."));
        player.sendMessage(TextUtils.parse(""));
    }

    // Out of the tutorial, rewarded or not
    private void end(Player player) {
        UUID uuid = player.getUniqueId();
        steps.remove(uuid);
        progress.remove(uuid);
        BossBar bar = bars.remove(uuid);
        if (bar != null) player.hideBossBar(bar);
        db.async(() -> store.remove(uuid));
    }

    private void teleportToStart(Player player) {
        if (start != null) player.teleport(start);
        else plugin.getLogger().warning("No tutorial start set, " + player.getName()
                + " starts at the main spawn. Set one with /aurora tutorial setstart");
    }

    // The step in chat and the bar, no title in the middle of the screen
    private void show(Player player) {
        TutorialStep step = step(player);
        if (step == null) return;
        player.sendMessage(TextUtils.parse("<aqua><b>TUTORIAL " + step.number() + "/" + TutorialStep.values().length
                + "</b> <white>" + step.title() + " <dark_gray>- <gray>" + step.hint()));
        updateBar(player);
    }

    private void updateBar(Player player) {
        TutorialStep step = step(player);
        if (step == null) return;
        int done = progress.getOrDefault(player.getUniqueId(), 0);
        Component name = TextUtils.parse("<aqua><b>Tutorial " + step.number() + "/" + TutorialStep.values().length
                + "</b> <dark_gray>» <white>" + step.title()
                + (step.goal() > 1 ? " <gray>(" + Math.min(done, step.goal()) + "/" + step.goal() + ")" : ""));
        float fill = step.goal() > 1 ? Math.min(1f, (float) done / step.goal()) : 0f;

        BossBar bar = bars.get(player.getUniqueId());
        if (bar == null) {
            bar = BossBar.bossBar(name, fill, BossBar.Color.BLUE, BossBar.Overlay.PROGRESS);
            bars.put(player.getUniqueId(), bar);
            player.showBossBar(bar);
        } else {
            bar.name(name);
            bar.progress(fill);
        }
    }
}
