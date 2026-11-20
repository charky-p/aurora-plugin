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
import org.charky.aurora.core.database.SpawnStore;
import org.charky.aurora.core.database.SpawnerStore;
import org.charky.aurora.core.database.ToolStore;
import org.charky.aurora.core.database.TutorialStore;
import org.charky.aurora.core.database.TraderStore;
import org.charky.aurora.core.database.UnlockStore;
import org.charky.aurora.core.database.WorldStore;
import org.charky.aurora.features.armory.ArmoryManager;
import org.charky.aurora.features.boss.BossManager;
import org.charky.aurora.features.collection.CollectionManager;
import org.charky.aurora.features.combat.CombatListener;
import org.charky.aurora.features.core.EssenceExchange;
import org.charky.aurora.features.core.OverworldCore;
import org.charky.aurora.features.cosmetic.CosmeticManager;
import org.charky.aurora.features.combat.SustenanceTask;
import org.charky.aurora.features.keys.AccessManager;
import org.charky.aurora.features.build.BuildManager;
import org.charky.aurora.features.pvp.SafeZoneManager;
import org.charky.aurora.features.keys.KeyItemListener;
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
import org.charky.aurora.features.player.PlayerData;
import org.charky.aurora.features.system.CommandFilter;
import org.charky.aurora.features.rank.RankManager;
import org.charky.aurora.features.rank.RankShopMenu;
import org.charky.aurora.features.spawner.LeftoverMobListener;
import org.charky.aurora.features.spawner.MobDeathListener;
import org.charky.aurora.features.spawner.SpawnerManager;
import org.charky.aurora.features.system.SpawnListener;
import org.charky.aurora.features.tutorial.TutorialCommand;
import org.charky.aurora.features.tutorial.TutorialManager;
import org.charky.aurora.features.system.WandListener;
import org.charky.aurora.features.system.WandManager;
import org.charky.aurora.features.system.commands.AuroraCommand;
import org.charky.aurora.features.system.commands.DumpCommand;
import org.charky.aurora.features.system.commands.OpenMenuCommand;
import org.charky.aurora.features.system.commands.RefreshCommand;
import org.charky.aurora.features.system.commands.SpawnCommand;
import org.charky.aurora.features.system.commands.TopCommand;
import org.charky.aurora.features.tool.ToolManager;
import org.charky.aurora.features.trader.TraderListener;
import org.charky.aurora.features.warp.WarpMenu;
import org.charky.aurora.features.trader.TraderManager;
import org.charky.aurora.features.world.WorldManager;

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
    private AccessManager accessManager;
    private BuildManager buildManager;
    private DashListener dashListener;
    private SafeZoneManager safeZoneManager;
    private MountManager mountManager;
    private ToolManager toolManager;
    private DataRegistry registry;
    private Map<World, Location> spawnMap = new HashMap<>();
    private String mainWorld = MAIN_WORLD;

    @Override
    public void onEnable() {
        // Register preset data
        this.registry = new DataRegistry(this);
        this.backpackManager = new BackpackManager(registry);
        registry.setBackpackManager(backpackManager);
        this.armoryManager = new ArmoryManager(registry);
        registry.setArmoryManager(armoryManager);
        registry.loadAll();

        // Database setup
        if (!getDataFolder().exists()) {
            getDataFolder().mkdirs();
        }

        db = new Database(this, getDataFolder().getAbsolutePath());
        Schema.create(db);
        getLogger().info("SQLite Database setup complete!");

        // One store per table, each manager only gets the one it needs
        this.spawnStore = new SpawnStore(db);
        backpackManager.loadBackpacks(db, new BackpackStore(db));
        backpackManager.startAutosave(this);
        armoryManager.load(db, new UnlockStore(db));
        armoryManager.migrateFrom(backpackManager);

        // Extra worlds first, spawns and portals look worlds up by name
        this.worldManager = new WorldManager(this, new WorldStore(db));
        worldManager.loadAll();

        this.spawnMap = spawnStore.loadAll();
        String savedMain = spawnStore.loadMain();
        if (savedMain != null && Bukkit.getWorld(savedMain) != null) mainWorld = savedMain;
        else if (savedMain != null) getLogger().warning("Main world '" + savedMain + "' is not loaded, players join in " + MAIN_WORLD);

        // Managers
        this.players = new PlayerManager(this, db, new PlayerStore(db));
        this.scoreboard = new ScoreboardManager(players);
        TabManager tabManager = new TabManager(players);
        this.levelManager = new LevelManager(this, players, scoreboard);
        this.rankManager = new RankManager(this, db, new RankStore(db), players, scoreboard, tabManager);
        this.collectionManager = new CollectionManager(this, registry, db, new CollectionStore(db), players, scoreboard);
        registry.registerAction("collections", collectionManager::open);
        // Gear lives in Collections, this is its tab
        registry.registerAction("armory", collectionManager::openGear);
        this.cosmeticManager = new CosmeticManager(this, db, new CosmeticStore(db), players, rankManager, scoreboard, tabManager);
        registry.registerAction("ranks", rankManager::openRanks);
        registry.registerAction("customise", cosmeticManager::openHub);
        this.perkManager = new PerkManager(this, db, new PerkStore(db), players, scoreboard);
        armoryManager.setKeyring(perkManager::hasKeyring);
        armoryManager.setEssence((player, amount) -> {
            PlayerData data = players.get(player);
            if (data == null) return;
            data.addEssence(amount);
            scoreboard.updateBoard(player);
        });
        // TODO: later. Switched off until they are finished, the code is all still here:
        //  - the gauntlet (features/gauntlet), /gauntlet and its listener
        //  - fishing (features/fishing), its listener and the rods in gear_other.csv
        // The Essence shop is back with utility only; Vitality and Crit Rate are no longer
        // sold, levels already bought still work
        registry.registerAction("shop", player -> new RankShopMenu(this).open(player));
        this.mountManager = new MountManager(this, registry, perkManager);
        // Warps are a supporter perk: donor and staff ranks, and admins
        registry.registerAction("warps", player -> {
            if (rankManager.isSupporter(player) || player.hasPermission("aurora.admin")) new WarpMenu(this).open(player);
            else TextUtils.error(player, "Warps are for supporters, see the store for a donor rank");
        });
        this.toolManager = new ToolManager(this, db, new ToolStore(db));
        armoryManager.setTools(toolManager);
        cosmeticManager.setMaxed((player, cosmetic) -> cosmetic.source() == Cosmetic.Source.ZONE
                ? toolManager.maxed(player, cosmetic.target())
                : Cosmetics.regions(cosmetic).stream().allMatch(region -> toolManager.maxedRegion(player, region)));
        toolManager.setOnMaxed(cosmeticManager::announceMaxed);
        rankManager.setOnPlaytime(cosmeticManager::playtimePassed);
        registry.registerAction("howto", HowToPlay::send);
        toolManager.setTrims(cosmeticManager::trim);
        cosmeticManager.setLookChanged(toolManager::refresh);
        registry.registerAction("upgrades", toolManager::openUpgrades);
        registry.registerAction("tools", toolManager::giveStarters);
        // The Core lives in Golem's Rest and needs every boss, it stays shut until that is released
        registry.registerAction("core", player -> {
            if (registry.isZoneReleased("golems_rest") || player.hasPermission("aurora.admin")) OverworldCore.open(this, player);
            else TextUtils.error(player, "The Overworld Core is not open yet");
        });
        registry.registerAction("exchange", player -> EssenceExchange.open(this, player));
        this.accessManager = new AccessManager(db, new AccessStore(db), registry);
        this.tutorialManager = new TutorialManager(this, db, new TutorialStore(db), players, scoreboard, cosmeticManager);
        backpackManager.setOnOpen(tutorialManager::openedBackpack);
        toolManager.setOnOpened(tutorialManager::openedUpgrades);
        toolManager.setOnBought(tutorialManager::bought);
        this.mineManager = new MineManager(this, new MineStore(db));
        this.spawnerManager = new SpawnerManager(this, new SpawnerStore(db, registry));
        this.bossManager = new BossManager(this);
        this.traderManager = new TraderManager(this, new TraderStore(db));
        this.wandManager = new WandManager();
        this.portalManager = new PortalManager(new PortalStore(db));
        this.buildManager = new BuildManager(db, new BuildStore(db), this::getMainSpawn);
        this.safeZoneManager = new SafeZoneManager(db, new SafeZoneStore(db));

        // Commands, each one is declared in plugin.yml
        command("aurora", new AuroraCommand(this));
        command("spawn", new SpawnCommand(this));
        command("backpack", new BackpackCommand(backpackManager));
        command("dump", new DumpCommand(this));
        command("refresh", new RefreshCommand(this));
        command("ranks", new OpenMenuCommand(rankManager::openRanks));
        command("customise", new OpenMenuCommand(cosmeticManager::openHub));
        command("leaderboard", new TopCommand(registry, players));
        registry.registerAction("leaderboard", player -> LeaderboardMenu.open(registry, players, player));
        TrashCommand trash = new TrashCommand(registry);
        command("trash", trash);
        command("collections", new OpenMenuCommand(collectionManager::open));
        command("tutorial", new TutorialCommand(tutorialManager));
        command("shop", new OpenMenuCommand(player -> new RankShopMenu(this).open(player)));
        this.dashListener = new DashListener(registry, perkManager);
        DashListener dash = dashListener;
        command("dash", dash);

        List<Listener> listeners = List.of(
                new MineListener(this),
                new PlayerSessionListener(this, players, scoreboard, tabManager, levelManager),
                levelManager,
                rankManager,
                cosmeticManager,
                toolManager,
                perkManager,
                new MobDeathListener(this),
                new LeftoverMobListener(),
                new TraderListener(this),
                new WandListener(this),
                new SpawnListener(this),
                tutorialManager,
                accessManager,
                buildManager,
                safeZoneManager,
                trash,
                new PortalListener(this),
                new KeyItemListener(this),
                new GrappleListener(this),
                dash,
                new DoubleJumpListener(perkManager),
                mountManager,
                new BackpackItemListener(this),
                new ChatShowcase(),
                new ServerMessages(registry, cosmeticManager, toolManager::giveStarters),
                new CombatListener(this),
                bossManager,
                new PagedMenuListener(),
                new CommandFilter());
        for (Listener listener : listeners) {
            getServer().getPluginManager().registerEvents(listener, this);
        }

        SustenanceTask.start(this);
        LeftoverMobListener.sweepLoaded();
        traderManager.startRespawning();

        getLogger().info("AuroraMines has been enabled!");
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

    public CosmeticManager getCosmeticManager() {
        return cosmeticManager;
    }

    public MountManager getMountManager() {
        return mountManager;
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

    public TutorialManager getTutorialManager() {
        return tutorialManager;
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
        int count = 0;
        for (World world : Bukkit.getWorlds()) {
            for (Entity entity : world.getEntities()) {
                if (entity.getPersistentDataContainer().has(DataKeys.SPAWNER_KEY, PersistentDataType.STRING)) {
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
