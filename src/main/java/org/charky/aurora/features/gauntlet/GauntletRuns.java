package org.charky.aurora.features.gauntlet;

import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;
import org.bukkit.scheduler.BukkitTask;
import org.charky.aurora.common.util.DataKeys;
import org.charky.aurora.common.util.TextUtils;
import org.charky.aurora.core.Aurora;
import org.charky.aurora.features.combat.CombatHooks;
import org.charky.aurora.features.spawner.SpawnerManager;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

// A run up the tower, solo, in the player's own copy of the arena. Every floor is a
// wave of mobs from floors.csv, made tougher by the floor; every kill pays Shards and
// clearing the floor a bonus (and Cores on a boss floor), banked when the run ends
// however it ends. Every 5th
// floor a care package lands and the player picks one of three boons, upgrades for
// this run only. A hit that would kill ends the run instead (a totem still saves),
// and the player is back in the lobby with their gear, keeping what they earned
public class GauntletRuns implements Listener {
    public enum End { DIED, LEFT, QUIT }

    private static final int BOON_EVERY = 5;
    private static final int BOON_CHOICES = 3;
    private static final long FLOOR_DELAY_TICKS = 60L;
    // A mob this far from the arena's spawn has fallen off or wandered, it is brought back
    private static final double STRAY_DISTANCE = 80;
    private static final NamespacedKey HEALTH_BOON = new NamespacedKey("aurora", "gauntlet_health");
    private static final NamespacedKey SPEED_BOON = new NamespacedKey("aurora", "gauntlet_speed");
    private static final NamespacedKey ABSORB_BOON = new NamespacedKey("aurora", "gauntlet_absorb");
    // Vanilla Strength does nothing to Aurora hits, in a run each level adds this much
    private static final double STRENGTH_PER_LEVEL = 0.3;

    private final Aurora plugin;
    private final GauntletManager manager;
    private final SpawnerManager spawners;
    private final Map<UUID, Run> runs = new HashMap<>();
    private BukkitTask task;

    private enum State { STARTING, FIGHTING, CHOOSING }

    private static final class Run {
        final UUID player;
        final int slot;
        int floor;
        int shards;
        int cores;
        State state = State.STARTING;
        final Set<UUID> mobs = new HashSet<>();
        final Map<String, Integer> boons = new LinkedHashMap<>();
        List<String> offered = List.of();

        Run(UUID player, int slot) {
            this.player = player;
            this.slot = slot;
        }

        int stacks(String boon) {
            return boons.getOrDefault(boon, 0);
        }

        int cleared() {
            return state == State.FIGHTING || state == State.STARTING ? floor - 1 : floor;
        }
    }

    public GauntletRuns(Aurora plugin, GauntletManager manager, SpawnerManager spawners) {
        this.plugin = plugin;
        this.manager = manager;
        this.spawners = spawners;
        CombatHooks.set(this::outgoing, this::incoming);
    }

    public void start() {
        task = plugin.getServer().getScheduler().runTaskTimer(plugin, this::tick, 20L, 20L);
    }

    public void shutdown() {
        if (task != null) task.cancel();
        for (Player player : plugin.getServer().getOnlinePlayers()) end(player, End.LEFT);
    }

    public boolean inRun(Player player) {
        return runs.containsKey(player.getUniqueId());
    }

    // For the sidebar, 0 outside a run
    int floorOf(Player player) {
        Run run = runs.get(player.getUniqueId());
        return run == null ? 0 : run.floor;
    }

    int shardsOf(Player player) {
        Run run = runs.get(player.getUniqueId());
        return run == null ? 0 : run.shards;
    }

    int coresOf(Player player) {
        Run run = runs.get(player.getUniqueId());
        return run == null ? 0 : run.cores;
    }

    // ---- Starting and ending ----

    // Null if it started, otherwise why not
    public String begin(Player player) {
        if (inRun(player)) return "You are already in a run";
        if (!manager.isHere(player)) return "Go to the Gauntlet first, /gauntlet";
        if (!manager.data().ready()) return "The Gauntlet's data did not load";
        GauntletArena arena = manager.arena();
        String missing = arena.missing();
        if (missing != null) return "The arena is not set up yet (" + missing + ")";

        int slot = arena.claimSlot();
        Run run = new Run(player.getUniqueId(), slot);
        runs.put(player.getUniqueId(), run);
        arena.paste(slot);

        player.closeInventory();
        manager.giveKit(player);
        heal(player);
        player.teleport(arena.inSlot(slot, arena.place(GauntletArena.SPAWN)));
        player.playSound(player.getLocation(), Sound.EVENT_RAID_HORN, 0.6f, 1f);
        nextFloor(player, run);
        return null;
    }

