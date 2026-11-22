package org.charky.aurora.features.gauntlet;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.charky.aurora.common.menu.PagedMenu;
import org.charky.aurora.common.util.TextUtils;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

// /gauntlet in the gauntlet's world, or its menu item: start a run, the forge, the
// starting kit, talents, the shop, challenges, achievements, milestones, the boards,
// PvP, duels, spectating, the party, and the way out
public class GauntletMenu extends PagedMenu {
    // A button and what clicking it does, null for one that only shows something
    private record Button(ItemStack item, Consumer<Player> click) {
    }

    private final GauntletManager manager;
    private final GauntletRuns runs;
    // What each entry of the last render does when clicked
    private List<Consumer<Player>> clicks = List.of();

    public GauntletMenu(GauntletManager manager, GauntletRuns runs) {
        super(manager.registry(), "The Gauntlet");
        this.manager = manager;
        this.runs = runs;
    }

    @Override
    protected List<ItemStack> entries(Player player) {
        GauntletProfile profile = manager.profile(player);
        List<Button> buttons = List.of(
                new Button(start(player), this::startRun),
                new Button(tower(profile), this::climbTower),
                new Button(forge(profile), viewer -> new ForgeMenu(manager, runs).open(viewer)),
                new Button(kit(), viewer -> new SetupMenu(manager, runs).open(viewer)),
                new Button(talents(profile), viewer -> new TalentsMenu(manager).open(viewer)),
                new Button(shop(profile), viewer -> new ShardShopMenu(manager).open(viewer)),
                new Button(challenges(player, profile), null),
                new Button(achievements(profile), viewer -> new AchievementsMenu(manager).open(viewer)),
                new Button(bestiary(profile), viewer -> new BestiaryMenu(manager).open(viewer)),
                new Button(milestones(profile), null),
                new Button(topFloors(profile), null),
                new Button(history(profile), viewer -> new HistoryMenu(manager).open(viewer)),
                new Button(stats(profile), null),
                new Button(week(player), null),
                new Button(pvp(profile), null),
                new Button(duels(profile), null),
                new Button(spectate(), viewer -> new SpectateMenu(manager).open(viewer)),
                new Button(party(player), viewer -> new PartyMenu(manager).open(viewer)),
                new Button(button(Material.OAK_DOOR, "<gray><b>Leave the Gauntlet", List.of(
                        "<gray>Back to the main spawn,", "<gray>with your own inventory.")), this::leaveGauntlet));
        clicks = buttons.stream().map(Button::click).toList();
        return buttons.stream().map(Button::item).toList();
    }

    private ItemStack start(Player player) {
        List<Integer> checkpoints = runs.checkpoints(player);
        GauntletParty party = manager.party();
        boolean inParty = party != null && party.inParty(player);
        return button(Material.IRON_SWORD, "<red><b>Start a Run", List.of(
                "<gray>Climb the tower floor by floor.",
                "<gray>Every " + (int) manager.data().setting("boon_every", 5) + " floors: a care package",
                "<gray>and a boon for this run.",
                "<gray>Shards and Cores are kept when it ends.",
                checkpoints.size() > 1 ? "<gray>Start on floor <white>" + checkpoints.getLast() + "</white> or earlier" : "",
                "",
                runs.inRun(player) ? "<red>You are in a run"
                        : inParty && !party.isLeader(player) ? "<gray>Your party leader starts the run"
                        : inParty ? "<yellow>Click to start for your party of " + party.team(player).size()
                        : "<yellow>Click to start"));
    }

    // Today's Daily Tower: how it works, how far the best got, and the player's try
    private ItemStack tower(GauntletProfile profile) {
        boolean tried = profile.stat(GauntletRuns.TOWER_DAY) == LocalDate.now().toEpochDay();
        List<String> lines = new ArrayList<>(List.of(
                "<gray>One try a day, alone, from floor 1.",
                "<gray>Every floor from " + (int) manager.data().setting("mutator_from", 6) + " is mutated, the",
                "<gray>same tower for everyone today.",
                "",
                "<yellow>Today's best"));
        lines.addAll(GauntletBoards.rows(runs.towerToday(5), top -> "floor " + top.stat(GauntletRuns.TOWER_BEST), "Nobody has climbed it yet"));
        lines.add("");
        lines.add(tried ? "<gray>Your floor today: <white>" + profile.stat(GauntletRuns.TOWER_BEST) : "<yellow>Click to climb it");
        return button(Material.LODESTONE, "<aqua><b>Daily Tower", lines);
    }

    private static ItemStack forge(GauntletProfile profile) {
        return button(Material.ANVIL, "<gold><b>The Forge", List.of(
                "<gray>Unlock and upgrade your gear",
                "<gray>and your care package.",
                "",
                "<gray>Shards: <aqua>" + profile.shards,
                "<gray>Cores: <light_purple>" + profile.cores));
    }

