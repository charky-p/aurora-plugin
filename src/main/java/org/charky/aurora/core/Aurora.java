package org.charky.aurora.core;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Entity;
import org.bukkit.event.Listener;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.charky.aurora.common.menu.PagedMenuListener;
import org.charky.aurora.common.util.DataKeys;
import org.charky.aurora.common.util.TextUtils;
import org.charky.aurora.core.database.AccessStore;
import org.charky.aurora.core.database.BuildStore;
import org.charky.aurora.core.database.PadStore;
import org.charky.aurora.core.database.GauntletStore;
import org.charky.aurora.core.database.QuestStore;
import org.charky.aurora.core.database.SafeZoneStore;
import org.charky.aurora.core.database.BackpackStore;
import org.charky.aurora.core.database.CollectionStore;
import org.charky.aurora.core.database.CosmeticStore;
import org.charky.aurora.core.database.Database;
import org.charky.aurora.core.database.MineStore;
import org.charky.aurora.core.database.PerkStore;
import org.charky.aurora.core.database.PlayerStore;
import org.charky.aurora.core.database.PortalStore;
import org.charky.aurora.core.database.RankStore;
import org.charky.aurora.core.database.Schema;
import org.charky.aurora.core.database.LoadoutStore;
import org.charky.aurora.core.database.SpawnStore;
import org.charky.aurora.core.database.SpawnerStore;
import org.charky.aurora.core.database.SuggestionStore;
import org.charky.aurora.core.database.ToolStore;
import org.charky.aurora.core.database.TutorialStore;
import org.charky.aurora.core.database.TraderStore;
import org.charky.aurora.core.database.UnlockStore;
import org.charky.aurora.core.database.WorldStore;
import org.charky.aurora.features.armory.ArmoryManager;
import org.charky.aurora.features.boss.BossManager;
import org.charky.aurora.features.collection.CollectionManager;
import org.charky.aurora.features.combat.BowListener;
import org.charky.aurora.features.combat.CombatListener;
import org.charky.aurora.features.core.EssenceExchange;
import org.charky.aurora.features.core.OverworldCore;
import org.charky.aurora.features.cosmetic.CosmeticManager;
import org.charky.aurora.features.combat.SustenanceTask;
import org.charky.aurora.features.keys.AccessManager;
import org.charky.aurora.features.build.BuildManager;
import org.charky.aurora.features.portal.PadManager;
import org.charky.aurora.features.pvp.SafeZoneManager;
import org.charky.aurora.features.keys.KeyItemListener;
import org.charky.aurora.features.loadout.Loadout;
import org.charky.aurora.features.loadout.LoadoutData;
import org.charky.aurora.features.loadout.LoadoutManager;
import org.charky.aurora.features.mine.BlockUseListener;
import org.charky.aurora.features.mine.MineListener;
import org.charky.aurora.features.movement.DashListener;
import org.charky.aurora.features.movement.DoubleJumpListener;
import org.charky.aurora.features.movement.GrappleListener;
import org.charky.aurora.features.movement.MountManager;
import org.charky.aurora.features.mine.MineManager;
import org.charky.aurora.features.player.BackpackCommand;
import org.charky.aurora.features.player.BackpackItemListener;
import org.charky.aurora.features.player.BackpackManager;
import org.charky.aurora.features.player.ChatShowcase;
import org.charky.aurora.features.player.LeaderboardMenu;
import org.charky.aurora.features.player.LevelManager;
import org.charky.aurora.features.player.PlayerManager;
import org.charky.aurora.features.player.PlayerSessionListener;
import org.charky.aurora.features.player.ScoreboardManager;
import org.charky.aurora.features.player.ServerMessages;
import org.charky.aurora.features.player.TabManager;
import org.charky.aurora.features.player.TrashCommand;
import org.charky.aurora.features.portal.PortalListener;
import org.charky.aurora.features.portal.PortalManager;
import org.charky.aurora.features.perk.PerkManager;
import org.charky.aurora.features.cosmetic.Cosmetic;
import org.charky.aurora.features.cosmetic.Cosmetics;
import org.charky.aurora.features.system.HowToPlay;
import org.charky.aurora.features.system.Tips;
import org.charky.aurora.features.player.PlayerData;
import org.charky.aurora.features.system.CommandFilter;
import org.charky.aurora.features.rank.RankManager;
import org.charky.aurora.features.rank.RankShopMenu;
import org.charky.aurora.features.spawner.LeftoverMobListener;
import org.charky.aurora.features.spawner.MobDeathListener;
import org.charky.aurora.features.spawner.SpawnerManager;
import org.charky.aurora.features.suggestion.SuggestCommand;
import org.charky.aurora.features.suggestion.SuggestionManager;
import org.charky.aurora.features.system.SpawnListener;
import org.charky.aurora.features.gauntlet.GauntletCommand;
import org.charky.aurora.features.gauntlet.GauntletManager;
import org.charky.aurora.features.gauntlet.GauntletRuns;
import org.charky.aurora.features.quest.QuestManager;
import org.charky.aurora.features.tutorial.TutorialCommand;
import org.charky.aurora.features.tutorial.TutorialManager;
import org.charky.aurora.features.system.WandListener;
import org.charky.aurora.features.system.WandManager;
import org.charky.aurora.features.system.commands.AuroraCommand;
import org.charky.aurora.features.system.commands.DiscordCommand;
import org.charky.aurora.features.system.commands.DumpCommand;
import org.charky.aurora.features.system.commands.OpenMenuCommand;
import org.charky.aurora.features.system.commands.RefreshCommand;
import org.charky.aurora.features.system.commands.SpawnCommand;
import org.charky.aurora.features.system.commands.TopCommand;
import org.charky.aurora.features.tool.ToolManager;
import org.charky.aurora.features.tool.UpgradeShop;
import org.charky.aurora.features.trader.TraderListener;
import org.charky.aurora.features.warp.WarpMenu;
import org.charky.aurora.features.trader.TraderManager;
import org.charky.aurora.features.world.WorldManager;
import org.charky.aurora.features.fishing.RodShopMenu;
import org.charky.aurora.features.fishing.FishingManager;
import org.charky.aurora.features.fishing.FishMerchantMenu;
import org.charky.aurora.core.database.FishingStore;
import org.charky.aurora.features.moderation.StaffPermissions;
import org.charky.aurora.features.moderation.ModerationManager;
import org.charky.aurora.features.moderation.ModerationCommand;
import org.charky.aurora.core.database.PunishmentStore;
import org.charky.aurora.features.pvp.KillStreakListener;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

