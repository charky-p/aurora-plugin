package org.charky.aurora.features.perk;

import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.charky.aurora.common.util.TextUtils;
import org.charky.aurora.core.Aurora;
import org.charky.aurora.core.database.Database;
import org.charky.aurora.core.database.PerkStore;
import org.charky.aurora.features.player.PlayerData;
import org.charky.aurora.features.player.PlayerManager;
import org.charky.aurora.features.player.ScoreboardManager;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

// The perk levels each player has bought, and what those levels do
public class PerkManager implements Listener {
    // Ten hearts, what every player has before Vitality
    private static final double BASE_HEALTH = 20.0;

    private final Aurora plugin;
    private final Database db;
    private final PerkStore store;
    private final PlayerManager players;
    private final ScoreboardManager scoreboard;
    private final Map<UUID, Map<String, Integer>> levels;

    public PerkManager(Aurora plugin, Database db, PerkStore store, PlayerManager players, ScoreboardManager scoreboard) {
        this.plugin = plugin;
        this.db = db;
        this.store = store;
        this.players = players;
        this.scoreboard = scoreboard;
        this.levels = store.loadAll();
    }

    public int level(Player player, Perk perk) {
        return levels.getOrDefault(player.getUniqueId(), Map.of()).getOrDefault(perk.id(), 0);
    }

    // A crit doubles a mined block's drops, one roll a block
    public boolean rollCrit(Player player) {
        return ThreadLocalRandom.current().nextInt(100) < level(player, Perk.CRIT_RATE);
    }

    public boolean hasKeyring(Player player) {
        return level(player, Perk.KEYRING) > 0;
    }

    // Null if the next level was bought, otherwise why not
    public String buy(Player player, Perk perk) {
        PlayerData data = players.get(player);
        if (data == null) return "Your profile is still loading";

        int next = level(player, perk) + 1;
        if (next > perk.maxLevel()) return "That perk is already at its highest level";
        if (!data.spendEssence(perk.cost(next))) {
            return "You need " + (perk.cost(next) - data.getEssence()) + " more Essence";
        }

        UUID uuid = player.getUniqueId();
        levels.computeIfAbsent(uuid, k -> new HashMap<>()).put(perk.id(), next);
        db.async(() -> store.save(uuid, perk.id(), next));

        apply(player);
        scoreboard.updateBoard(player);
        player.sendMessage(TextUtils.parse("<gold><b>PERK</b> ").append(TextUtils.parse(perk.display()))
                .append(TextUtils.parse(" <yellow>" + next + "<dark_gray>/<gray>" + perk.maxLevel())));
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.6f, 1.2f);
        return null;
    }

    // Vitality is real health: mobs hit for a set number of hearts, so twice the
    // hearts takes twice the hits
    private void apply(Player player) {
        AttributeInstance maxHealth = player.getAttribute(Attribute.MAX_HEALTH);
        if (maxHealth != null) maxHealth.setBaseValue(BASE_HEALTH + level(player, Perk.VITALITY));
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        apply(event.getPlayer());
    }

    // Next tick, during the event the player is not the respawned one yet
    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (!player.isOnline()) return;
            apply(player);
            AttributeInstance maxHealth = player.getAttribute(Attribute.MAX_HEALTH);
            if (maxHealth != null) player.setHealth(maxHealth.getValue());
        });
    }
}