    // Banks what the run earned and puts the player back in the lobby with their gear
    public void end(Player player, End why) {
        Run run = runs.remove(player.getUniqueId());
        if (run == null) return;
        for (UUID id : run.mobs) {
            Entity mob = Bukkit.getEntity(id);
            if (mob != null) mob.remove();
        }
        manager.arena().freeSlot(run.slot);
        clearBoons(player);
        takeRunItems(player);

        int cleared = run.cleared();
        manager.bank(player, run.shards, run.cores, cleared);
        if (why == End.QUIT) return;

        heal(player);
        if (manager.isHere(player)) {
            Location lobby = manager.arena().lobby();
            if (lobby != null) player.teleport(lobby);
            manager.giveKit(player);
        }
        String how = why == End.DIED ? "<red>You fell on floor " + run.floor : "<gray>You left the run on floor " + run.floor;
        player.showTitle(Title.title(TextUtils.parse(why == End.DIED ? "<red><b>DEFEATED" : "<gray><b>RUN OVER"),
                TextUtils.parse("<gray>Floors cleared: <white>" + cleared)));
        TextUtils.send(player, how + "<gray>. Cleared <white>" + cleared + "</white>, earned <aqua>" + run.shards
                + " Shards" + (run.cores > 0 ? " <gray>and <light_purple>" + run.cores + " Cores" : "")
                + "<gray>. Best: <white>" + manager.profile(player).best);
    }

    private void heal(Player player) {
        AttributeInstance max = player.getAttribute(Attribute.MAX_HEALTH);
        player.setHealth(max == null ? 20 : max.getValue());
        player.setFoodLevel(20);
        player.setSaturation(10);
        player.setFireTicks(0);
    }

    // ---- Floors ----