public class Aurora extends JavaPlugin {
    // Where players join until /aurora setspawn main picks another world. Loaded
    // like any other world made with /aurora world create, the plugin makes none itself
    public static final String MAIN_WORLD = "aurora_overworld";

    private Database db;
    private SpawnStore spawnStore;
    private MineManager mineManager;
    private PlayerManager players;
    private ScoreboardManager scoreboard;
    private SpawnerManager spawnerManager;
    private TraderManager traderManager;
    private WandManager wandManager;
    private WorldManager worldManager;
    private PortalManager portalManager;
    private BackpackManager backpackManager;
    private ArmoryManager armoryManager;
    private BossManager bossManager;
    private LevelManager levelManager;
    private RankManager rankManager;
    private CollectionManager collectionManager;
    private PerkManager perkManager;
    private CosmeticManager cosmeticManager;
    private TutorialManager tutorialManager;
    private QuestManager questManager;
    private GauntletManager gauntletManager;
    private GauntletRuns gauntletRuns;
    private AccessManager accessManager;
    private BuildManager buildManager;
    private DashListener dashListener;
    private TabManager tabManager;
    private TrashCommand trash;
    private SafeZoneManager safeZoneManager;
    private PadManager padManager;
    private FishingManager fishingManager;
    private StaffPermissions staffPermissions;
    private ModerationManager moderationManager;
    private MountManager mountManager;
    private ToolManager toolManager;
    private UpgradeShop upgradeShop;
    private SuggestionManager suggestionManager;
    private LoadoutManager loadoutManager;
    private DataRegistry registry;
    private Map<World, Location> spawnMap = new HashMap<>();
    private String mainWorld = MAIN_WORLD;

    @Override
    public void onEnable() {
        loadData();
        openDatabase();
        loadWorlds();
        createPlayerSystems();
        createProgression();
        createWorldSystems();
        createQuests();
        createGauntlet();
        createLoadouts();
        registerMenuActions();
        registerCommands();
        registerListeners();

        SustenanceTask.start(this);
        Tips.start(this);
        LeftoverMobListener.sweepLoaded();
        traderManager.startRespawning();

        getLogger().info("AuroraMines has been enabled!");
    }

