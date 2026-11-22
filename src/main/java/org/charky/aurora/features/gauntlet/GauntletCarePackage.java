package org.charky.aurora.features.gauntlet;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionType;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

// Care packages: a chest by the arena's package spot with the loot of care_package.csv,
// at the best care level in the party, every item a little likelier with Scavenger.
// Gone once emptied and closed; all of it is taken back when the run ends
final class GauntletCarePackage {
    private final GauntletManager manager;
    private final GauntletBonuses bonuses;

    GauntletCarePackage(GauntletManager manager, GauntletBonuses bonuses) {
        this.manager = manager;
        this.bonuses = bonuses;
    }

    void drop(GauntletRun run) {
        GauntletArena arena = manager.arena();
        Location at = arena.place(GauntletArena.PACKAGE);
        List<Player> online = run.online();
        if (online.isEmpty()) return;
        at = at == null ? online.getFirst().getLocation() : arena.inSlot(run.slot, at);
        Block block = at.getBlock();
        block.setType(Material.CHEST);
        if (!(block.getState() instanceof Chest chest)) return;

        int level = 1;
        double scavenger = 0;
        for (Player player : online) {
            GauntletProfile profile = manager.profile(player);
            level = Math.max(level, profile.care);
            scavenger = Math.max(scavenger, bonuses.talentTotal(profile, "care"));
        }
        GauntletData.CareLevel care = manager.data().careLevel(level);
        if (care != null) {
            for (GauntletData.Loot loot : care.loot()) {
                if (ThreadLocalRandom.current().nextDouble(100) >= loot.chance() + scavenger) continue;
                ItemStack item = lootItem(loot.item(), ThreadLocalRandom.current().nextInt(loot.min(), loot.max() + 1));
                if (item == null) continue;
                GauntletGear.markRunItem(item);
                chest.getBlockInventory().addItem(item);
            }
        }
        block.getWorld().spawnParticle(Particle.FIREWORK, block.getLocation().add(0.5, 1, 0.5), 40, 0.4, 0.6, 0.4, 0.05);
        block.getWorld().playSound(block.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_BLAST, 1f, 1f);
        run.tell("<gold><b>CARE PACKAGE</b> <gray>A chest has landed in the arena (level " + level + ")");
    }

    // A chest closed empty by a player of the run: gone, if it is in their arena
    void closedEmpty(GauntletRun run, Chest chest) {
        Block block = chest.getBlock();
        if (!manager.arena().isInSlot(run.slot, block.getLocation())) return;
        block.setType(Material.AIR);
        block.getWorld().spawnParticle(Particle.POOF, block.getLocation().add(0.5, 0.5, 0.5), 15, 0.3, 0.3, 0.3, 0.02);
        block.getWorld().playSound(block.getLocation(), Sound.BLOCK_WOOL_BREAK, 0.8f, 1f);
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

    // Everything a run handed out, back off the player
    static void takeRunItems(Player player) {
        PlayerInventory inventory = player.getInventory();
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            if (GauntletGear.isRunItem(inventory.getItem(slot))) inventory.setItem(slot, null);
        }
        ItemStack cursor = player.getItemOnCursor();
        if (GauntletGear.isRunItem(cursor)) player.setItemOnCursor(null);
    }
}
