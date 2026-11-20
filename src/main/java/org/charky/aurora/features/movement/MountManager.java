package org.charky.aurora.features.movement;

import org.bukkit.Bukkit;
import org.bukkit.Input;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.AbstractHorse;
import org.bukkit.entity.Entity;
import org.bukkit.entity.HappyGhast;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Phantom;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityCombustEvent;
import org.bukkit.event.entity.EntityDamageEvent;
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
// getting off (shift) makes it vanish. The horse, skeleton horse, camel and happy ghast
// steer the vanilla way; the pig, the chicken and the phantom cannot, so they go where
// you look and the keys you press. Every mount has three levels in the shop, each one
// faster. A mount cannot be hurt, and only its owner can ride it
public class MountManager implements Listener {
    private static final String ITEM_PREFIX = "mount_";
    private static final double JUMP = 0.45;
    // The phantom rises and sinks this fast with jump held, or flying backwards
    private static final double PHANTOM_CLIMB = 0.4;

    // A mount out under its owner, at the level they bought
    private record Ride(Entity entity, Mount mount, int level) {
    }

    private final Plugin plugin;
    private final DataRegistry registry;
    private final PerkManager perks;
    private final NamespacedKey ownerKey;
    // owner -> the mount they are riding
    private final Map<UUID, Ride> riding = new HashMap<>();

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
        Perk perk = Perk.byId(id);
        if (perk == null || !perks.owns(player, perk)) {
            TextUtils.error(player, "This saddle is not yours");
            return;
        }
        Mount mount = Mount.byId(id.substring(ITEM_PREFIX.length()));
        if (player.isInsideVehicle() || mount == null) return;
        summon(player, mount, Math.min(perks.level(player, perk), perk.maxLevel()));
    }

    private static void attribute(Entity entity, Attribute attribute, double value) {
        if (!(entity instanceof LivingEntity living)) return;
        AttributeInstance instance = living.getAttribute(attribute);
        if (instance != null) instance.setBaseValue(value);
    }

    private void summon(Player player, Mount mount, int level) {
        UUID uuid = player.getUniqueId();
        dismiss(uuid);
        Location at = player.getLocation();

        Entity entity = at.getWorld().spawnEntity(at, mount.type());
        entity.setPersistent(false);
        entity.setInvulnerable(true);
        entity.getPersistentDataContainer().set(ownerKey, PersistentDataType.STRING, uuid.toString());
        if (entity instanceof LivingEntity living) living.setRemoveWhenFarAway(false);

        if (entity instanceof AbstractHorse horse) {
            horse.setAdult();
            horse.setTamed(true);
            horse.setOwner(player);
            horse.getInventory().setSaddle(new ItemStack(Material.SADDLE));
        }
        if (entity instanceof HappyGhast ghast) ghast.getEquipment().setItem(EquipmentSlot.BODY, new ItemStack(Material.WHITE_HARNESS));
        if (entity instanceof Phantom phantom) {
            phantom.setAware(false);
            phantom.setShouldBurnInDay(false);
        }

        switch (mount.steering()) {
            // No wandering of its own, it only goes where the rider steers it
            case WALK -> {
                if (entity instanceof Mob mob) Bukkit.getMobGoals().removeAllGoals(mob);
            }
            case FLY -> entity.setGravity(false);
            case VANILLA -> {
                attribute(entity, Attribute.MOVEMENT_SPEED, mount.speed(level));
                if (mount.hasJump()) attribute(entity, Attribute.JUMP_STRENGTH, mount.jump(level));
            }
            case VANILLA_FLYING -> attribute(entity, Attribute.FLYING_SPEED, mount.speed(level));
        }

        entity.addPassenger(player);
        riding.put(uuid, new Ride(entity, mount, level));
        player.playSound(at, Sound.ENTITY_HORSE_SADDLE, 1f, 1.2f);
    }

    private void dismiss(UUID owner) {
        Ride ride = riding.remove(owner);
        if (ride != null && ride.entity().isValid()) {
            ride.entity().eject();
            ride.entity().remove();
        }
    }

    // Each tick: the pig and the chicken walk where their rider looks and presses, the
    // phantom flies there. The others are vanilla steered and left alone
    private void steer() {
        for (Ride ride : riding.values()) {
            Mount.Steering steering = ride.mount().steering();
            if (steering != Mount.Steering.WALK && steering != Mount.Steering.FLY) continue;
            Entity mount = ride.entity();
            if (!mount.isValid() || mount.getPassengers().isEmpty()) continue;
            if (!(mount.getPassengers().getFirst() instanceof Player rider)) continue;

            Input input = rider.getCurrentInput();
            Location eye = rider.getLocation();
            double speed = ride.mount().speed(ride.level());
            if (steering == Mount.Steering.FLY) fly(mount, input, eye, speed);
            else walk(mount, input, eye.getYaw(), speed);
        }
    }

    private static void walk(Entity mount, Input input, float yaw, double speed) {
        Vector forward = new Vector(-Math.sin(Math.toRadians(yaw)), 0, Math.cos(Math.toRadians(yaw)));
        Vector side = new Vector(forward.getZ(), 0, -forward.getX());
        Vector move = new Vector();
        if (input.isForward()) move.add(forward);
        if (input.isBackward()) move.subtract(forward);
        if (input.isLeft()) move.add(side);
        if (input.isRight()) move.subtract(side);
        if (move.lengthSquared() > 0) move.normalize().multiply(speed);

        double y = mount.getVelocity().getY();
        if (input.isJump() && mount.isOnGround()) y = JUMP;
        mount.setVelocity(move.setY(y));
        mount.setRotation(yaw, 0);
    }

    // Forward flies the way the rider looks, up and down included. Nothing held hovers,
    // jump climbs straight up
    private static void fly(Entity mount, Input input, Location eye, double speed) {
        Vector move = new Vector();
        if (input.isForward()) move.add(eye.getDirection().multiply(speed));
        if (input.isBackward()) move.add(eye.getDirection().multiply(-speed / 2));
        if (input.isJump()) move.setY(Math.max(move.getY(), PHANTOM_CLIMB));
        mount.setVelocity(move);
        mount.setRotation(eye.getYaw(), eye.getPitch());
    }

    // Getting off, shift, sends the mount away. Vanilla also throws a rider off in deep
    // water; the mounts that swim keep their rider unless they shift
    @EventHandler(ignoreCancelled = true)
    public void onDismount(EntityDismountEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        UUID uuid = player.getUniqueId();
        Ride ride = riding.get(uuid);
        if (ride == null || !ride.entity().equals(event.getDismounted())) return;
        if (ride.mount().swims(ride.level()) && !player.isSneaking() && ride.entity().isInWater() && event.isCancellable()) {
            event.setCancelled(true);
            return;
        }
        Bukkit.getScheduler().runTask(plugin, () -> dismiss(uuid));
    }

    // Nobody but the owner gets on
    @EventHandler(ignoreCancelled = true)
    public void onMount(EntityMountEvent event) {
        String owner = event.getMount().getPersistentDataContainer().get(ownerKey, PersistentDataType.STRING);
        if (owner != null && !owner.equals(event.getEntity().getUniqueId().toString())) event.setCancelled(true);
    }

    // Riding a mount never hurts: a horse or camel landing from a jump passes its fall
    // to the rider in vanilla
    @EventHandler(ignoreCancelled = true)
    public void onFall(EntityDamageEvent event) {
        if (event.getCause() != EntityDamageEvent.DamageCause.FALL || !(event.getEntity() instanceof Player player)) return;
        Ride ride = riding.get(player.getUniqueId());
        if (ride != null && ride.entity().equals(player.getVehicle())) event.setCancelled(true);
    }

    // A mount never catches fire, the phantom in daylight included
    @EventHandler(ignoreCancelled = true)
    public void onCombust(EntityCombustEvent event) {
        if (event.getEntity().getPersistentDataContainer().has(ownerKey, PersistentDataType.STRING)) event.setCancelled(true);
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