    // The generated YAML: items, mobs, menus, traders, tools and upgrades
    private void loadData() {
        this.registry = new DataRegistry(this);
        this.backpackManager = new BackpackManager(registry);
        registry.setBackpackManager(backpackManager);
        this.armoryManager = new ArmoryManager(registry);
        registry.setArmoryManager(armoryManager);
        registry.loadAll();
    }

    // One store per table, each manager only gets the one it needs
    private void openDatabase() {
        if (!getDataFolder().exists()) getDataFolder().mkdirs();
        db = new Database(this, getDataFolder().getAbsolutePath());
        Schema.create(db);
        getLogger().info("SQLite Database setup complete!");

        this.spawnStore = new SpawnStore(db);
        backpackManager.loadBackpacks(db, new BackpackStore(db));
        backpackManager.startAutosave(this);
        armoryManager.load(db, new UnlockStore(db));
        armoryManager.migrateFrom(backpackManager);
    }

    // Extra worlds first, spawns and portals look worlds up by name
    private void loadWorlds() {
        this.worldManager = new WorldManager(this, new WorldStore(db));
        worldManager.loadAll();

        this.spawnMap = spawnStore.loadAll();
        String savedMain = spawnStore.loadMain();
        if (savedMain != null && Bukkit.getWorld(savedMain) != null) mainWorld = savedMain;
        else if (savedMain != null) getLogger().warning("Main world '" + savedMain + "' is not loaded, players join in " + MAIN_WORLD);
    }

    // Everything a player carries between sessions: stats, ranks, collections, cosmetics, perks
    private void createPlayerSystems() {
        this.players = new PlayerManager(this, db, new PlayerStore(db));
        this.scoreboard = new ScoreboardManager(players);
        this.tabManager = new TabManager(players);
        this.levelManager = new LevelManager(this, players, scoreboard);
        this.rankManager = new RankManager(this, db, new RankStore(db), players, scoreboard, tabManager);
        // A staff rank is its permissions, redrawn whenever ranks change
        this.staffPermissions = new StaffPermissions(this, rankManager);
        rankManager.setOnChange(staffPermissions::update);
        this.moderationManager = new ModerationManager(db, new PunishmentStore(db));
        this.collectionManager = new CollectionManager(this, registry, db, new CollectionStore(db), players, scoreboard);
        this.cosmeticManager = new CosmeticManager(this, db, new CosmeticStore(db), players, rankManager, scoreboard, tabManager);
        this.perkManager = new PerkManager(this, db, new PerkStore(db), players, scoreboard);
        armoryManager.setKeyring(perkManager::hasKeyring);
        armoryManager.setEssence((player, amount) -> {
            PlayerData data = players.get(player);
            if (data == null) return;
            data.addEssence(amount);
            scoreboard.updateBoard(player);
        });
        this.mountManager = new MountManager(this, registry, perkManager);
    }

    // Tools and their upgrades, and what listens to them: cosmetics for maxed zones, the tutorial
    private void createProgression() {
        this.toolManager = new ToolManager(this, db, new ToolStore(db));
        this.upgradeShop = new UpgradeShop(this, toolManager);
        armoryManager.setTools(toolManager);
        cosmeticManager.setMaxed((player, cosmetic) -> cosmetic.source() == Cosmetic.Source.ZONE
                ? upgradeShop.maxed(player, cosmetic.target())
                : Cosmetics.regions(cosmetic).stream().allMatch(region -> upgradeShop.maxedRegion(player, region)));
        upgradeShop.setOnMaxed(cosmeticManager::announceMaxed);
        rankManager.setOnPlaytime(cosmeticManager::playtimePassed);
        toolManager.setTrims(cosmeticManager::trim);
        toolManager.setLooks(cosmeticManager::lookFor);
        cosmeticManager.setMaterialOwned((player, cosmetic) -> toolManager.ownsMaterial(player,
                Cosmetics.LOOK_ARMOR.equals(cosmetic.target()), cosmetic.value()));
        cosmeticManager.setLookChanged(toolManager::refresh);
        this.accessManager = new AccessManager(db, new AccessStore(db), registry);
        this.tutorialManager = new TutorialManager(this, db, new TutorialStore(db), players, scoreboard, cosmeticManager);
        backpackManager.setOnOpen(tutorialManager::openedBackpack);
    }