    private static ItemStack kit() {
        return button(Material.TOTEM_OF_UNDYING, "<yellow><b>Starting Kit", List.of(
                "<gray>Totems, potions, arrows, an elytra:",
                "<gray>bought for good, every run",
                "<gray>starts with them.",
                "",
                "<gray>Your lobby inventory is your setup:",
                "<gray>lay it out how you like it."));
    }

    private ItemStack talents(GauntletProfile profile) {
        int learned = profile.talents.values().stream().mapToInt(Integer::intValue).sum();
        int total = manager.data().talents().values().stream().mapToInt(GauntletData.Talent::max).sum();
        return button(Material.ENCHANTED_BOOK, "<green><b>Talents", List.of(
                "<gray>Permanent upgrades for every run:",
                "<gray>hearts, Shards, rerolls, a Second Wind.",
                "",
                "<gray>Learned: <white>" + learned + "/" + total));
    }

    private static ItemStack shop(GauntletProfile profile) {
        return button(Material.AMETHYST_SHARD, "<aqua><b>Shard Shop", List.of(
                "<gray>Cosmetics only the Gauntlet sells,",
                "<gray>worn everywhere in /customise.",
                "",
                "<gray>Shards: <aqua>" + profile.shards));
    }

    // Today's three, each with its progress and pay
    private ItemStack challenges(Player player, GauntletProfile profile) {
        GauntletChallenges challenges = manager.challenges();
        List<String> lines = new ArrayList<>(List.of("<gray>New ones every day, the same for", "<gray>everyone. Done the moment you are.", ""));
        for (GauntletData.Challenge challenge : challenges.today()) {
            boolean done = challenges.done(player, challenge);
            int goal = challenges.goal(player, challenge);
            lines.add((done ? "<green>✔ " : "<gray>• <white>") + challenge.describe(profile.best)
                    + (done ? "" : " <gray>" + Math.min(goal, challenges.progress(player, challenge)) + "/" + goal));
            lines.add("   <aqua>" + challenges.reward(player, challenge) + " Shards" + (challenge.cores() > 0 ? " <light_purple>+" + challenge.cores() + " Core" : ""));
        }
        return button(Material.CLOCK, "<aqua><b>Daily Challenges <gray>" + challenges.doneToday(player) + "/" + challenges.today().size(), lines);
    }

    private ItemStack achievements(GauntletProfile profile) {
        return button(Material.GOLD_BLOCK, "<gold><b>Achievements <gray>" + manager.achievements().doneCount(profile)
                + "/" + manager.data().achievements().size(), List.of(
                "<gray>Long goals: kills, bosses, combos,",
                "<gray>duels and more. Each pays Essence,",
                "<gray>Shards, and some a cosmetic.",
                "",
                "<yellow>Click to see them"));
    }

    private ItemStack bestiary(GauntletProfile profile) {
        GauntletData data = manager.data();
        return button(Material.KNOWLEDGE_BOOK, "<dark_aqua><b>Bestiary", List.of(
                "<gray>Every mob of the tower, what it",
                "<gray>is and where it comes in.",
                "",
                "<gray>Met: <white>" + BestiaryMenu.met(data, profile) + "/" + BestiaryMenu.firstFloors(data).size(),
                "<yellow>Click to see them"));
    }

    // Only the next milestone to reach, and how many are done
    private ItemStack milestones(GauntletProfile profile) {
        List<String> lines = new ArrayList<>();
        List<GauntletData.Milestone> all = manager.data().milestones();
        GauntletData.Milestone next = all.stream().filter(m -> !profile.milestones.contains(m.floor())).findFirst().orElse(null);
        long done = all.stream().filter(m -> profile.milestones.contains(m.floor())).count();
        if (next == null) {
            lines.add("<green>Every milestone reached");
        } else {
            String reward = (next.essence() > 0 ? next.essence() + " Essence" : "")
                    + (!next.cosmetic().isBlank() ? (next.essence() > 0 ? " + " : "") + cosmeticName(next.cosmetic()) : "");
            lines.add("<gray>Next: <white>clear floor " + next.floor());
            lines.add("<gray>Reward: <white>" + reward);
            lines.add("<gray>Your best: <white>" + profile.best + " <dark_gray>(" + Math.max(0, next.floor() - profile.best) + " to go)");
        }
        lines.add("");
        lines.add("<dark_gray>" + done + "/" + all.size() + " milestones reached");
        return button(Material.GOLD_INGOT, "<yellow><b>Next Milestone", lines);
    }

