package org.charky.aurora.features.quest;

import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;
import org.charky.aurora.common.util.Swaps;
import org.charky.aurora.common.util.TextUtils;
import org.charky.aurora.core.Aurora;
import org.charky.aurora.core.database.Database;
import org.charky.aurora.core.database.QuestStore;
import org.charky.aurora.features.cosmetic.CosmeticManager;
import org.charky.aurora.features.loadout.LoadoutData;
import org.charky.aurora.features.player.PlayerManager;
import org.charky.aurora.features.player.ScoreboardManager;
import org.charky.aurora.features.tool.UpgradeShop;
import org.charky.aurora.features.tool.UpgradeZone;
import org.charky.aurora.features.trader.TraderManager;
import org.charky.aurora.features.trader.TraderSpot;
import org.charky.aurora.features.tutorial.TutorialManager;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

// The Origin Isles questline, picked up where the tutorial ends: for each zone in turn,
// buy every upgrade it sells, then follow a particle trail to the next zone's trader.
// After the Gold Reef the trail leads to the Dunesworld Gate. Each maxed zone pays a
// little Essence, reaching the gate pays more and the Islander suffix. The step shows
// in a boss bar, like the tutorial's. Players who were past the tutorial before this
// existed join it at the first zone they have not maxed, or never if they maxed them all
public class QuestManager implements Listener, LoadoutData {
    public static final String REGION = "origin_isles";
    public static final int ZONE_ESSENCE = 25;
    public static final int FINAL_ESSENCE = 100;
    public static final String SUFFIX_REWARD = "suffix_islander";

    // A step is a zone id (buy everything there), "go:" and a zone id or GATE (walk to
    // that trader), or DONE
    private static final String GO = "go:";
    private static final String GATE = "gate";
    private static final String DONE = "done";
    private static final String GATE_TRADER = "gate_trader";
    private static final String GATE_NAME = "the Dunesworld Gate";

    // Close enough to a trader to count as there
    private static final double ARRIVE = 6;
    // How far ahead of the player the trail reaches
    private static final double TRAIL_LENGTH = 14;
    private static final Particle.DustOptions TRAIL = new Particle.DustOptions(Color.fromRGB(0x55FFFF), 1.3f);
    private static final long TICK = 10L;

    private final Aurora plugin;
    private final Database db;
    private final QuestStore store;
    private final PlayerManager players;
    private final ScoreboardManager scoreboard;
    private final CosmeticManager cosmetics;
    private final UpgradeShop shop;
    private final TraderManager traders;
    private final TutorialManager tutorial;

    private final Map<UUID, String> steps = new HashMap<>();
    private final Map<UUID, BossBar> bars = new HashMap<>();
    // How far the trader was when a walk began, so the bar fills on the way
    private final Map<UUID, Double> walkFrom = new HashMap<>();
    private BukkitTask task;
    private int ticks;

    public QuestManager(Aurora plugin, Database db, QuestStore store, PlayerManager players, ScoreboardManager scoreboard,
                        CosmeticManager cosmetics, UpgradeShop shop, TraderManager traders, TutorialManager tutorial) {
        this.plugin = plugin;
        this.db = db;
        this.store = store;
        this.players = players;
        this.scoreboard = scoreboard;
        this.cosmetics = cosmetics;
        this.shop = shop;
        this.traders = traders;
        this.tutorial = tutorial;
        steps.putAll(store.loadAll());
    }

    public void start() {
        task = plugin.getServer().getScheduler().runTaskTimer(plugin, this::tick, TICK, TICK);
    }

    public void shutdown() {
        if (task != null) task.cancel();
        for (Player player : plugin.getServer().getOnlinePlayers()) hideBar(player);
        bars.clear();
    }

    // Once the tutorial is over, finished or skipped. Never again once done, a tutorial
    // replayed through /aurora tutorial start does not pay for it twice
    public void begin(Player player) {
        if (DONE.equals(step(player))) return;
        String first = firstUnmaxed(player);
        set(player, first != null ? first : GO + GATE);
        afterStep(player);
    }

    // For /aurora reset: out of it, to start again once the tutorial is done
    public void reset(Player player) {
        UUID uuid = player.getUniqueId();
        steps.remove(uuid);
        walkFrom.remove(uuid);
        hideBar(player);
        db.async(() -> store.remove(uuid));
    }

