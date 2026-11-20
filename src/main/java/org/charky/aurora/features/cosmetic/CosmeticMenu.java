package org.charky.aurora.features.cosmetic;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ArmorMeta;
import org.bukkit.inventory.meta.trim.ArmorTrim;
import org.charky.aurora.features.tool.UpgradeZone;
import org.charky.aurora.common.menu.PagedMenu;
import org.charky.aurora.common.util.TextUtils;
import org.charky.aurora.core.DataRegistry;
import org.charky.aurora.features.cosmetic.Cosmetic.Slot;
import org.charky.aurora.features.cosmetic.Cosmetic.Type;
import org.charky.aurora.features.rank.RankManager;

import java.util.ArrayList;
import java.util.List;

// Every cosmetic of one kind. Click one you own to wear it, or take it off again.
// One you can buy is bought with the click. Suffixes go in the first place with a
// left click and the second with a right click
public class CosmeticMenu extends PagedMenu {
    private final CosmeticManager cosmetics;
    private final RankManager ranks;
    private final Type type;
    private final List<Cosmetic> all;

    public CosmeticMenu(CosmeticManager cosmetics, RankManager ranks, DataRegistry registry, Type type) {
        super(registry, type.title());
        this.cosmetics = cosmetics;
        this.ranks = ranks;
        this.type = type;
        this.all = Cosmetics.ofType(type);
    }

    // Without the ones earned in a region that is not released yet, admins see them all
    private List<Cosmetic> visible(Player player) {
        if (player.hasPermission("aurora.admin")) return all;
        return all.stream().filter(cosmetic -> cosmetics.owns(player, cosmetic) || switch (cosmetic.source()) {
            case ZONE -> registry.getRegions().isZoneReleased(cosmetic.target());
            case WORLD -> Cosmetics.regions(cosmetic).stream().allMatch(registry.getRegions()::isReleased);
            default -> Cosmetics.isReleased(cosmetic);
        }).toList();
    }