    // What is placed in the worlds: mines, spawners, bosses, traders, portals, build and safe zones
    private void createWorldSystems() {
        this.mineManager = new MineManager(this, new MineStore(db));
        this.spawnerManager = new SpawnerManager(this, new SpawnerStore(db, registry));
        this.bossManager = new BossManager(this);
        this.traderManager = new TraderManager(this, new TraderStore(db));
        this.wandManager = new WandManager();
        this.portalManager = new PortalManager(new PortalStore(db));
        this.buildManager = new BuildManager(db, new BuildStore(db), this::getMainSpawn);
        this.safeZoneManager = new SafeZoneManager(db, new SafeZoneStore(db));
        this.padManager = new PadManager(db, new PadStore(db), accessManager);
        this.fishingManager = new FishingManager(registry, db, new FishingStore(db), backpackManager, collectionManager);
    }

    // The Origin Isles questline after the tutorial, it walks players to the traders
    private void createQuests() {
        this.questManager = new QuestManager(this, db, new QuestStore(db), players, scoreboard, cosmeticManager,
                upgradeShop, traderManager, tutorialManager);
        tutorialManager.setOnEnd(questManager::begin);
        upgradeShop.setOnOpened((player, zoneId) -> {
            tutorialManager.openedUpgrades(player, zoneId);
            questManager.openedUpgrades(player, zoneId);
        });
        upgradeShop.setOnBought((player, zoneId) -> {
            tutorialManager.bought(player, zoneId);
            questManager.bought(player, zoneId);
        });
        questManager.start();
    }

    // The gauntlet, a tower mode in a world of its own. Its mobs on each floor are
    // variants of the ones in mobs/gauntlet.yml, made when first asked for
    private void createGauntlet() {
        registry.setMobVariants(id -> registry.getGauntlet().variant(id, registry::getMob));
        this.gauntletManager = new GauntletManager(this, db, new GauntletStore(db), players, scoreboard,
                cosmeticManager, buildManager);
        this.gauntletRuns = new GauntletRuns(this, gauntletManager, spawnerManager);
        gauntletManager.setRuns(gauntletRuns);
        gauntletManager.start();
        gauntletRuns.start();
    }

    // The main and experimental loadouts, after everything that keeps per-player data
    private void createLoadouts() {
        List<LoadoutData> holders = List.of(backpackManager, toolManager, rankManager, cosmeticManager,
                perkManager, collectionManager, armoryManager, accessManager, tutorialManager, questManager, fishingManager);
        this.loadoutManager = new LoadoutManager(registry, db, new LoadoutStore(db), players, buildManager,
                toolManager, holders, player -> {
                    scoreboard.setupBoard(player);
                    levelManager.show(player);
                    tabManager.updateAll();
                    staffPermissions.update(player);
                }, tutorialManager::begin);
        // Leaderboards only count main loadouts, an experimental one in play is left out
        players.setRanked(uuid -> loadoutManager.activeOf(uuid) == Loadout.MAIN);
    }

    // What the ACTION buttons in the menu files do. Looked up when clicked, see MenuActions
    private void registerMenuActions() {
        registry.registerAction("collections", collectionManager::open);
        // Gear lives in Collections, this is its tab
        registry.registerAction("armory", collectionManager::openGear);
        registry.registerAction("ranks", rankManager::openRanks);
        registry.registerAction("customise", cosmeticManager::openHub);
        // Fishing: the Fish Merchant, and each spot's trader opens fishing:<spot>
        registry.registerAction("fish_merchant", player ->
                new FishMerchantMenu(registry, fishingManager, backpackManager, players).open(player));
        registry.registerAction("fishing", (player, spot) -> {
            if (!registry.getFishingSpots().containsKey(spot)) TextUtils.error(player, "There is no fishing here yet.");
            else new RodShopMenu(registry, fishingManager, backpackManager, spot).open(player);
        });
        // The Essence shop is back with utility only; Vitality and Crit Rate are no longer
        // sold, levels already bought still work
        registry.registerAction("shop", player -> new RankShopMenu(this).open(player));
        // Warps are a supporter perk: donor and staff ranks, and admins
        registry.registerAction("warps", player -> {
            if (rankManager.isSupporter(player) || player.hasPermission("aurora.admin")) new WarpMenu(this).open(player);
            else TextUtils.error(player, "Warps are for supporters, see the store for a donor rank");
        });
        registry.registerAction("howto", HowToPlay::send);
        registry.registerAction("upgrades", upgradeShop::open);
        registry.registerAction("tools", toolManager::giveStarters);
        // The Core lives in Golem's Rest and needs every boss, it stays shut until that is released
        registry.registerAction("core", player -> {
            if (registry.getRegions().isZoneReleased("golems_rest") || player.hasPermission("aurora.admin")) OverworldCore.open(this, player);
            else TextUtils.error(player, "The Overworld Core is not open yet");
        });
        registry.registerAction("exchange", player -> EssenceExchange.open(this, player));
        registry.registerAction("leaderboard", player -> LeaderboardMenu.open(registry, players, player));
        registry.registerAction("gauntlet", player -> {
            String problem = GauntletCommand.open(gauntletManager, gauntletRuns, player);
            if (problem != null) TextUtils.error(player, problem);
        });
    }