    // MONITOR, after the tutorial has had its say on a first join. Later still, once
    // the player's tools are loaded
    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        showLater(event.getPlayer(), 40L);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        walkFrom.remove(event.getPlayer().getUniqueId());
        hideBar(event.getPlayer());
    }

    private void showLater(Player player, long delay) {
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline() && !joinIfNew(player)) show(player);
        }, delay);
    }

    // A player with no step who is not in the tutorial was past it before the
    // questline existed: in at the first zone not maxed, or done without the reward.
    // True when it started them, and so has shown the step already
    private boolean joinIfNew(Player player) {
        if (steps.containsKey(player.getUniqueId()) || tutorial.isIn(player)) return false;
        String first = firstUnmaxed(player);
        set(player, first != null ? first : DONE);
        if (first == null) return false;
        afterStep(player);
        return true;
    }

    // Each upgrade bought, from the shop
    public void bought(Player player, String zoneId) {
        String step = step(player);
        if (step == null || !step.equals(zoneId)) return;
        if (shop.maxed(player, zoneId)) zoneDone(player, zoneId);
        else updateBar(player);
    }

    // Opening a zone's upgrades counts as reaching its trader
    public void openedUpgrades(Player player, String zoneId) {
        if ((GO + zoneId).equals(step(player))) arrived(player);
    }

    private void tick() {
        ticks++;
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            String step = step(player);
            if (step == null || !step.startsWith(GO)) continue;
            Location target = target(player, step.substring(GO.length()));
            if (target == null) continue;
            double distance = player.getLocation().distance(target);
            if (distance <= ARRIVE) {
                arrived(player);
                continue;
            }
            walkFrom.putIfAbsent(player.getUniqueId(), distance);
            if (ticks % 2 == 0) trail(player, target, distance);
            updateBar(player);
        }
    }

    // Dots from just ahead of the player towards the trader, seen only by them
    private void trail(Player player, Location target, double distance) {
        Location from = player.getLocation().add(0, 0.5, 0);
        Vector direction = target.toVector().add(new Vector(0, 1, 0)).subtract(from.toVector()).normalize();
        for (double along = 1.5; along <= Math.min(distance, TRAIL_LENGTH); along += 1) {
            Location dot = from.clone().add(direction.clone().multiply(along));
            player.spawnParticle(Particle.DUST, dot, 1, 0, 0, 0, 0, TRAIL);
        }
    }

    private void arrived(Player player) {
        String step = step(player);
        if (step == null || !step.startsWith(GO)) return;
        String zoneId = step.substring(GO.length());
        walkFrom.remove(player.getUniqueId());
        if (zoneId.equals(GATE)) {
            finish(player);
            return;
        }
        set(player, zoneId);
        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.7f, 1.2f);
        afterStep(player);
    }

    private void zoneDone(Player player, String zoneId) {
        List<String> zones = zones();
        int index = zones.indexOf(zoneId);
        String next = index >= 0 && index + 1 < zones.size() ? zones.get(index + 1) : GATE;
        set(player, GO + next);

        players.addEssence(player, ZONE_ESSENCE);
        scoreboard.updateBoard(player);
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.6f, 1.4f);
        player.sendMessage(TextUtils.parse("<gold><b>QUEST</b> <white>" + zoneName(zoneId) + " done! <aqua>+"
                + ZONE_ESSENCE + " Essence"));
        show(player);
    }

    private void finish(Player player) {
        set(player, DONE);
        hideBar(player);
        players.addEssence(player, FINAL_ESSENCE);
        scoreboard.updateBoard(player);
        cosmetics.give(player.getUniqueId(), SUFFIX_REWARD);

        player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.7f, 1f);
        player.sendMessage(TextUtils.parse(""));
        player.sendMessage(TextUtils.parse("<gradient:#F7971E:#FFD200><b>ORIGIN ISLES COMPLETE</b></gradient> <aqua>+"
                + FINAL_ESSENCE + " Essence"));
        player.sendMessage(TextUtils.parse("<yellow>» <gray>This trader sells the <white>Gormak Key<gray>, it opens the portal to Gormak."));
        player.sendMessage(TextUtils.parse("<yellow>» <gray>Wear your new suffix in <white>/customise<gray>."));
        player.sendMessage(TextUtils.parse(""));
    }

    // A step that is already done moves straight on: a zone maxed before it came up
    private void afterStep(Player player) {
        String step = step(player);
        if (step != null && !step.startsWith(GO) && !step.equals(DONE) && shop.maxed(player, step)) {
            zoneDone(player, step);
            return;
        }
        show(player);
    }

    private void set(Player player, String step) {
        UUID uuid = player.getUniqueId();
        steps.put(uuid, step);
        db.async(() -> store.save(uuid, step));
    }

    private String step(Player player) {
        return steps.get(player.getUniqueId());
    }

    private List<String> zones() {
        return plugin.getRegistry().getRegions().zonesOf(REGION);
    }

    private String firstUnmaxed(Player player) {
        for (String zone : zones()) {
            if (!shop.maxed(player, zone)) return zone;
        }
        return null;
    }

    private String zoneName(String zoneId) {
        UpgradeZone zone = plugin.getRegistry().getUpgradeZone(zoneId);
        return "the " + (zone != null ? zone.name() : zoneId);
    }

    private String placeName(String zoneId) {
        return zoneId.equals(GATE) ? GATE_NAME : zoneName(zoneId);
    }

    // The nearest spot of the zone's trader in the player's world, null if none stands there
    private Location target(Player player, String zoneId) {
        String traderId = zoneId.equals(GATE) ? GATE_TRADER : zoneId + "_trader";
        String world = player.getWorld().getName();
        Location here = player.getLocation();
        Location best = null;
        for (TraderSpot spot : traders.getSpots()) {
            if (!spot.traderId().equals(traderId) || !spot.world().equals(world)) continue;
            Location location = spot.toLocation();
            if (location != null && (best == null || here.distanceSquared(location) < here.distanceSquared(best))) {
                best = location;
            }
        }
        return best;
    }

    // The step in chat and the bar
    private void show(Player player) {
        String step = step(player);
        if (step == null || step.equals(DONE)) {
            hideBar(player);
            return;
        }
        String hint = step.startsWith(GO)
                ? "Follow the trail to " + placeName(step.substring(GO.length())) + "'s trader"
                : "Mine here and buy from its trader until every upgrade is bought";
        if (step.equals(GO + GATE)) hint = "Follow the trail to the gate, the way to Gormak";
        player.sendMessage(TextUtils.parse("<gold><b>QUEST</b> <white>" + title(step) + " <dark_gray>- <gray>" + hint));
        updateBar(player);
    }

    private String title(String step) {
        if (step.startsWith(GO)) return "Go to " + placeName(step.substring(GO.length()));
        return "Max out " + zoneName(step);
    }

    private void updateBar(Player player) {
        String step = step(player);
        if (step == null || step.equals(DONE)) return;
        String detail;
        float fill;
        if (step.startsWith(GO)) {
            Location target = target(player, step.substring(GO.length()));
            if (target == null) {
                detail = "";
                fill = 0f;
            } else {
                double distance = player.getLocation().distance(target);
                double from = walkFrom.getOrDefault(player.getUniqueId(), distance);
                detail = " <gray>(" + (int) Math.round(distance) + "m)";
                fill = from <= 0 ? 0f : (float) Math.max(0, Math.min(1, 1 - distance / from));
            }
        } else {
            UpgradeZone zone = plugin.getRegistry().getUpgradeZone(step);
            int total = zone == null ? 0 : zone.steps().size();
            int bought = 0;
            if (zone != null) {
                for (String tool : zone.tools()) bought += shop.bought(player, zone, tool);
            }
            detail = " <gray>(" + Math.min(bought, total) + "/" + total + " upgrades)";
            fill = total == 0 ? 0f : Math.min(1f, (float) bought / total);
        }
        Component name = TextUtils.parse("<gold><b>Origin Isles</b> <dark_gray>» <white>" + title(step) + detail);

        BossBar bar = bars.get(player.getUniqueId());
        if (bar == null) {
            bar = BossBar.bossBar(name, fill, BossBar.Color.YELLOW, BossBar.Overlay.PROGRESS);
            bars.put(player.getUniqueId(), bar);
            player.showBossBar(bar);
        } else {
            bar.name(name);
            bar.progress(fill);
        }
    }

    private void hideBar(Player player) {
        BossBar bar = bars.remove(player.getUniqueId());
        if (bar != null) player.hideBossBar(bar);
    }

    // Trades what it holds for the player with their other loadout, see LoadoutManager
    @Override
    public void swapLoadout(Player player, UUID other) {
        UUID uuid = player.getUniqueId();
        walkFrom.remove(uuid);
        hideBar(player);
        Swaps.keys(steps, uuid, other);
        // After the loadout's tools and tutorial are in place
        showLater(player, 20L);
    }
}
