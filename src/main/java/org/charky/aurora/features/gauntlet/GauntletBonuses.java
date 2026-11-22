package org.charky.aurora.features.gauntlet;

import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.charky.aurora.features.gauntlet.GauntletRun.Member;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

// What a player's boons, held gear's perks, armour set and talents add up to in a run,
// the attributes and effects that carry them, and the draw of boons to pick from
final class GauntletBonuses {
    private static final NamespacedKey HEALTH_BOON = new NamespacedKey("aurora", "gauntlet_health");
    private static final NamespacedKey SPEED_BOON = new NamespacedKey("aurora", "gauntlet_speed");
    private static final NamespacedKey ABSORB_BOON = new NamespacedKey("aurora", "gauntlet_absorb");
    private static final NamespacedKey TALENT_HEALTH = new NamespacedKey("aurora", "gauntlet_talent_health");

    private final GauntletManager manager;

    GauntletBonuses(GauntletManager manager) {
        this.manager = manager;
    }

    private GauntletData data() {
        return manager.data();
    }

    // A boon's stat added up over its stacks
    double boonTotal(Member member, String stat) {
        if (member == null) return 0;
        double total = 0;
        for (Map.Entry<String, Integer> entry : member.boons.entrySet()) {
            GauntletData.Boon boon = data().boons().get(entry.getKey());
            if (boon != null && boon.stat().equals(stat)) total += boon.amount() * entry.getValue();
        }
        return total;
    }

    // A perk stat of the gear held (or worn as a set), for the levels the player has
    double perkTotal(Player player, String stat) {
        GauntletProfile profile = manager.profile(player);
        String held = GauntletGear.gearId(player.getInventory().getItemInMainHand());
        double total = 0;
        if (held != null) {
            for (GauntletData.Perk perk : data().perksOf(held)) {
                if (perk.stat().equals(stat) && profile.level(held) >= perk.level()) total += perk.amount();
            }
        }
        return total + setTotal(profile, stat);
    }

    // An armour set stat: every set perk whose level all four pieces have reached
    double setTotal(GauntletProfile profile, String stat) {
        int lowest = armorSetLevel(profile);
        double total = 0;
        for (GauntletData.Perk perk : data().perksOf(GauntletData.ARMOR_SET)) {
            if (perk.stat().equals(stat) && lowest >= perk.level()) total += perk.amount();
        }
        return total;
    }

    // The lowest level of the armour pieces, 0 while one is missing
    int armorSetLevel(GauntletProfile profile) {
        int lowest = Integer.MAX_VALUE;
        boolean any = false;
        for (GauntletData.Gear gear : data().gear().values()) {
            if (!gear.armor()) continue;
            any = true;
            lowest = Math.min(lowest, Math.max(0, profile.level(gear.id())));
        }
        return any ? lowest : 0;
    }

    // A talent's stat added up over its levels
    double talentTotal(GauntletProfile profile, String stat) {
        double total = 0;
        for (GauntletData.Talent talent : data().talents().values()) {
            if (talent.stat().equals(stat)) total += talent.amount() * profile.talent(talent.id());
        }
        return total;
    }

    // Boons not taken to their max, picked by the weight of their rarity, none twice, of
    // one rarity only when one is given
    List<String> pickBoons(Member member, int count, String rarity) {
        List<GauntletData.Boon> open = new ArrayList<>();
        for (GauntletData.Boon boon : data().boons().values()) {
            if (member.stacks(boon.id()) < boon.max() && (rarity == null || boon.rarity().equals(rarity))) open.add(boon);
        }
        List<String> picked = new ArrayList<>();
        while (picked.size() < count && !open.isEmpty()) {
            double total = 0;
            for (GauntletData.Boon boon : open) total += rarityWeight(boon);
            double roll = ThreadLocalRandom.current().nextDouble(total);
            GauntletData.Boon chosen = open.getLast();
            for (GauntletData.Boon boon : open) {
                roll -= rarityWeight(boon);
                if (roll < 0) {
                    chosen = boon;
                    break;
                }
            }
            picked.add(chosen.id());
            open.remove(chosen);
        }
        return picked;
    }

    private double rarityWeight(GauntletData.Boon boon) {
        return Math.max(0.01, data().setting("rarity_" + boon.rarity(), boon.rarity().equals("common") ? 60 : 30));
    }

    // Vitality and the Copper Set's hearts, for the whole run
    void runHealth(Player player, GauntletProfile profile) {
        setModifier(player, Attribute.MAX_HEALTH, TALENT_HEALTH, talentTotal(profile, "health") + setTotal(profile, "health"),
                AttributeModifier.Operation.ADD_NUMBER);
    }

    // Bulwark and the Netherite Set: absorption hearts at the start of every floor
    void floorAbsorption(Player player, Member member) {
        int absorb = (int) Math.round(boonTotal(member, "absorb") + setTotal(manager.profile(player), "absorb"));
        if (absorb <= 0) return;
        setModifier(player, Attribute.MAX_ABSORPTION, ABSORB_BOON, absorb, AttributeModifier.Operation.ADD_NUMBER);
        player.setAbsorptionAmount(absorb);
    }

    // Vitality boons and the altars' hearts, Swiftness and Mending
    void applyBoons(Player player, Member member) {
        setModifier(player, Attribute.MAX_HEALTH, HEALTH_BOON, boonTotal(member, "health") + member.altarHealth, AttributeModifier.Operation.ADD_NUMBER);
        setModifier(player, Attribute.MOVEMENT_SPEED, SPEED_BOON, boonTotal(member, "speed") / 100, AttributeModifier.Operation.ADD_SCALAR);
        int regen = (int) Math.round(boonTotal(member, "regen"));
        player.removePotionEffect(PotionEffectType.REGENERATION);
        if (regen > 0) player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, PotionEffect.INFINITE_DURATION, regen - 1, true, false));
    }

    // Everything a run gave, gone when it ends
    void clearBoons(Player player) {
        setModifier(player, Attribute.MAX_HEALTH, HEALTH_BOON, 0, AttributeModifier.Operation.ADD_NUMBER);
        setModifier(player, Attribute.MAX_HEALTH, TALENT_HEALTH, 0, AttributeModifier.Operation.ADD_NUMBER);
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
}