    // Each one is declared in plugin.yml
    private void registerCommands() {
        this.trash = new TrashCommand(registry);
        this.dashListener = new DashListener(registry, perkManager);
        this.suggestionManager = new SuggestionManager(this, db, new SuggestionStore(db));
        command("aurora", new AuroraCommand(this));
        command("spawn", new SpawnCommand(this));
        command("backpack", new BackpackCommand(backpackManager));
        command("dump", new DumpCommand(this));
        ModerationCommand moderation = new ModerationCommand(moderationManager);
        for (String name : List.of("mute", "unmute", "kick", "ban", "unban", "history")) {
            command(name, moderation);
            PluginCommand registered = getCommand(name);
            if (registered != null) registered.setTabCompleter(moderation);
        }
        command("refresh", new RefreshCommand(this));
        command("ranks", new OpenMenuCommand(rankManager::openRanks));
        command("customise", new OpenMenuCommand(cosmeticManager::openHub));
        command("leaderboard", new TopCommand(registry, players));
        command("trash", trash);
        command("collections", new OpenMenuCommand(collectionManager::open));
        command("tutorial", new TutorialCommand(tutorialManager));
        command("gauntlet", new GauntletCommand(gauntletManager, gauntletRuns));
        command("shop", new OpenMenuCommand(player -> new RankShopMenu(this).open(player)));
        command("dash", dashListener);
        command("discord", new DiscordCommand());
        command("suggest", new SuggestCommand(suggestionManager));
    }

    private void registerListeners() {
        List<Listener> listeners = List.of(
                new MineListener(this),
                new BlockUseListener(this),
                new PlayerSessionListener(this, players, scoreboard, tabManager, levelManager),
                levelManager,
                rankManager,
                cosmeticManager,
                toolManager,
                perkManager,
                new MobDeathListener(this),
                new LeftoverMobListener(),
                new BowListener(),
                new TraderListener(this),
                new WandListener(this),
                new SpawnListener(this),
                tutorialManager,
                questManager,
                gauntletManager,
                gauntletRuns,
                accessManager,
                buildManager,
                safeZoneManager,
                new KillStreakListener(),
                padManager,
                fishingManager,
                staffPermissions,
                moderationManager,
                trash,
                new PortalListener(this),
                new KeyItemListener(this),
                new GrappleListener(this),
                dashListener,
                new DoubleJumpListener(perkManager),
                mountManager,
                new BackpackItemListener(this),
                new ChatShowcase(),
                new ServerMessages(registry, cosmeticManager, toolManager::giveStarters),
                new CombatListener(this),
                bossManager,
                new PagedMenuListener(),
                new CommandFilter(),
                // Last, after the join spawn, the first-join kit and the build world inventory
                loadoutManager);
        for (Listener listener : listeners) {
            getServer().getPluginManager().registerEvents(listener, this);
        }
    }

    private void command(String name, CommandExecutor executor) {
        PluginCommand command = getCommand(name);
        if (command == null) {
            getLogger().warning("Command '" + name + "' is missing from plugin.yml, it will not work");
            return;
        }
        command.setExecutor(executor);
    }

    public SpawnerManager getSpawnerManager() {
        return spawnerManager;
    }

    public MineManager getMineManager() {
        return mineManager;
    }

    public PlayerManager getPlayerManager() {
        return players;
    }

