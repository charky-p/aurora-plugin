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
import java.util.Map;
import java.util.UUID;

// /gauntlet in the gauntlet's world, or its menu item: start a run, the forge, the
// starting kit, milestones, the top floors, and the way out
public class GauntletMenu extends PagedMenu {
    private static final int START = 0, FORGE = 1, KIT = 2, TALENTS = 3, MILESTONES = 4, TOP = 5, PVP = 6, LEAVE = 7;

    private final GauntletManager manager;
    private final GauntletRuns runs;

    public GauntletMenu(GauntletManager manager, GauntletRuns runs) {
        super(manager.registry(), "The Gauntlet");
        this.manager = manager;
        this.runs = runs;
    }

    @Override
    protected List<ItemStack> entries(Player player) {
        GauntletProfile profile = manager.profile(player);
        List<ItemStack> items = new ArrayList<>();
        List<Integer> checkpoints = runs.checkpoints(player);
        items.add(button(Material.IRON_SWORD, "<red><b>Start a Run", List.of(
                "<gray>Climb the tower floor by floor.",
                "<gray>Every 5 floors: a care package",
                "<gray>and a boon for this run.",
                "<gray>Shards and Cores are kept when it ends.",
                checkpoints.size() > 1 ? "<gray>Start on floor <white>" + checkpoints.getLast() + "</white> or earlier" : "",
                "",
                runs.inRun(player) ? "<red>You are in a run" : "<yellow>Click to start")));
        items.add(button(Material.ANVIL, "<gold><b>The Forge", List.of(
                "<gray>Unlock and upgrade your gear",
                "<gray>and your care package.",
                "",
                "<gray>Shards: <aqua>" + profile.shards,
                "<gray>Cores: <light_purple>" + profile.cores)));
        items.add(button(Material.TOTEM_OF_UNDYING, "<yellow><b>Starting Kit", List.of(
                "<gray>Totems, potions, arrows, an elytra:",
                "<gray>bought for good, every run",
                "<gray>starts with them.",
                "",
                "<gray>Your lobby inventory is your setup:",
                "<gray>lay it out how you like it.")));

        // Only the next milestone to reach, and how many are done
        int learned = profile.talents.values().stream().mapToInt(Integer::intValue).sum();
        int total = manager.data().talents().values().stream().mapToInt(GauntletData.Talent::max).sum();
        items.add(button(Material.ENCHANTED_BOOK, "<green><b>Talents", List.of(
                "<gray>Permanent upgrades for every run:",
                "<gray>hearts, Shards, rerolls, a Second Wind.",
                "",
                "<gray>Learned: <white>" + learned + "/" + total)));

        List<String> milestones = new ArrayList<>();
        List<GauntletData.Milestone> all = manager.data().milestones();
        GauntletData.Milestone next = all.stream().filter(m -> !profile.milestones.contains(m.floor())).findFirst().orElse(null);
        long done = all.stream().filter(m -> profile.milestones.contains(m.floor())).count();
        if (next == null) {
            milestones.add("<green>Every milestone reached");
        } else {
            String reward = (next.essence() > 0 ? next.essence() + " Essence" : "")
                    + (!next.cosmetic().isBlank() ? (next.essence() > 0 ? " + " : "") + cosmeticName(next.cosmetic()) : "");
            milestones.add("<gray>Next: <white>clear floor " + next.floor());
            milestones.add("<gray>Reward: <white>" + reward);
            milestones.add("<gray>Your best: <white>" + profile.best + " <dark_gray>(" + Math.max(0, next.floor() - profile.best) + " to go)");
        }
        milestones.add("");
        milestones.add("<dark_gray>" + done + "/" + all.size() + " milestones reached");
        items.add(button(Material.GOLD_INGOT, "<yellow><b>Next Milestone", milestones));

        List<String> top = new ArrayList<>();
        int place = 1;
        for (Map.Entry<UUID, GauntletProfile> entry : manager.top(10)) {
            top.add("<gray>" + place++ + ". <white>" + GauntletManager.nameOf(entry.getKey()) + " <gray>- floor " + entry.getValue().best);
        }
        if (top.isEmpty()) top.add("<gray>Nobody has cleared a floor yet");
        top.add("");
        top.add("<gray>Your best: <white>" + profile.best + " <dark_gray>(" + profile.runs + " runs)");
        items.add(button(Material.PLAYER_HEAD, "<aqua><b>Top Floors", top));

        List<String> pvp = new ArrayList<>(List.of(
                "<gray>Fight other players in the PvP zone",
                "<gray>with your setup. Kills pay Shards,",
                "<gray>more on a streak.",
                "",
                "<gray>Your kills: <red>" + profile.stat(GauntletPvp.KILLS) + " <gray>deaths: <white>" + profile.stat(GauntletPvp.DEATHS),
                "<gray>Best streak: <gold>" + profile.stat(GauntletPvp.BEST_STREAK),
                ""));
        int rank = 1;
        for (Map.Entry<UUID, GauntletProfile> entry : manager.topBy(GauntletPvp.KILLS, 5)) {
            pvp.add("<gray>" + rank++ + ". <white>" + GauntletManager.nameOf(entry.getKey()) + " <gray>- " + entry.getValue().stat(GauntletPvp.KILLS) + " kills");
        }
        if (rank == 1) pvp.add("<gray>Nobody has a PvP kill yet");
        items.add(button(Material.DIAMOND_SWORD, "<red><b>PvP", pvp));

        items.add(button(Material.OAK_DOOR, "<gray><b>Leave the Gauntlet", List.of(
                "<gray>Back to the main spawn,", "<gray>with your own inventory.")));
        return items;
    }

    private static String cosmeticName(String id) {
        int sep = id.indexOf('_');
        String kind = sep < 0 ? "" : id.substring(0, sep);
        String name = (sep < 0 ? id : id.substring(sep + 1)).replace('_', ' ').toUpperCase();
        return switch (kind) {
            case "suffix" -> "[" + name + "] suffix";
            case "trail" -> name.charAt(0) + name.substring(1).toLowerCase() + " trail";
            default -> name;
        };
    }

    private static ItemStack button(Material material, String name, List<String> lines) {
        ItemStack item = new ItemStack(material);
        item.editMeta(meta -> {
            meta.displayName(TextUtils.parse("<!italic>" + name));
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            List<Component> lore = new ArrayList<>();
            for (String line : lines) lore.add(line.isEmpty() ? Component.empty() : TextUtils.parse("<!italic>" + line));
            meta.lore(lore);
        });
        return item;
    }

    @Override
    protected List<Component> info(Player player, List<ItemStack> entries) {
        return List.of(TextUtils.parse("<!italic><gray>A game of its own: gear here"),
                TextUtils.parse("<!italic><gray>stays here. Milestones pay Essence"),
                TextUtils.parse("<!italic><gray>and cosmetics for the main game."));
    }

    @Override
    protected void onEntryClick(Player player, int index) {
        switch (index) {
            case START -> {
                String problem = runs.startFlow(player);
                if (problem != null) TextUtils.error(player, problem);
            }
            case TALENTS -> new TalentsMenu(manager).open(player);
            case FORGE -> new ForgeMenu(manager, runs).open(player);
            case KIT -> new SetupMenu(manager, runs).open(player);
            case LEAVE -> {
                player.closeInventory();
                // From the PvP zone only after its countdown
                String problem = manager.pvp() == null ? null : manager.pvp().leave(player, true);
                if (manager.pvp() == null) manager.leave(player);
                if (problem != null) TextUtils.error(player, problem);
            }
            default -> {
            }
        }
    }
}
