package org.charky.aurora.features.cosmetic;

import io.papermc.paper.event.player.AsyncChatEvent;
import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.meta.trim.ArmorTrim;
import org.bukkit.inventory.meta.trim.TrimMaterial;
import org.bukkit.inventory.meta.trim.TrimPattern;
import org.bukkit.scheduler.BukkitTask;
import org.charky.aurora.common.util.TextUtils;
import org.charky.aurora.core.Aurora;
import org.charky.aurora.core.database.CosmeticStore;
import org.charky.aurora.core.database.Database;
import org.charky.aurora.features.cosmetic.Cosmetic.Slot;
import org.charky.aurora.features.cosmetic.Cosmetic.Source;
import org.charky.aurora.features.cosmetic.Cosmetic.Type;
import org.charky.aurora.features.player.PlayerData;
import org.charky.aurora.features.player.PlayerManager;
import org.charky.aurora.features.player.ScoreboardManager;
import org.charky.aurora.features.player.TabManager;
import org.charky.aurora.features.rank.RankManager;
import org.charky.aurora.features.rank.Ranks;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

// Which cosmetics a player owns and wears, and everywhere they show: the chat line,
// the tab list, the join message, the trail behind them and the mobs they kill
public class CosmeticManager implements Listener {
    // How often a trail leaves particles behind a moving player
    private static final long TRAIL_TICKS = 4L;
    // Standing still leaves nothing, this far counts as having moved
    private static final double TRAIL_MIN_MOVE_SQUARED = 0.01;

    private final Aurora plugin;
    private final Database db;
    private final CosmeticStore store;
    private final PlayerManager players;
    private final RankManager ranks;
    private final ScoreboardManager scoreboard;
    private final TabManager tab;
    private final Map<UUID, Set<String>> owned;
    private final Map<UUID, Map<String, String>> equipped;
    // Where each player was the last time trails were drawn
    private final Map<UUID, Location> lastSeen = new HashMap<>();
    private final BukkitTask trails;
    // Redraws the armour a player wears, after they change its trim. Tools load
    // after cosmetics, so it is set once they have
    private Consumer<Player> lookChanged = player -> {};

    public void setLookChanged(Consumer<Player> lookChanged) {
        this.lookChanged = lookChanged;
    }

    // The trim on a player's armour, null with no pattern chosen
    public ArmorTrim trim(Player player) {
        Cosmetic pattern = equipped(player.getUniqueId(), Slot.TRIM_PATTERN);
        Cosmetic material = equipped(player.getUniqueId(), Slot.TRIM_MATERIAL);
        if (pattern == null || material == null) return null;

        TrimPattern trimPattern = RegistryAccess.registryAccess().getRegistry(RegistryKey.TRIM_PATTERN)
                .get(NamespacedKey.minecraft(pattern.value()));
        TrimMaterial trimMaterial = RegistryAccess.registryAccess().getRegistry(RegistryKey.TRIM_MATERIAL)
                .get(NamespacedKey.minecraft(material.value()));
        return trimPattern == null || trimMaterial == null ? null : new ArmorTrim(trimMaterial, trimPattern);
    }

    public CosmeticManager(Aurora plugin, Database db, CosmeticStore store, PlayerManager players,
                           RankManager ranks, ScoreboardManager scoreboard, TabManager tab) {
        this.plugin = plugin;
        this.db = db;
        this.store = store;
        this.players = players;
        this.ranks = ranks;
        this.scoreboard = scoreboard;
        this.tab = tab;
        this.owned = store.loadOwned();
        this.equipped = store.loadEquipped();
        tab.setNames(this::coloredName);
        this.trails = Bukkit.getScheduler().runTaskTimer(plugin, this::drawTrails, TRAIL_TICKS, TRAIL_TICKS);
    }

    public void shutdown() {
        trails.cancel();
    }

    // ------------------------------------------------------------------ owning

    public boolean owns(Player player, Cosmetic cosmetic) {
        return switch (cosmetic.source()) {
            case DEFAULT -> true;
            case PLAYTIME -> ranks.playtimeOf(player) >= cosmetic.requirement();
            // The AURORA rank comes with every chat colour
            case ESSENCE -> has(player.getUniqueId(), cosmetic.id())
                    || (cosmetic.type() == Type.CHAT_COLOR && ranks.owns(player, Ranks.get("AURORA")));
            case GRANT -> has(player.getUniqueId(), cosmetic.id());
        };
    }

    public boolean hasSecondSuffix(Player player) {
        return has(player.getUniqueId(), Cosmetics.SECOND_SUFFIX_SLOT);
    }

