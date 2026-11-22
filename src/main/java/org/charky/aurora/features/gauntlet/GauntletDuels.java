package org.charky.aurora.features.gauntlet;

import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.potion.PotionEffect;
import org.bukkit.scheduler.BukkitTask;
import org.charky.aurora.common.util.TextUtils;
import org.charky.aurora.core.Aurora;

import java.time.Duration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.UUID;

// 1v1 duels in the Gauntlet: /gauntlet duel <player> sends a challenge, open for
// REQUEST_MS, that they accept or decline with a click. Both get their own copy of the
// duel arena and their setup, a countdown they stand still for, and fight by the PvP
// zone's rules. A blow that would kill ends it, as does leaving, /spawn (a forfeit) or
// the clock (duel_seconds, more health left wins). The winner takes rating from the
// loser (GauntletElo) and some Shards, never twice in a row from the same player. Both
// are back in the lobby with their setup
public class GauntletDuels implements Listener {
    public static final String WINS = "duel_wins";
    public static final String LOSSES = "duel_losses";
    public static final String RATING = "duel_rating";
    private static final long REQUEST_MS = 30_000;
    private static final int COUNTDOWN_SECONDS = 3;

    private record Request(UUID from, long expires) {
    }

    private static final class Duel {
        final UUID a;
        final UUID b;
        final int slot;
        boolean fighting;
        long started;
        BukkitTask task;

        Duel(UUID a, UUID b, int slot) {
            this.a = a;
            this.b = b;
            this.slot = slot;
        }

        UUID other(UUID one) {
            return one.equals(a) ? b : a;
        }
    }

    private final Aurora plugin;
    private final GauntletManager manager;
    // Open challenges, by who was challenged
    private final Map<UUID, Request> requests = new HashMap<>();
    // Duels in progress, under both players
    private final Map<UUID, Duel> duels = new HashMap<>();
    // Who each winner was last paid for beating
    private final Map<UUID, UUID> lastPaid = new HashMap<>();

    public GauntletDuels(Aurora plugin, GauntletManager manager) {
        this.plugin = plugin;
        this.manager = manager;
    }

    public boolean inDuel(Player player) {
        return duels.containsKey(player.getUniqueId());
    }

    // Both in the same duel, past its countdown
    public boolean areDueling(Player one, Player two) {
        Duel duel = duels.get(one.getUniqueId());
        return duel != null && duel.fighting && duel == duels.get(two.getUniqueId());
    }

    public static int rating(GauntletProfile profile) {
        int rating = profile.stat(RATING);
        return rating == 0 ? GauntletElo.START : rating;
    }

    // The other duelist, null outside a duel
    public Player opponent(Player player) {
        Duel duel = duels.get(player.getUniqueId());
        return duel == null ? null : Bukkit.getPlayer(duel.other(player.getUniqueId()));
    }

    public void shutdown() {
        for (Duel duel : new HashSet<>(duels.values())) finish(duel, null, "the server stopped");
    }

    // ---- Challenges ----

    private String canDuel(Player player) {
        if (!manager.isHere(player)) return "Go to the Gauntlet first";
        if (manager.inRun(player)) return "Not during a run";
        if (inDuel(player)) return "Already in a duel";
        if (manager.isWatching(player)) return "Stop spectating first";
        if (manager.pvp() != null && manager.pvp().inZone(player)) return "Leave the PvP zone first";
        return null;
    }

    // Null if the challenge was sent, otherwise why not
    public String challenge(Player from, Player to) {
        if (to == null) return "That player is not online";
        if (from.equals(to)) return "You can't duel yourself";
        String problem = canDuel(from);
        if (problem != null) return problem;
        if (canDuel(to) != null) return to.getName() + " can't duel right now";
        String missing = manager.arena().duelMissing();
        if (missing != null) return "Duels are not set up yet (" + missing + ")";
        requests.put(to.getUniqueId(), new Request(from.getUniqueId(), System.currentTimeMillis() + REQUEST_MS));
        TextUtils.send(from, "<gray>Duel challenge sent to <white>" + to.getName() + "<gray>, it lasts " + REQUEST_MS / 1000 + " seconds");
        TextUtils.send(to, "<red><b>DUEL</b> <white>" + from.getName() + " <gray>(rating " + rating(manager.profile(from))
                + ") challenges you! <click:run_command:'/gauntlet duel accept'><hover:show_text:'<gray>Fight them'><green><b>[ACCEPT]</b></hover></click> "
                + "<click:run_command:'/gauntlet duel decline'><hover:show_text:'<gray>Not now'><red><b>[DECLINE]</b></hover></click>");
        to.playSound(to.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 0.8f, 1.2f);
        return null;
    }

    public String accept(Player to) {
        Request request = requests.remove(to.getUniqueId());
        if (request == null || request.expires() < System.currentTimeMillis()) return "You have no duel challenge to accept";
        Player from = Bukkit.getPlayer(request.from());
        if (from == null) return "They are not online any more";
        String problem = canDuel(from);
        if (problem == null) problem = canDuel(to);
        if (problem != null) return "The duel can't start: " + problem;
        String missing = manager.arena().duelMissing();
        if (missing != null) return "Duels are not set up yet (" + missing + ")";
        start(from, to);
        return null;
    }

