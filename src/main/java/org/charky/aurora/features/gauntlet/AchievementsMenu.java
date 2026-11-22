package org.charky.aurora.features.gauntlet;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.charky.aurora.common.menu.PagedMenu;
import org.charky.aurora.common.util.TextUtils;

import java.util.ArrayList;
import java.util.List;

// Every achievement with its progress and reward, the done ones ticked
public class AchievementsMenu extends PagedMenu {
    private final GauntletManager manager;

    public AchievementsMenu(GauntletManager manager) {
        super(manager.registry(), "Achievements");
        this.manager = manager;
    }

    @Override
    protected List<ItemStack> entries(Player player) {
        GauntletProfile profile = manager.profile(player);
        GauntletAchievements achievements = manager.achievements();
        List<ItemStack> items = new ArrayList<>();
        for (GauntletData.Achievement achievement : manager.data().achievements()) {
            boolean done = achievements.done(profile, achievement);
            int value = Math.min(achievement.goal(), achievements.value(profile, achievement.stat()));
            ItemStack item = new ItemStack(done ? Material.GOLD_BLOCK : Material.PAPER);
            item.editMeta(meta -> {
                meta.displayName(TextUtils.parse("<!italic>" + (done ? "<gold><b>✔ " : "<white><b>") + achievement.name()));
                meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
                List<Component> lore = new ArrayList<>();
                lore.add(TextUtils.parse("<!italic><gray>" + achievement.describe()));
                if (!done) lore.add(TextUtils.parse("<!italic><gray>Progress: <white>" + String.format("%,d", value) + "/" + String.format("%,d", achievement.goal())));
                lore.add(Component.empty());
                if (achievement.essence() > 0) lore.add(TextUtils.parse("<!italic><aqua>" + achievement.essence() + " Essence"));
                if (achievement.shards() > 0) lore.add(TextUtils.parse("<!italic><aqua>" + achievement.shards() + " Shards"));
                if (!achievement.cosmetic().isBlank()) lore.add(TextUtils.parse("<!italic><light_purple>A cosmetic for /customise"));
                meta.lore(lore);
                if (done) meta.setEnchantmentGlintOverride(true);
            });
            items.add(item);
        }
        return items;
    }

    @Override
    protected List<Component> info(Player player, List<ItemStack> entries) {
        GauntletProfile profile = manager.profile(player);
        return List.of(TextUtils.parse("<!italic><gray>Done: <white>" + manager.achievements().doneCount(profile)
                + "/" + manager.data().achievements().size()));
    }

    @Override
    protected void back(Player player) {
        manager.openMenu(player);
    }
}
