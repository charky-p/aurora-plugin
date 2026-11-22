package org.charky.aurora.features.gauntlet;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.charky.aurora.common.menu.PagedMenu;
import org.charky.aurora.common.util.TextUtils;
import org.charky.aurora.features.combat.CombatFormulas;

import java.util.ArrayList;
import java.util.List;

// The gauntlet's forge: every piece of gear in gear.csv's order, unlocked with Shards
// once the best floor is high enough, then upgraded a level at a time with Shards and,
// every 5th level, Cores. The care package's level is bought here too
public class ForgeMenu extends PagedMenu {
    private final GauntletManager manager;
    private final GauntletRuns runs;
    private final List<GauntletData.Gear> gear;

    public ForgeMenu(GauntletManager manager, GauntletRuns runs) {
        super(manager.registry(), "The Forge");
        this.manager = manager;
        this.runs = runs;
        this.gear = List.copyOf(manager.data().gear().values());
    }

    @Override
    protected List<ItemStack> entries(Player player) {
        GauntletProfile profile = manager.profile(player);
        List<ItemStack> items = new ArrayList<>();
        for (GauntletData.Gear piece : gear) items.add(gearItem(profile, piece));
        items.add(careItem(profile));
        return items;
    }

    private ItemStack gearItem(GauntletProfile profile, GauntletData.Gear piece) {
        GauntletData data = manager.data();
        boolean owned = profile.owns(piece.id());
        int level = Math.max(0, profile.level(piece.id()));
        GauntletData.GearLevel now = data.level(level);
        ItemStack item = now == null ? new ItemStack(Material.BARRIER)
                : GauntletGear.build(manager.registry(), piece, owned ? now : data.level(0));
        if (!owned) item = item.withType(Material.GRAY_DYE);

        List<Component> lore = new ArrayList<>();
        if (!owned) {
            lore.add(TextUtils.parse("<!italic><red>Locked"));
            if (piece.unlockFloor() > 0) {
                lore.add(TextUtils.parse("<!italic>" + (profile.best >= piece.unlockFloor() ? "<green>" : "<red>")
                        + "Needs floor " + piece.unlockFloor() + " cleared"));
            }
            lore.add(TextUtils.parse("<!italic><gray>Unlock: <aqua>" + piece.unlockShards() + " Shards"));
            lore.add(TextUtils.parse("<!italic><yellow>Click to unlock"));
        } else {
            GauntletData.GearLevel next = data.level(level + 1);
            lore.add(TextUtils.parse("<!italic><gray>Level <white>" + level + "/" + data.maxLevel()));
            for (String line : gains(piece, now, next)) lore.add(TextUtils.parse("<!italic>" + line));
            if (next == null) {
                lore.add(TextUtils.parse("<!italic><green>Fully upgraded"));
            } else {
                String material = GauntletGear.material(piece, next);
                String nowMaterial = now == null ? null : GauntletGear.material(piece, now);
                if (material != null && !material.equals(nowMaterial)) {
                    lore.add(TextUtils.parse("<!italic><gold>Next level turns it " + material.toLowerCase()));
                }
                lore.add(TextUtils.parse("<!italic><gray>Next: <aqua>" + next.shards() + " Shards"
                        + (next.cores() > 0 ? " <gray>+ <light_purple>" + next.cores() + " Cores" : "")));
                lore.add(TextUtils.parse("<!italic><yellow>Click to upgrade"));
            }
        }
        ItemStack shown = item;
        shown.editMeta(meta -> {
            meta.displayName(TextUtils.parse("<!italic><white><b>" + piece.name() + (owned ? " <gray>+" + level : "")));
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            meta.lore(lore);
        });
        return shown;
    }