    @Override
    protected List<ItemStack> entries(Player player) {
        int essence = ranks.essenceOf(player);
        boolean twoSuffixes = type == Type.SUFFIX && cosmetics.hasSecondSuffix(player);

        List<ItemStack> items = new ArrayList<>();
        for (Cosmetic cosmetic : visible(player)) {
            boolean owned = cosmetics.owns(player, cosmetic);
            Slot wornIn = wornIn(player, cosmetic);

            // A trim is always drawn, so it can be seen before it is bought
            ItemStack item = new ItemStack(owned || type == Type.TRIM_PATTERN ? cosmetic.icon() : Material.GRAY_DYE);
            if (type == Type.TRIM_PATTERN) {
                ArmorTrim trim = cosmetics.trimOf(player, cosmetic);
                item.editMeta(ArmorMeta.class, meta -> {
                    if (trim != null) meta.setTrim(trim);
                    meta.addItemFlags(ItemFlag.HIDE_ARMOR_TRIM);
                });
            }
            item.editMeta(meta -> {
                meta.displayName(TextUtils.parse("<!italic>").append(cosmetic.component()));
                meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);

                List<Component> lore = new ArrayList<>();
                if (type == Type.JOIN_MESSAGE) {
                    lore.add(TextUtils.parse("<!italic>" + cosmetic.value().replace(Cosmetics.PLAYER_TAG, player.getName())));
                    lore.add(Component.empty());
                }
                if (type == Type.DEATH_MESSAGE) {
                    for (String line : cosmetic.value().split(java.util.regex.Pattern.quote(Cosmetics.DEATH_SEP))) {
                        lore.add(TextUtils.parse("<!italic>" + line.replace(Cosmetics.PLAYER_TAG, player.getName())
                                .replace(Cosmetics.KILLER_TAG, "<white>a Pharaoh")));
                    }
                    lore.add(Component.empty());
                }
                lore.add(TextUtils.parse("<!italic><dark_gray>" + source(cosmetic)));
                if (cosmetic.note() != null) lore.add(TextUtils.parse("<!italic><dark_gray>" + cosmetic.note()));
                lore.add(Component.empty());

                if (wornIn != null) {
                    lore.add(TextUtils.parse("<!italic><green>You are wearing this"
                            + (twoSuffixes ? (wornIn == Slot.SUFFIX_1 ? " first" : " second") : "")));
                    lore.add(TextUtils.parse("<!italic><gray>Click to take it off"));
                } else if (owned) {
                    lore.add(TextUtils.parse(twoSuffixes
                            ? "<!italic><yellow>Left click: first suffix, right click: second"
                            : "<!italic><yellow>Click to wear"));
                } else {
                    lore.add(TextUtils.parse("<!italic>" + locked(player, cosmetic, essence)));
                }
                meta.lore(lore);

                if (wornIn != null) meta.setEnchantmentGlintOverride(true);
            });
            items.add(item);
        }
        return items;
    }

    // The slot a cosmetic is worn in, null if it is not worn
    private Slot wornIn(Player player, Cosmetic cosmetic) {
        for (Slot slot : Slot.values()) {
            if (slot.type() == type && cosmetics.equipped(player.getUniqueId(), slot) == cosmetic) return slot;
        }
        return null;
    }

    @Override
    protected void back(Player player) {
        cosmetics.openHub(player);
    }

    private String source(Cosmetic cosmetic) {
        return switch (cosmetic.source()) {
            case DEFAULT -> "Everyone has this";
            case ZONE -> {
                UpgradeZone zone = registry.getUpgradeZone(cosmetic.target());
                yield "Earned by maxing every upgrade of " + (zone == null ? "its zone" : zone.name());
            }
            case WORLD -> "Earned by maxing every zone of this world";
            case PLAYTIME -> "Earned by playing for " + cosmetic.requirement() / 3600 + "h";
            case ESSENCE -> "Costs " + String.format("%,d", cosmetic.requirement()) + " Essence";
            case GRANT -> "Not sold for Essence";
        };
    }

    private String locked(Player player, Cosmetic cosmetic, int essence) {
        return switch (cosmetic.source()) {
            case PLAYTIME -> {
                long left = cosmetic.requirement() - ranks.playtimeOf(player);
                yield "<red>Play " + (left >= 3600 ? left / 3600 + " more hours" : Math.max(1, left / 60) + " more minutes");
            }
            case ESSENCE -> essence >= cosmetic.requirement() ? "<yellow>Click to buy"
                    : "<red>" + String.format("%,d", cosmetic.requirement() - essence) + " more Essence needed";
            case ZONE -> "<red>Buy every upgrade there";
            case WORLD -> Cosmetics.regions(cosmetic).stream().allMatch(region -> registry.getRegions().zonesOf(region).isEmpty())
                    ? "<red>Not open yet" : "<red>Max out every zone there";
            default -> "<red>Locked";
        };
    }

    @Override
    protected List<Component> info(Player player, List<ItemStack> entries) {
        List<Cosmetic> all = visible(player);
        long owned = all.stream().filter(cosmetic -> cosmetics.owns(player, cosmetic)).count();
        List<Component> lines = new ArrayList<>();
        lines.add(TextUtils.parse("<!italic><gray>" + type.description() + "."));
        lines.add(TextUtils.parse("<!italic><gray>Unlocked: <white>" + owned + "/" + all.size()));
        lines.add(TextUtils.parse("<!italic><gray>Your Essence: <gold>" + String.format("%,d", ranks.essenceOf(player))));
        if (type == Type.SUFFIX && !cosmetics.hasSecondSuffix(player)) {
            lines.add(TextUtils.parse("<!italic><dark_gray>A second suffix comes with a donor package"));
        }
        return lines;
    }

    @Override
    protected void onEntryClick(Player player, int index, boolean rightClick) {
        List<Cosmetic> all = visible(player);
        if (index >= all.size()) return;

        Cosmetic cosmetic = all.get(index);
        Slot wornIn = wornIn(player, cosmetic);
        String problem = null;

        if (wornIn != null) {
            cosmetics.unequip(player, wornIn);
        } else if (cosmetics.owns(player, cosmetic)) {
            Slot slot = type == Type.SUFFIX && rightClick && cosmetics.hasSecondSuffix(player)
                    ? Slot.SUFFIX_2 : Slot.of(type);
            problem = cosmetics.equip(player, slot, cosmetic);
        } else if (cosmetic.source() == Cosmetic.Source.ESSENCE) {
            problem = cosmetics.buy(player, cosmetic);
        } else {
            return;
        }

        if (problem != null) TextUtils.error(player, problem);
        else player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1f);
        render(player);
    }
}