    public ScoreboardManager getScoreboardManager() {
        return scoreboard;
    }

    public TraderManager getTraderManager() {
        return traderManager;
    }

    public WandManager getWandManager() {
        return wandManager;
    }

    public WorldManager getWorldManager() {
        return worldManager;
    }

    public PortalManager getPortalManager() {
        return portalManager;
    }

    public BackpackManager getBackpackManager() {
        return backpackManager;
    }

    public LevelManager getLevelManager() {
        return levelManager;
    }

    public RankManager getRankManager() {
        return rankManager;
    }

    public CollectionManager getCollectionManager() {
        return collectionManager;
    }

    public ToolManager getToolManager() {
        return toolManager;
    }

    public UpgradeShop getUpgradeShop() {
        return upgradeShop;
    }

    public CosmeticManager getCosmeticManager() {
        return cosmeticManager;
    }

    public MountManager getMountManager() {
        return mountManager;
    }

    public PadManager getPadManager() {
        return padManager;
    }

    public FishingManager getFishingManager() {
        return fishingManager;
    }

    public SafeZoneManager getSafeZoneManager() {
        return safeZoneManager;
    }

    public DashListener getDashListener() {
        return dashListener;
    }

    public BuildManager getBuildManager() {
        return buildManager;
    }

    public AccessManager getAccessManager() {
        return accessManager;
    }

    public GauntletManager getGauntletManager() {
        return gauntletManager;
    }

    public QuestManager getQuestManager() {
        return questManager;
    }

    public TutorialManager getTutorialManager() {
        return tutorialManager;
    }

    public LoadoutManager getLoadoutManager() {
        return loadoutManager;
    }

    public SuggestionManager getSuggestionManager() {
        return suggestionManager;
    }

    public PerkManager getPerkManager() {
        return perkManager;
    }

    public ArmoryManager getArmoryManager() {
        return armoryManager;
    }

    public DataRegistry getRegistry() {
        return registry;
    }

    // The spawn set with /aurora setspawn, null for a world without one
    public Location getSpawn(World world) {
        return spawnMap.get(world);
    }

    public void setSpawn(Location location) {
        spawnMap.put(location.getWorld(), location);
        spawnStore.save(location);
    }

    // The world players join in, its spawn set with /aurora setspawn main
    public void setMainSpawn(Location location) {
        setSpawn(location);
        spawnStore.setMain(location.getWorld());
        mainWorld = location.getWorld().getName();
    }

    // Where a player joining lands: the main world's spawn, or its own if none was set
    public Location getMainSpawn() {
        World world = Bukkit.getWorld(mainWorld);
        if (world == null) return null;
        Location spawn = getSpawn(world);
        return spawn != null ? spawn : world.getSpawnLocation();
    }

    // Every manager is checked, enabling may have failed before it was made
    @Override
    public void onDisable() {
        // Saves queued so far land first, a loadout switch's swap among them, then the
        // saves below
        if (db != null) db.flush();

        int count = 0;
        for (World world : Bukkit.getWorlds()) {
            for (Entity entity : world.getEntities()) {
                if (LeftoverMobListener.isSpawned(entity)) {
                    entity.remove();
                    count++;
                }
            }
        }

        getLogger().info("Purged " + count + " mobs before shutdown.");
        if (mineManager != null) mineManager.shutdown();
        if (players != null) players.shutdown();
        if (bossManager != null) bossManager.shutdown();
        if (spawnerManager != null) spawnerManager.shutdown();
        if (traderManager != null) traderManager.shutdown();
        if (backpackManager != null) backpackManager.shutdown();
        if (rankManager != null) rankManager.shutdown();
        if (cosmeticManager != null) cosmeticManager.shutdown();
        if (tutorialManager != null) tutorialManager.shutdown();
        if (questManager != null) questManager.shutdown();
        if (gauntletRuns != null) gauntletRuns.shutdown();
        if (gauntletManager != null) gauntletManager.shutdown();
        if (mountManager != null) mountManager.shutdown();
        if (collectionManager != null) collectionManager.shutdown();

        // Last, so the saves above and any still queued finish first
        try {
            if (db != null) db.close();
        } catch (SQLException e) {
            getLogger().log(Level.SEVERE, "Could not close the database", e);
        }

        getLogger().info("AuroraMines shutting down...");
    }
}