    private ItemStack topFloors(GauntletProfile profile) {
        List<String> lines = GauntletBoards.rows(manager.top(10), top -> "floor " + top.best, "Nobody has cleared a floor yet");
        lines.add("");
        lines.add("<gray>Your best: <white>" + profile.best + " <dark_gray>(" + profile.runs + " runs)");
        lines.add("");
        lines.add("<yellow>Fastest to floor " + GauntletRuns.sprintFloor(manager.data()) + " <gray>(solo, from floor 1)");
        lines.addAll(GauntletBoards.rows(manager.lowestBy(GauntletRuns.SPRINT, 3), top -> GauntletRuns.clock(top.stat(GauntletRuns.SPRINT)),
                "Nobody has made it yet"));
        if (profile.stat(GauntletRuns.SPRINT) > 0) lines.add("<gray>Yours: <white>" + GauntletRuns.clock(profile.stat(GauntletRuns.SPRINT)));
        return button(Material.PLAYER_HEAD, "<aqua><b>Top Floors", lines);
    }

    private static ItemStack history(GauntletProfile profile) {
        List<String> lines = new ArrayList<>(List.of(
                "<gray>Your last " + GauntletProfile.HISTORY + " runs: how far they went,",
                "<gray>how they ended and what they paid.",
                ""));
        if (profile.history.isEmpty()) {
            lines.add("<gray>No runs yet");
        } else {
            RunRecord last = profile.history.getFirst();
            lines.add("<gray>Last: <white>floor " + last.floor() + " <gray>" + (last.died() ? "fell" : "left")
                    + ", <aqua>" + last.shards() + " Shards");
            lines.add("<yellow>Click to see them");
        }
        return button(Material.WRITABLE_BOOK, "<white><b>Your Runs", lines);
    }

    // Everything counted, in one place
    private ItemStack stats(GauntletProfile profile) {
        int sprint = profile.stat(GauntletRuns.SPRINT);
        return button(Material.BOOK, "<white><b>Your Stats", List.of(
                "<gray>Runs: <white>" + profile.runs + " <gray>(" + profile.stat("party_runs") + " in a party)",
                "<gray>Best floor: <white>" + profile.best,
                "<gray>Fastest to floor " + GauntletRuns.sprintFloor(manager.data()) + ": <white>" + (sprint > 0 ? GauntletRuns.clock(sprint) : "-"),
                "",
                "<gray>Kills: <white>" + profile.stat("kills") + " <gray>(" + profile.stat("elite_kills") + " elite)",
                "<gray>Bosses: <white>" + profile.stat("boss_kills") + " <gray>Hoarders: <white>" + profile.stat("hoarders"),
                "<gray>Fast clears: <white>" + profile.stat("fast_clears") + " <gray>Best combo: <gold>x" + profile.stat(GauntletRuns.BEST_COMBO),
                "<gray>Teammates revived: <white>" + profile.stat("revives"),
                "<gray>Mobs met: <white>" + BestiaryMenu.met(manager.data(), profile) + "/" + BestiaryMenu.firstFloors(manager.data()).size(),
                "",
                "<gray>PvP: <red>" + profile.stat(GauntletPvp.KILLS) + " kills <gray>/ <white>" + profile.stat(GauntletPvp.DEATHS)
                        + " deaths <gray>best streak <gold>" + profile.stat(GauntletPvp.BEST_STREAK),
                "<gray>Duels: <white>" + profile.stat(GauntletDuels.WINS) + " won, " + profile.stat(GauntletDuels.LOSSES)
                        + " lost <gray>rating <gold>" + GauntletDuels.rating(profile)));
    }

    // The week's two boards, what their top three are paid, and the time left
    private ItemStack week(Player player) {
        GauntletWeekly weekly = manager.weekly();
        GauntletData data = manager.data();
        List<String> lines = new ArrayList<>(List.of(
                "<gray>The week's best floor and most PvP",
                "<gray>wins. The top three of each are paid",
                "<gray>when the week ends, in <white>" + GauntletWeekly.timeLeft(),
                "",
                "<gray>Your floor: <white>" + weekly.mine(player.getUniqueId(), GauntletWeekly.FLOOR)
                        + " <gray>PvP wins: <white>" + weekly.mine(player.getUniqueId(), GauntletWeekly.PVP),
                "",
                "<yellow>Best floor <gray>(" + (int) data.setting("weekly_floor_1", 0) + " / " + (int) data.setting("weekly_floor_2", 0)
                        + " / " + (int) data.setting("weekly_floor_3", 0) + " Shards)"));
        lines.addAll(GauntletBoards.rows(weekly.top(GauntletWeekly.FLOOR, 3), top -> "floor " + top.stat(GauntletWeekly.FLOOR), "Nobody yet this week"));
        lines.add("<yellow>PvP wins <gray>(" + (int) data.setting("weekly_pvp_1", 0) + " / " + (int) data.setting("weekly_pvp_2", 0)
                + " / " + (int) data.setting("weekly_pvp_3", 0) + " Shards)");
        lines.addAll(GauntletBoards.rows(weekly.top(GauntletWeekly.PVP, 3), top -> top.stat(GauntletWeekly.PVP) + " wins", "Nobody yet this week"));
        return button(Material.SUNFLOWER, "<gold><b>This Week", lines);
    }