    private boolean has(UUID uuid, String id) {
        return owned.getOrDefault(uuid, Set.of()).contains(id);
    }

    // Null if it was bought and put on, otherwise why not
    public String buy(Player player, Cosmetic cosmetic) {
        PlayerData data = players.get(player);
        if (data == null) return "Your profile is still loading";
        if (cosmetic.source() != Source.ESSENCE) return "That is not for sale";
        if (owns(player, cosmetic)) return "You already own that";
        if (!data.spendEssence((int) cosmetic.requirement())) {
            return "You need " + (cosmetic.requirement() - data.getEssence()) + " more Essence";
        }

        give(player.getUniqueId(), cosmetic.id());
        scoreboard.updateBoard(player);
        player.sendMessage(TextUtils.parse("<gold><b>UNLOCKED</b> ").append(cosmetic.component()));
        player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.6f, 1f);

        // Straight on, into the first slot that is free for a suffix
        Slot slot = Slot.of(cosmetic.type());
        if (cosmetic.type() == Type.SUFFIX && equipped(player.getUniqueId(), Slot.SUFFIX_1) != null
                && hasSecondSuffix(player)) {
            slot = Slot.SUFFIX_2;
        }
        equip(player, slot, cosmetic);
        return null;
    }

    // For admins: a cosmetic id or the second suffix slot. True if it changed anything
    public boolean give(UUID uuid, String id) {
        if (!owned.computeIfAbsent(uuid, k -> ConcurrentHashMap.newKeySet()).add(id)) return false;
        db.async(() -> store.addOwned(uuid, id));
        return true;
    }

    public boolean take(UUID uuid, String id) {
        if (!owned.getOrDefault(uuid, Set.of()).remove(id)) return false;
        db.async(() -> store.removeOwned(uuid, id));

        // Whatever was being worn goes back to what the slot starts with
        for (Slot slot : Slot.values()) {
            boolean lostSlot = slot == Slot.SUFFIX_2 && id.equals(Cosmetics.SECOND_SUFFIX_SLOT);
            if (lostSlot || id.equals(equipped.getOrDefault(uuid, Map.of()).get(slot.id()))) {
                equipped.get(uuid).remove(slot.id());
                db.async(() -> store.setEquipped(uuid, slot.id(), lostSlot ? Cosmetics.NONE : defaultId(slot)));
            }
        }
        tab.updateAll();
        Player player = Bukkit.getPlayer(uuid);
        if (player != null) lookChanged.accept(player);
        return true;
    }

    private static String defaultId(Slot slot) {
        Cosmetic standard = Cosmetics.defaultFor(slot);
        return standard == null ? Cosmetics.NONE : standard.id();
    }

    // ----------------------------------------------------------------- wearing

    // What a slot holds, null for nothing. A slot never touched holds its default
    public Cosmetic equipped(UUID uuid, Slot slot) {
        String id = equipped.getOrDefault(uuid, Map.of()).get(slot.id());
        if (id == null) return Cosmetics.defaultFor(slot);
        return Cosmetics.get(id);
    }

    // Null if it is now worn, otherwise why not
    public String equip(Player player, Slot slot, Cosmetic cosmetic) {
        if (cosmetic.type() != slot.type()) return "That does not go there";
        if (!owns(player, cosmetic)) return "You have not unlocked that";
        if (slot == Slot.SUFFIX_2 && !hasSecondSuffix(player)) return "You have not unlocked a second suffix";

        UUID uuid = player.getUniqueId();
        // The same suffix twice would only repeat itself
        if (cosmetic.type() == Type.SUFFIX) {
            Slot other = slot == Slot.SUFFIX_1 ? Slot.SUFFIX_2 : Slot.SUFFIX_1;
            if (equipped(uuid, other) == cosmetic) set(uuid, other, Cosmetics.NONE);
        }

        set(uuid, slot, cosmetic.id());
        tab.updateAll();
        lookChanged.accept(player);
        return null;
    }

    public void unequip(Player player, Slot slot) {
        // A suffix comes off to nothing, a colour or a join message to the plain one
        set(player.getUniqueId(), slot, slot.type() == Type.SUFFIX ? Cosmetics.NONE : defaultId(slot));
        tab.updateAll();
        lookChanged.accept(player);
    }

    private void set(UUID uuid, Slot slot, String id) {
        equipped.computeIfAbsent(uuid, k -> new ConcurrentHashMap<>()).put(slot.id(), id);
        db.async(() -> store.setEquipped(uuid, slot.id(), id));
    }

    // ------------------------------------------------------------------ showing

    private NamedTextColor color(UUID uuid, Slot slot, String fallback) {
        Cosmetic worn = equipped(uuid, slot);
        NamedTextColor color = NamedTextColor.NAMES.value(worn == null ? fallback : worn.value());
        return color == null ? NamedTextColor.NAMES.value(fallback) : color;
    }

    // The player's name in the colour they chose, for chat and the tab list
    public Component coloredName(Player player) {
        return Component.text(player.getName(),
                color(player.getUniqueId(), Slot.NAME_COLOR, Cosmetics.DEFAULT_NAME_COLOR));
    }

    // " [ONE] [TWO]", empty with none worn
    private Component suffixes(UUID uuid) {
        Component result = Component.empty();
        for (Slot slot : new Slot[]{Slot.SUFFIX_1, Slot.SUFFIX_2}) {
            Cosmetic suffix = equipped(uuid, slot);
            if (suffix != null) result = result.append(Component.space()).append(suffix.component());
        }
        return result;
    }

    // Rank, name, suffixes, then what they said. The renderer runs off the main
    // thread, so everything it shows is worked out here first
    @EventHandler(ignoreCancelled = true)
    public void onChat(AsyncChatEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        Component prefix = Component.empty()
                .append(ranks.prefix(player))
                .append(Component.space())
                .append(coloredName(player))
                .append(suffixes(uuid))
                .append(Component.text(" » ", NamedTextColor.DARK_GRAY));
        NamedTextColor chat = color(uuid, Slot.CHAT_COLOR, Cosmetics.DEFAULT_CHAT_COLOR);

        event.renderer((source, displayName, message, viewer) -> prefix.append(message.colorIfAbsent(chat)));
    }

    // How a player's death reads, with <player> and, when someone killed them,
    // <killer> still to fill in
    public String deathMessage(UUID uuid, boolean killed) {
        Cosmetic message = equipped(uuid, Slot.DEATH_MESSAGE);
        if (message == null) message = Cosmetics.defaultFor(Slot.DEATH_MESSAGE);

        String[] lines = message.value().split(java.util.regex.Pattern.quote(Cosmetics.DEATH_SEP));
        return killed || lines.length < 2 ? lines[0] : lines[1];
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        Cosmetic message = equipped(player.getUniqueId(), Slot.JOIN_MESSAGE);
        if (message == null) return;

        event.joinMessage(TextUtils.parse(message.value().replace(Cosmetics.PLAYER_TAG, player.getName())));
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        lastSeen.remove(event.getPlayer().getUniqueId());
    }

    private void drawTrails() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            Cosmetic trail = equipped(player.getUniqueId(), Slot.TRAIL);
            if (trail == null) continue;

            Location now = player.getLocation();
            Location before = lastSeen.put(player.getUniqueId(), now);
            if (before == null || before.getWorld() != now.getWorld()) continue;
            if (before.distanceSquared(now) < TRAIL_MIN_MOVE_SQUARED) continue;
            if (player.getGameMode() == GameMode.SPECTATOR || player.isInvisible()) continue;

            try {
                now.getWorld().spawnParticle(Particle.valueOf(trail.value()), now.clone().add(0, 0.15, 0),
                        2, 0.15, 0.05, 0.15, 0);
            } catch (IllegalArgumentException e) {
                // A particle this server version does not have, nothing to draw
            }
        }
    }

    // MONITOR, this only watches
    @EventHandler(priority = EventPriority.MONITOR)
    public void onKill(EntityDeathEvent event) {
        Player killer = event.getEntity().getKiller();
        if (killer == null || event.getEntity() instanceof Player) return;

        Cosmetic effect = equipped(killer.getUniqueId(), Slot.KILL_EFFECT);
        if (effect != null) KillEffects.play(effect.value(), event.getEntity().getLocation());
    }

    // -------------------------------------------------------------------- menus

    public void openHub(Player player) {
        new CustomiseMenu(this, ranks, plugin.getRegistry()).open(player);
    }

    public void open(Player player, Type type) {
        new CosmeticMenu(this, ranks, plugin.getRegistry(), type).open(player);
    }

    // Everything owned and worn goes, back to what a new player has
    public void reset(Player player) {
        UUID uuid = player.getUniqueId();
        owned.remove(uuid);
        equipped.remove(uuid);
        db.async(() -> store.deleteAll(uuid));
        tab.updateAll();
        lookChanged.accept(player);
    }
}