    private void nextFloor(Player player, Run run) {
        run.floor++;
        run.state = State.STARTING;
        GauntletData.Floor floor = manager.data().floor(run.floor);
        boolean boss = floor != null && floor.boss();
        player.showTitle(Title.title(TextUtils.parse((boss ? "<dark_red><b>" : "<red><b>") + "FLOOR " + run.floor),
                TextUtils.parse(boss ? "<gold>A boss awaits" : "<gray>Get ready"),
                Title.Times.times(Duration.ofMillis(200), Duration.ofMillis(1600), Duration.ofMillis(400))));
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (runs.get(player.getUniqueId()) == run && player.isOnline()) spawnFloor(player, run);
        }, FLOOR_DELAY_TICKS);
    }

    private void spawnFloor(Player player, Run run) {
        GauntletData.Floor floor = manager.data().floor(run.floor);
        GauntletArena arena = manager.arena();
        List<Location> spawns = new ArrayList<>(arena.mobSpawns().stream().map(spawn -> arena.inSlot(run.slot, spawn)).toList());
        Collections.shuffle(spawns);
        int next = 0;
        for (Map.Entry<String, Integer> entry : floor.mobs().entrySet()) {
            String id = GauntletData.floorMobId(entry.getKey(), run.floor);
            for (int i = 0; i < entry.getValue(); i++) {
                Location at = spawns.get(next++ % spawns.size()).clone()
                        .add(ThreadLocalRandom.current().nextDouble(-1.5, 1.5), 0, ThreadLocalRandom.current().nextDouble(-1.5, 1.5));
                LivingEntity mob = spawners.spawnGauntletMob(id, at);
                if (mob == null) continue;
                run.mobs.add(mob.getUniqueId());
                if (mob instanceof Mob hunter) hunter.setTarget(player);
            }
        }
        // Bulwark: absorption hearts at the start of every floor
        int absorb = (int) Math.round(boonTotal(run, "absorb"));
        if (absorb > 0) {
            setModifier(player, Attribute.MAX_ABSORPTION, ABSORB_BOON, absorb, AttributeModifier.Operation.ADD_NUMBER);
            player.setAbsorptionAmount(absorb);
        }
        run.state = State.FIGHTING;
    }

    // Every second: mobs that are gone count as dead, strays are brought back, the
    // floor ends when none are left, and the action bar shows how it is going
    private void tick() {
        for (Run run : new ArrayList<>(runs.values())) {
            Player player = Bukkit.getPlayer(run.player);
            if (player == null) continue;
            if (run.state == State.FIGHTING) {
                Location spawn = manager.arena().inSlot(run.slot, manager.arena().place(GauntletArena.SPAWN));
                run.mobs.removeIf(id -> {
                    Entity mob = Bukkit.getEntity(id);
                    if (mob == null || !mob.isValid() || mob.isDead()) return true;
                    if (spawn != null && (!mob.getWorld().equals(spawn.getWorld()) || mob.getLocation().distance(spawn) > STRAY_DISTANCE)) {
                        mob.teleport(spawn);
                    }
                    if (mob instanceof Mob hunter && hunter.getTarget() == null) hunter.setTarget(player);
                    return false;
                });
                if (run.mobs.isEmpty()) cleared(player, run);
                // The player fell out of their arena
                else if (spawn != null && !manager.arena().isInSlot(run.slot, player.getLocation())
                        && player.getLocation().getY() < spawn.getY() - 30) {
                    player.teleport(spawn);
                }
            }
            String status = switch (run.state) {
                case FIGHTING -> "<red>Floor " + run.floor + " <dark_gray>| <white>" + run.mobs.size() + " left";
                case CHOOSING -> "<gold>Pick a boon <dark_gray>| <yellow><click:run_command:'/gauntlet boon'>/gauntlet boon</click>";
                case STARTING -> "<red>Floor " + run.floor;
            };
            manager.refreshBoard(player);
            player.sendActionBar(TextUtils.parse(status + " <dark_gray>| <aqua>" + run.shards + " Shards"
                    + (run.cores > 0 ? " <dark_gray>| <light_purple>" + run.cores + " Cores" : "")));
        }
    }

    private void cleared(Player player, Run run) {
        GauntletData.Floor floor = manager.data().floor(run.floor);
        run.shards += floor.shards();
        run.cores += floor.cores();
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.7f, 1.3f);
        TextUtils.send(player, "<green>Floor " + run.floor + " cleared <aqua>+" + floor.shards() + " bonus Shards"
                + (floor.cores() > 0 ? " <light_purple>+" + floor.cores() + " Core" + (floor.cores() > 1 ? "s" : "") : ""));

        if (run.floor % BOON_EVERY == 0) {
            carePackage(player, run);
            offerBoons(player, run);
        } else {
            nextFloor(player, run);
        }
    }

    // A hit that would kill ends the run, unless a totem in hand saves them
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onLethal(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player) || !inRun(player)) return;
        if (event.getFinalDamage() < player.getHealth() + player.getAbsorptionAmount()) return;
        PlayerInventory inventory = player.getInventory();
        if (inventory.getItemInMainHand().getType() == Material.TOTEM_OF_UNDYING
                || inventory.getItemInOffHand().getType() == Material.TOTEM_OF_UNDYING) return;
        event.setCancelled(true);
        player.playSound(player.getLocation(), Sound.ENTITY_WITHER_DEATH, 0.5f, 1.2f);
        end(player, End.DIED);
    }

    // Every kill pays Shards at once, kept even if the run ends before the floor is cleared
    @EventHandler
    public void onMobDeath(EntityDeathEvent event) {
        UUID id = event.getEntity().getUniqueId();
        for (Run run : runs.values()) {
            if (!run.mobs.remove(id)) continue;
            Player player = Bukkit.getPlayer(run.player);
            String mobId = event.getEntity().getPersistentDataContainer().get(DataKeys.MOB_KEY, PersistentDataType.STRING);
            GauntletData.Floor floor = manager.data().floor(run.floor);
            if (mobId != null && floor != null) {
                int pay = floor.killPay(GauntletData.baseMobId(mobId));
                run.shards += pay;
                if (player != null) {
                    player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.4f, 1.6f);
                    if (pay > floor.kill()) TextUtils.send(player, "<gold>Boss slain <aqua>+" + pay + " Shards");
                }
            }
            // Bloodthirst: half a heart a kill a stack
            double heal = boonTotal(run, "killheal");
            if (player != null && heal > 0) {
                AttributeInstance max = player.getAttribute(Attribute.MAX_HEALTH);
                player.setHealth(Math.min(max == null ? 20 : max.getValue(), player.getHealth() + heal));
            }
            return;
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        end(event.getPlayer(), End.QUIT);
    }

    // ---- Care packages ----

    // A chest by the arena's package spot (or the player) with the care package's loot.
    // All of it is taken back when the run ends
    private void carePackage(Player player, Run run) {
        GauntletArena arena = manager.arena();
        Location at = arena.place(GauntletArena.PACKAGE);
        at = at == null ? player.getLocation() : arena.inSlot(run.slot, at);
        Block block = at.getBlock();
        block.setType(Material.CHEST);
        if (!(block.getState() instanceof Chest chest)) return;

        GauntletData.CareLevel care = manager.data().careLevel(manager.profile(player).care);
        if (care != null) {
            for (GauntletData.Loot loot : care.loot()) {
                if (ThreadLocalRandom.current().nextDouble(100) >= loot.chance()) continue;
                ItemStack item = lootItem(loot.item(), ThreadLocalRandom.current().nextInt(loot.min(), loot.max() + 1));
                if (item == null) continue;
                GauntletGear.markRunItem(item);
                chest.getBlockInventory().addItem(item);
            }
        }
        block.getWorld().spawnParticle(Particle.FIREWORK, block.getLocation().add(0.5, 1, 0.5), 40, 0.4, 0.6, 0.4, 0.05);
        player.playSound(block.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_BLAST, 1f, 1f);
        TextUtils.send(player, "<gold><b>CARE PACKAGE</b> <gray>A chest has landed in the arena (level " + manager.profile(player).care + ")");
    }

    // A material, or POTION@TYPE / SPLASH_POTION@TYPE. Null for anything unknown
    static ItemStack lootItem(String id, int amount) {
        String[] parts = id.split("@", 2);
        Material material = Material.matchMaterial(parts[0]);
        if (material == null || !material.isItem() || amount <= 0) return null;
        ItemStack item = new ItemStack(material, amount);
        if (parts.length == 2 && item.getItemMeta() instanceof PotionMeta) {
            PotionType type;
            try {
                type = PotionType.valueOf(parts[1].toUpperCase());
            } catch (IllegalArgumentException e) {
                return null;
            }
            item.editMeta(PotionMeta.class, meta -> meta.setBasePotionType(type));
        }
        return item;
    }

    private void takeRunItems(Player player) {
        PlayerInventory inventory = player.getInventory();
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            if (GauntletGear.isRunItem(inventory.getItem(slot))) inventory.setItem(slot, null);
        }
        ItemStack cursor = player.getItemOnCursor();
        if (GauntletGear.isRunItem(cursor)) player.setItemOnCursor(null);
    }

    // ---- Boons ----

    private void offerBoons(Player player, Run run) {
        List<String> open = new ArrayList<>();
        for (GauntletData.Boon boon : manager.data().boons().values()) {
            if (run.stacks(boon.id()) < boon.max()) open.add(boon.id());
        }
        Collections.shuffle(open);
        run.offered = List.copyOf(open.subList(0, Math.min(BOON_CHOICES, open.size())));
        if (run.offered.isEmpty()) {
            nextFloor(player, run);
            return;
        }
        run.state = State.CHOOSING;
        TextUtils.send(player, "<gold><b>BOON</b> <gray>Pick an upgrade for this run. Loot the care package first if you like: "
                + "<yellow><click:run_command:'/gauntlet boon'>[Choose]</click>");
        new BoonMenu(manager.registry(), this, player).open(player);
    }

    // For /gauntlet boon. Null if the menu opened, otherwise why not
    public String openBoons(Player player) {
        Run run = runs.get(player.getUniqueId());
        if (run == null || run.state != State.CHOOSING) return "There is no boon to pick right now";
        new BoonMenu(manager.registry(), this, player).open(player);
        return null;
    }

    List<GauntletData.Boon> offered(Player player) {
        Run run = runs.get(player.getUniqueId());
        if (run == null || run.state != State.CHOOSING) return List.of();
        return run.offered.stream().map(id -> manager.data().boons().get(id)).filter(Objects::nonNull).toList();
    }

    int stacks(Player player, String boon) {
        Run run = runs.get(player.getUniqueId());
        return run == null ? 0 : run.stacks(boon);
    }

    void choose(Player player, String boonId) {
        Run run = runs.get(player.getUniqueId());
        if (run == null || run.state != State.CHOOSING || !run.offered.contains(boonId)) return;
        GauntletData.Boon boon = manager.data().boons().get(boonId);
        run.boons.merge(boonId, 1, Integer::sum);
        run.offered = List.of();
        applyBoons(player, run);
        player.closeInventory();
        player.playSound(player.getLocation(), Sound.BLOCK_BEACON_POWER_SELECT, 0.8f, 1.4f);
        TextUtils.send(player, "<gold>" + boon.name() + " <gray>" + boon.description() + " <dark_gray>(x" + run.stacks(boonId) + ")");
        nextFloor(player, run);
    }

    // Closing the menu without picking leaves the choice waiting, /gauntlet boon reopens it
    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (!(event.getInventory().getHolder(false) instanceof BoonMenu)) return;
        if (event.getPlayer() instanceof Player player && !offered(player).isEmpty()) {
            TextUtils.send(player, "<gray>Your boon is waiting: <yellow><click:run_command:'/gauntlet boon'>[Choose]</click>");
        }
    }

    // A boon's stat added up over its stacks
    private double boonTotal(Run run, String stat) {
        double total = 0;
        for (Map.Entry<String, Integer> entry : run.boons.entrySet()) {
            GauntletData.Boon boon = manager.data().boons().get(entry.getKey());
            if (boon != null && boon.stat().equals(stat)) total += boon.amount() * entry.getValue();
        }
        return total;
    }

    private void applyBoons(Player player, Run run) {
        setModifier(player, Attribute.MAX_HEALTH, HEALTH_BOON, boonTotal(run, "health"), AttributeModifier.Operation.ADD_NUMBER);
        setModifier(player, Attribute.MOVEMENT_SPEED, SPEED_BOON, boonTotal(run, "speed") / 100, AttributeModifier.Operation.ADD_SCALAR);
        int regen = (int) Math.round(boonTotal(run, "regen"));
        player.removePotionEffect(PotionEffectType.REGENERATION);
        if (regen > 0) player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, PotionEffect.INFINITE_DURATION, regen - 1, true, false));
    }

    private void clearBoons(Player player) {
        setModifier(player, Attribute.MAX_HEALTH, HEALTH_BOON, 0, AttributeModifier.Operation.ADD_NUMBER);
        setModifier(player, Attribute.MOVEMENT_SPEED, SPEED_BOON, 0, AttributeModifier.Operation.ADD_SCALAR);
        setModifier(player, Attribute.MAX_ABSORPTION, ABSORB_BOON, 0, AttributeModifier.Operation.ADD_NUMBER);
        player.setAbsorptionAmount(0);
        for (PotionEffect effect : player.getActivePotionEffects()) player.removePotionEffect(effect.getType());
    }

    private static void setModifier(Player player, Attribute attribute, NamespacedKey key, double amount,
                                    AttributeModifier.Operation operation) {
        AttributeInstance instance = player.getAttribute(attribute);
        if (instance == null) return;
        instance.removeModifier(key);
        if (amount != 0) instance.addModifier(new AttributeModifier(key, amount, operation, EquipmentSlotGroup.ANY));
    }

    // ---- Combat ----

    // Might and Keen Eye, the weapon's own damage, and Strength potions, in a run only
    private double outgoing(Player attacker, boolean ranged) {
        Run run = runs.get(attacker.getUniqueId());
        if (run == null) return 1.0;
        double multiplier = 1 + boonTotal(run, "damage") / 100;
        if (ThreadLocalRandom.current().nextDouble(100) < boonTotal(run, "crit")) multiplier *= 2;
        PotionEffect strength = attacker.getPotionEffect(PotionEffectType.STRENGTH);
        if (strength != null) multiplier *= 1 + STRENGTH_PER_LEVEL * (strength.getAmplifier() + 1);
        if (!ranged) {
            GauntletData.Gear gear = manager.data().gear().get(GauntletGear.gearId(attacker.getInventory().getItemInMainHand()));
            if (gear != null) multiplier *= gear.damage() / 100.0;
        }
        return multiplier;
    }

    // Evasion and Iron Skin
    private double incoming(Player victim, double damage) {
        Run run = runs.get(victim.getUniqueId());
        if (run == null) return damage;
        if (ThreadLocalRandom.current().nextDouble(100) < boonTotal(run, "dodge")) {
            victim.playSound(victim.getLocation(), Sound.ENTITY_BREEZE_JUMP, 0.6f, 1.6f);
            return 0;
        }
        return damage * (1 - Math.min(0.8, boonTotal(run, "guard") / 100));
    }
}