    private ItemStack pvp(GauntletProfile profile) {
        List<String> lines = new ArrayList<>(List.of(
                "<gray>Fight other players in the PvP zone",
                "<gray>with your setup. Kills pay Shards,",
                "<gray>more on a streak.",
                "",
                "<gray>Your kills: <red>" + profile.stat(GauntletPvp.KILLS) + " <gray>deaths: <white>" + profile.stat(GauntletPvp.DEATHS),
                "<gray>Best streak: <gold>" + profile.stat(GauntletPvp.BEST_STREAK),
                ""));
        lines.addAll(GauntletBoards.rows(manager.topBy(GauntletPvp.KILLS, 5), top -> top.stat(GauntletPvp.KILLS) + " kills", "Nobody has a PvP kill yet"));
        return button(Material.DIAMOND_SWORD, "<red><b>PvP", lines);
    }

    private ItemStack duels(GauntletProfile profile) {
        List<String> lines = new ArrayList<>(List.of(
                "<gray>Challenge a player to a 1v1 in your",
                "<gray>own arena: <white>/gauntlet duel <player>",
                "<gray>Wins raise your rating and pay Shards.",
                "",
                "<gray>Your rating: <gold>" + GauntletDuels.rating(profile) + " <gray>(" + profile.stat(GauntletDuels.WINS)
                        + " won, " + profile.stat(GauntletDuels.LOSSES) + " lost)",
                ""));
        lines.addAll(GauntletBoards.rows(manager.topBy(GauntletDuels.RATING, 5), top -> String.valueOf(top.stat(GauntletDuels.RATING)),
                "Nobody has fought a duel yet"));
        return button(Material.CROSSBOW, "<gold><b>Duels", lines);
    }

    private ItemStack spectate() {
        long live = Bukkit.getOnlinePlayers().stream().filter(other -> manager.inRun(other) || manager.inDuel(other)).count();
        return button(Material.ENDER_EYE, "<aqua><b>Spectate", List.of(
                "<gray>Watch someone's run or duel live.",
                "",
                "<gray>Live right now: <white>" + live,
                "<yellow>Click to pick one"));
    }

    // The party, an invite waiting, or how to start one
    private ItemStack party(Player player) {
        GauntletParty party = manager.party();
        List<String> lines = new ArrayList<>(List.of(
                "<gray>Climb with friends: more mobs,",
                "<gray>tougher bosses, one pot of Shards",
                "<gray>for everyone.",
                ""));
        Player inviter = party == null ? null : party.inviter(player);
        if (party != null && party.inParty(player)) {
            List<Player> team = party.team(player);
            lines.add("<gray>Your party: <white>" + team.size() + "/" + party.max());
            for (Player member : team) lines.add((member.equals(team.getFirst()) ? "<gold>★ " : "<gray>• <white>") + member.getName());
            lines.add("");
            lines.add("<yellow>Click to manage it");
        } else if (inviter != null) {
            lines.add("<green>" + inviter.getName() + " invited you!");
            lines.add("<yellow>Click to join");
        } else {
            lines.add("<yellow>Click to invite players");
        }
        return button(Material.CAKE, "<aqua><b>Party", lines);
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

    @Override
    protected List<Component> info(Player player, List<ItemStack> entries) {
        return List.of(TextUtils.parse("<!italic><gray>A game of its own: gear here"),
                TextUtils.parse("<!italic><gray>stays here. Milestones pay Essence"),
                TextUtils.parse("<!italic><gray>and cosmetics for the main game."));
    }

    @Override
    protected void onEntryClick(Player player, int index) {
        if (index < clicks.size() && clicks.get(index) != null) clicks.get(index).accept(player);
    }

    private void climbTower(Player player) {
        String problem = runs.beginTower(player);
        if (problem != null) TextUtils.error(player, problem);
    }

    private void startRun(Player player) {
        String problem = runs.startFlow(player);
        if (problem != null) TextUtils.error(player, problem);
    }

    // From the PvP zone only after its countdown
    private void leaveGauntlet(Player player) {
        player.closeInventory();
        String problem = manager.pvp() == null ? null : manager.pvp().leave(player, true);
        if (manager.pvp() == null) manager.leave(player);
        if (problem != null) TextUtils.error(player, problem);
    }
}