    public String decline(Player to) {
        Request request = requests.remove(to.getUniqueId());
        if (request == null) return "You have no duel challenge";
        Player from = Bukkit.getPlayer(request.from());
        if (from != null) TextUtils.send(from, "<gray>" + to.getName() + " declined your duel");
        TextUtils.send(to, "<gray>Duel declined");
        return null;
    }

    public String forfeit(Player player) {
        Duel duel = duels.get(player.getUniqueId());
        if (duel == null) return "You are not in a duel";
        finish(duel, duel.other(player.getUniqueId()), player.getName() + " gave up");
        return null;
    }

    // ---- The fight ----

    private void start(Player a, Player b) {
        GauntletArena arena = manager.arena();
        int slot = arena.claimDuelSlot();
        arena.pasteDuel(slot);
        Duel duel = new Duel(a.getUniqueId(), b.getUniqueId(), slot);
        duels.put(duel.a, duel);
        duels.put(duel.b, duel);
        ready(a, arena.duelSpawn(slot, 1), b);
        ready(b, arena.duelSpawn(slot, 2), a);

        int[] left = {COUNTDOWN_SECONDS};
        duel.task = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            if (duels.get(duel.a) != duel) return;
            Player pa = Bukkit.getPlayer(duel.a), pb = Bukkit.getPlayer(duel.b);
            if (left[0] > 0) {
                for (Player p : new Player[]{pa, pb}) {
                    if (p == null) continue;
                    p.showTitle(Title.title(TextUtils.parse("<red><b>" + left[0]), TextUtils.parse("<gray>Get ready"),
                            Title.Times.times(Duration.ZERO, Duration.ofMillis(900), Duration.ofMillis(100))));
                    p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_HAT, 1f, 1f);
                }
                left[0]--;
                return;
            }
            duel.task.cancel();
            duel.fighting = true;
            duel.started = System.currentTimeMillis();
            for (Player p : new Player[]{pa, pb}) {
                if (p == null) continue;
                p.showTitle(Title.title(TextUtils.parse("<red><b>FIGHT!"), TextUtils.parse(""),
                        Title.Times.times(Duration.ZERO, Duration.ofMillis(800), Duration.ofMillis(300))));
                p.playSound(p.getLocation(), Sound.EVENT_RAID_HORN, 0.7f, 1.4f);
            }
            long seconds = (long) manager.data().setting("duel_seconds", 180);
            duel.task = plugin.getServer().getScheduler().runTaskLater(plugin, () -> timeUp(duel), seconds * 20);
        }, 20L, 20L);
    }

    private void ready(Player player, Location spawn, Player against) {
        player.closeInventory();
        manager.giveKit(player);
        heal(player);
        if (spawn != null) player.teleport(spawn);
        player.showTitle(Title.title(TextUtils.parse("<red><b>DUEL"), TextUtils.parse("<gray>vs <white>" + against.getName())));
        TextUtils.send(player, "<red><b>DUEL</b> <gray>vs <white>" + against.getName() + " <gray>(rating "
                + rating(manager.profile(against)) + "). <white>/spawn</white> gives up");
    }

    // The clock ran out: more health left wins, the same is a draw
    private void timeUp(Duel duel) {
        if (duels.get(duel.a) != duel) return;
        Player a = Bukkit.getPlayer(duel.a), b = Bukkit.getPlayer(duel.b);
        double ha = a == null ? 0 : share(a), hb = b == null ? 0 : share(b);
        UUID winner = Math.abs(ha - hb) < 0.01 ? null : ha > hb ? duel.a : duel.b;
        finish(duel, winner, "time ran out");
    }

    private static double share(Player player) {
        AttributeInstance max = player.getAttribute(Attribute.MAX_HEALTH);
        return player.getHealth() / (max == null ? 20 : max.getValue());
    }

    // Rating, wins and losses, the winner's Shards, then both back in the lobby
    private void finish(Duel duel, UUID winner, String how) {
        if (duels.get(duel.a) != duel) return;
        duels.remove(duel.a);
        duels.remove(duel.b);
        if (duel.task != null) duel.task.cancel();
        manager.arena().freeDuelSlot(duel.slot);

        String result;
        if (winner != null) {
            UUID loser = duel.other(winner);
            GauntletProfile won = manager.profile(winner), lost = manager.profile(loser);
            int[] after = GauntletElo.after(rating(won), rating(lost), manager.data().setting("duel_k", 32));
            int gain = after[0] - rating(won);
            manager.setStat(winner, RATING, after[0]);
            manager.setStat(loser, RATING, after[1]);
            manager.addStat(winner, WINS, 1);
            manager.addStat(loser, LOSSES, 1);
            Player winning = Bukkit.getPlayer(winner);
            if (!loser.equals(lastPaid.get(winner))) {
                lastPaid.put(winner, loser);
                int shards = (int) manager.data().setting("duel_win_shards", 10);
                if (winning != null) manager.giveShards(winning, shards);
                else {
                    won.shards += shards;
                    manager.save(winner);
                }
            }
            if (winning != null) manager.challenges().add(winning, "pvp_kills", 1);
            manager.weekly().pvpWin(winner);
            result = "<gold>" + GauntletManager.nameOf(winner) + " <gray>beat <white>" + GauntletManager.nameOf(loser)
                    + " <gray>in a duel, " + how + " <dark_gray>(" + after[0] + " +" + gain + " / " + after[1] + " -" + gain + ")";
        } else {
            result = "<gray>The duel between <white>" + GauntletManager.nameOf(duel.a) + " <gray>and <white>"
                    + GauntletManager.nameOf(duel.b) + " <gray>is a draw, " + how;
        }
        Location lobby = manager.arena().lobby();
        if (lobby != null && lobby.getWorld() != null) {
            for (Player player : lobby.getWorld().getPlayers()) TextUtils.send(player, result);
        }
        for (UUID id : new UUID[]{duel.a, duel.b}) {
            Player player = Bukkit.getPlayer(id);
            if (player == null) continue;
            boolean won = id.equals(winner);
            player.showTitle(Title.title(TextUtils.parse(winner == null ? "<gray><b>DRAW" : won ? "<gold><b>VICTORY" : "<red><b>DEFEAT"),
                    TextUtils.parse("<gray>" + how)));
            for (PotionEffect effect : player.getActivePotionEffects()) player.removePotionEffect(effect.getType());
            heal(player);
            if (manager.isHere(player) && lobby != null) player.teleport(lobby);
            manager.giveKit(player);
        }
    }

    private static void heal(Player player) {
        AttributeInstance max = player.getAttribute(Attribute.MAX_HEALTH);
        player.setHealth(max == null ? 20 : max.getValue());
        player.setFoodLevel(20);
        player.setSaturation(10);
        player.setFireTicks(0);
    }

    // A blow that would kill ends the duel, a totem in hand still saves. Nothing hurts
    // during the countdown
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onLethal(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        Duel duel = duels.get(player.getUniqueId());
        if (duel == null) return;
        if (!duel.fighting) {
            event.setCancelled(true);
            return;
        }
        if (event.getFinalDamage() < player.getHealth() + player.getAbsorptionAmount()) return;
        PlayerInventory inventory = player.getInventory();
        if (inventory.getItemInMainHand().getType() == Material.TOTEM_OF_UNDYING
                || inventory.getItemInOffHand().getType() == Material.TOTEM_OF_UNDYING) return;
        event.setCancelled(true);
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_DEATH, 1f, 1f);
        finish(duel, duel.other(player.getUniqueId()), player.getName() + " fell");
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        requests.remove(player.getUniqueId());
        Duel duel = duels.get(player.getUniqueId());
        if (duel != null) finish(duel, duel.other(player.getUniqueId()), player.getName() + " left");
    }

    // Standing still for the countdown
    @EventHandler(ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        Duel duel = duels.get(event.getPlayer().getUniqueId());
        if (duel == null || duel.fighting) return;
        Location from = event.getFrom(), to = event.getTo();
        if (from.getX() != to.getX() || from.getY() != to.getY() || from.getZ() != to.getZ()) {
            event.setTo(from.clone().setDirection(to.getDirection()));
        }
    }

    // Pearls and chorus fruit stay in the arena; any other way out gives up the duel
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onTeleport(PlayerTeleportEvent event) {
        Player player = event.getPlayer();
        Duel duel = duels.get(player.getUniqueId());
        if (duel == null || manager.arena().isInDuelSlot(duel.slot, event.getTo())) return;
        PlayerTeleportEvent.TeleportCause cause = event.getCause();
        if (cause == PlayerTeleportEvent.TeleportCause.ENDER_PEARL || cause == PlayerTeleportEvent.TeleportCause.CHORUS_FRUIT
                || cause == PlayerTeleportEvent.TeleportCause.CONSUMABLE_EFFECT) {
            event.setCancelled(true);
            return;
        }
        // Still on the way in, the first teleport to the spawn
        if (!duel.fighting && manager.arena().isInDuelSlot(duel.slot, event.getTo())) return;
        if (duel.fighting) finish(duel, duel.other(player.getUniqueId()), player.getName() + " left the arena");
    }

    // /spawn and /gauntlet leave give up the duel, a run can't start from one
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        if (!inDuel(player)) return;
        String command = event.getMessage().toLowerCase().trim().replaceAll("\\s+", " ");
        if (command.equals("/spawn") || command.startsWith("/spawn ") || command.equals("/gauntlet leave")) {
            event.setCancelled(true);
            forfeit(player);
        } else if (command.startsWith("/gauntlet start")) {
            event.setCancelled(true);
            TextUtils.error(player, "Not during a duel");
        }
    }
}
