package org.charky.aurora.features.movement;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;
import org.charky.aurora.common.util.TextUtils;
import org.charky.aurora.core.DataRegistry;
import org.charky.aurora.features.perk.Perk;
import org.charky.aurora.features.perk.PerkManager;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

// Dash, an Essence shop perk: right click the Dash Feather to dash the way you face,
// up to twice a second. /dash, or clicking Dash in the shop, hands the feather over again.
// The Gauntlet sells a Dash Feather of its own (PerkManager.has)
public class DashListener implements Listener, CommandExecutor {
    public static final String ITEM = "dash_feather";
    private static final long COOLDOWN_MS = 500;
    private static final double SPEED = 1.4;
    private static final double LIFT = 0.25;

    private final DataRegistry registry;
    private final PerkManager perks;
    private final Map<UUID, Long> lastDash = new HashMap<>();

    public DashListener(DataRegistry registry, PerkManager perks) {
        this.registry = registry;
        this.perks = perks;
    }

    public void giveFeather(Player player) {
        ItemStack feather = registry.getAllItems().get(ITEM);
        if (feather == null) return;
        if (player.getInventory().firstEmpty() == -1) {
            TextUtils.error(player, "Your inventory is full");
            return;
        }
        player.getInventory().addItem(feather.clone());
        TextUtils.success(player, "Right click the Dash Feather to dash");
    }

    @EventHandler
    public void onUse(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (!ITEM.equals(DataRegistry.itemId(event.getItem()))) return;
        event.setCancelled(true);

        Player player = event.getPlayer();
        if (!perks.has(player, Perk.DASH)) {
            player.sendActionBar(Component.text("Buy Dash in the Essence shop, /shop", NamedTextColor.RED));
            return;
        }
        if (player.isInsideVehicle() || player.isGliding() || player.isFlying()) return;

        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();
        Long dashed = lastDash.get(uuid);
        if (dashed != null && now - dashed < COOLDOWN_MS) return;
        lastDash.put(uuid, now);

        // Along the ground, whatever the pitch, with a small hop so it clears a step
        Vector direction = player.getLocation().getDirection().setY(0);
        if (direction.lengthSquared() < 1e-6) return;
        player.setVelocity(direction.normalize().multiply(SPEED).setY(LIFT));
        player.getWorld().spawnParticle(Particle.CLOUD, player.getLocation().add(0, 0.2, 0), 12, 0.3, 0.1, 0.3, 0.02);
        player.playSound(player.getLocation(), Sound.ENTITY_BREEZE_JUMP, 0.8f, 1.4f);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        lastDash.remove(event.getPlayer().getUniqueId());
    }

    // /dash, the feather again
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String @NotNull [] args) {
        if (!(sender instanceof Player player)) return false;
        if (!perks.has(player, Perk.DASH)) {
            player.sendActionBar(Component.text("Buy Dash in the Essence shop, /shop", NamedTextColor.RED));
            return true;
        }
        giveFeather(player);
        return true;
    }
}