    // What the piece does now, and with the next level what it would do, so every
    // upgrade shows what it is worth: "Hit: 18 -> 22"
    private static List<String> gains(GauntletData.Gear piece, GauntletData.GearLevel now, GauntletData.GearLevel next) {
        if (now == null) return List.of();
        List<String> lines = new ArrayList<>();
        switch (piece.kind()) {
            case "melee" -> lines.add(change("Hit", hit(now.melee(), piece.damage()), next == null ? null : hit(next.melee(), piece.damage())));
            case "ranged" -> lines.add(change("Full shot", hit(now.ranged(), 100), next == null ? null : hit(next.ranged(), 100)));
            case "thrown" -> {
                lines.add(change("Hit", hit(now.melee(), piece.damage()), next == null ? null : hit(next.melee(), piece.damage())));
                lines.add(change("Throw", hit(now.ranged(), 100), next == null ? null : hit(next.ranged(), 100)));
            }
            case "armor" -> {
                lines.add(change("Defence", now.defence(), next == null ? null : next.defence()));
                lines.add(change("Protection", now.protection(), next == null ? null : next.protection()));
            }
            case "shield" -> {
                lines.add(change("Deflection", now.deflection(), next == null ? null : next.deflection()));
                lines.add(change("Defence", now.defence(), next == null ? null : next.defence()));
            }
            default -> {
            }
        }
        return lines;
    }

    // A hit at this much Sharpness or Power, with the weapon's own share
    private static int hit(int stat, int damagePercent) {
        return (int) Math.round((CombatFormulas.BASE_DAMAGE + CombatFormulas.sharpnessBonus(stat)) * damagePercent / 100.0);
    }

    private static String change(String name, int now, Integer next) {
        return "<gray>" + name + ": <white>" + now + (next == null || next == now ? "" : " <gray>-> <green>" + next);
    }

    private ItemStack careItem(GauntletProfile profile) {
        GauntletData data = manager.data();
        ItemStack item = new ItemStack(Material.CHEST);
        item.editMeta(meta -> {
            meta.displayName(TextUtils.parse("<!italic><gold><b>Care Package <gray>Level " + profile.care));
            List<Component> lore = new ArrayList<>();
            lore.add(TextUtils.parse("<!italic><gray>Lands every 5 floors with supplies:"));
            lore.add(TextUtils.parse("<!italic><gray>food, golden apples, potions and more."));
            lore.add(TextUtils.parse("<!italic><gray>Each level drops better loot."));
            lore.add(Component.empty());
            if (profile.care >= data.maxCareLevel()) {
                lore.add(TextUtils.parse("<!italic><green>Fully upgraded"));
            } else {
                GauntletData.CareLevel next = data.careLevel(profile.care + 1);
                lore.add(TextUtils.parse("<!italic><gray>Next: <aqua>" + next.shards() + " Shards"
                        + (next.cores() > 0 ? " <gray>+ <light_purple>" + next.cores() + " Cores" : "")));
                lore.add(TextUtils.parse("<!italic><yellow>Click to upgrade"));
            }
            meta.lore(lore);
        });
        return item;
    }

    @Override
    protected List<Component> info(Player player, List<ItemStack> entries) {
        GauntletProfile profile = manager.profile(player);
        return List.of(TextUtils.parse("<!italic><gray>Shards: <aqua>" + profile.shards),
                TextUtils.parse("<!italic><gray>Cores: <light_purple>" + profile.cores),
                TextUtils.parse("<!italic><dark_gray>Earned by running the tower"));
    }

    @Override
    protected void onEntryClick(Player player, int index) {
        if (runs.inRun(player)) {
            TextUtils.error(player, "The forge is closed during a run");
            return;
        }
        String problem;
        if (index < gear.size()) {
            GauntletData.Gear piece = gear.get(index);
            problem = manager.profile(player).owns(piece.id()) ? manager.upgrade(player, piece.id()) : manager.unlock(player, piece.id());
        } else if (index == gear.size()) {
            problem = manager.upgradeCare(player);
        } else {
            return;
        }
        if (problem != null) TextUtils.error(player, problem);
        render(player);
    }

    @Override
    protected void back(Player player) {
        manager.openMenu(player);
    }
}
