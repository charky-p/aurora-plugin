package org.charky.aurora.features.movement;

import org.bukkit.Bukkit;
import org.bukkit.Input;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Horse;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDismountEvent;
import org.bukkit.event.entity.EntityMountEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Vector;
import org.charky.aurora.common.util.TextUtils;
import org.charky.aurora.core.DataRegistry;
import org.charky.aurora.features.perk.Perk;
import org.charky.aurora.features.perk.PerkManager;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

// Mounts from the Essence shop. Right clicking a mount's saddle puts you on it, and
// getting off (shift) makes it vanish. The horse and the happy ghast steer the vanilla
// way; the pig and the chicken cannot, so they follow where you look and the keys you
// press. A mount cannot be hurt, and only its owner can ride it
public class MountManager implements Listener {
    private static final String ITEM_PREFIX = "mount_";
    // How fast the pig and the chicken walk, blocks a tick
    private static final Map<String, Double> WALK = Map.of("pig", 0.32, "chicken", 0.26);
    private static final double JUMP = 0.45;

    private final Plugin plugin;
    private final DataRegistry registry;
    private final PerkManager perks;
    private final NamespacedKey ownerKey;
    // owner -> the mount they are riding
    private final Map<UUID, Entity> riding = new HashMap<>();

    public MountManager(Plugin plugin, DataRegistry registry, PerkManager perks) {
        this.plugin = plugin;
        this.registry = registry;
        this.perks = perks;
        this.ownerKey = new NamespacedKey(plugin, "mount_owner");
        Bukkit.getScheduler().runTaskTimer(plugin, this::steer, 1L, 1L);
    }

    public void giveSaddle(Player player, String mount) {
        ItemStack saddle = registry.getAllItems().get(ITEM_PREFIX + mount);
        if (saddle == null) return;
        if (player.getInventory().firstEmpty() == -1) {
            TextUtils.error(player, "Your inventory is full");
            return;
        }
        player.getInventory().addItem(saddle.clone());
    }

    @EventHandler
    public void onUse(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        String id = DataRegistry.itemId(event.getItem());
        if (id == null || !id.startsWith(ITEM_PREFIX)) return;
        event.setCancelled(true);

        Player player = event.getPlayer();
        String mount = id.substring(ITEM_PREFIX.length());
        Perk perk = Perk.byId(id);
        if (perk == null || !perks.owns(player, perk)) {
            TextUtils.error(player, "This saddle is not yours");
            return;
        }
        if (player.isInsideVehicle()) return;
        summon(player, mount);
    }

    private void summon(Player player, String mount) {
        dismiss(player.getUniqueId());
        Location at = player.getLocation();
        EntityType type = switch (mount) {
            case "pig" -> EntityType.PIG;
            case "chicken" -> EntityType.CHICKEN;
            case "horse" -> EntityType.HORSE;
            case "ghast" -> EntityType.HAPPY_GHAST;
            default -> null;
        };
        if (type == null) return;

        Entity entity = at.getWorld().spawnEntity(at, type);
        entity.setPersistent(false);
        entity.setInvulnerable(true);
        entity.getPersistentDataContainer().set(ownerKey, PersistentDataType.STRING, player.getUniqueId().toString());
        if (entity instanceof LivingEntity living) living.setRemoveWhenFarAway(false);

        switch (mount) {
            case "pig", "chicken" -> {
                // No wandering of its own, it only goes where the rider steers it
                if (entity instanceof Mob mob) Bukkit.getMobGoals().removeAllGoals(mob);
            }
            case "horse" -> {
                Horse horse = (Horse) entity;
                horse.setAdult();
                horse.setTamed(true);
                horse.setOwner(player);
                horse.getInventory().setSaddle(new ItemStack(Material.SADDLE));
                AttributeInstance speed = horse.getAttribute(Attribute.MOVEMENT_SPEED);
                if (speed != null) speed.setBaseValue(0.32);
                AttributeInstance jump = horse.getAttribute(Attribute.JUMP_STRENGTH);
                if (jump != null) jump.setBaseValue(0.9);
            }
            case "ghast" -> ((LivingEntity) entity).getEquipment().setItem(EquipmentSlot.BODY, new ItemStack(Material.WHITE_HARNESS));
            default -> {
            }
        }

        entity.addPassenger(player);
        riding.put(player.getUniqueId(), entity);
        player.playSound(at, Sound.ENTITY_HORSE_SADDLE, 1f, 1.2f);
    }

    private void dismiss(UUID owner) {
        Entity mount = riding.remove(owner);
        if (mount != null && mount.isValid()) {
            mount.eject();
            mount.remove();
        }
    }

    // Each tick: the pig and the chicken walk where their rider looks and presses
    private void steer() {
        for (Map.Entry<UUID, Entity> entry : riding.entrySet()) {
            Entity mount = entry.getValue();
            Double walk = WALK.get(mountOf(mount));
            if (walk == null || !mount.isValid() || mount.getPassengers().isEmpty()) continue;
            if (!(mount.getPassengers().getFirst() instanceof Player rider)) continue;

            Input input = rider.getCurrentInput();
            float yaw = rider.getLocation().getYaw();
            Vector forward = new Vector(-Math.sin(Math.toRadians(yaw)), 0, Math.cos(Math.toRadians(yaw)));
            Vector side = new Vector(forward.getZ(), 0, -forward.getX());
            Vector move = new Vector();
            if (input.isForward()) move.add(forward);
            if (input.isBackward()) move.subtract(forward);
            if (input.isLeft()) move.add(side);
            if (input.isRight()) move.subtract(side);
            if (move.lengthSquared() > 0) move.normalize().multiply(walk);

            double y = mount.getVelocity().getY();
            if (input.isJump() && mount.isOnGround()) y = JUMP;
            mount.setVelocity(move.setY(y));
            mount.setRotation(yaw, 0);
        }
    }

    private String mountOf(Entity entity) {
        return switch (entity.getType()) {
            case PIG -> "pig";
            case CHICKEN -> "chicken";
            default -> null;
        };
    }

    // Getting off, shift, sends the mount away
    @EventHandler
    public void onDismount(EntityDismountEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        Entity mount = riding.get(player.getUniqueId());
        if (mount == null || !mount.equals(event.getDismounted())) return;
        Bukkit.getScheduler().runTask(plugin, () -> dismiss(player.getUniqueId()));
    }

    // Nobody but the owner gets on
    @EventHandler(ignoreCancelled = true)
    public void onMount(EntityMountEvent event) {
        String owner = event.getMount().getPersistentDataContainer().get(ownerKey, PersistentDataType.STRING);
        if (owner != null && !owner.equals(event.getEntity().getUniqueId().toString())) event.setCancelled(true);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        dismiss(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        dismiss(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onWorld(PlayerChangedWorldEvent event) {
        dismiss(event.getPlayer().getUniqueId());
    }

    public void shutdown() {
        for (UUID owner : riding.keySet().toArray(UUID[]::new)) dismiss(owner);
    }
}
