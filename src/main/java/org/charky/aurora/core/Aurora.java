package org.charky.aurora.core;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.charky.aurora.common.menu.PagedMenuListener;
import org.charky.aurora.common.util.DataKeys;
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
import org.charky.aurora.core.database.TraderStore;
import org.charky.aurora.core.database.UnlockStore;
import org.charky.aurora.core.database.WorldStore;
import org.charky.aurora.features.armory.ArmoryManager;
import org.charky.aurora.features.boss.BossManager;
import org.charky.aurora.features.collection.CollectionManager;
import org.charky.aurora.features.combat.CombatListener;
import org.charky.aurora.features.cosmetic.CosmeticManager;
import org.charky.aurora.features.combat.SustenanceTask;
import org.charky.aurora.features.keys.KeyItemListener;
import org.charky.aurora.features.mine.MineListener;
import org.charky.aurora.features.mine.MineManager;
import org.charky.aurora.features.player.BackpackCommand;
import org.charky.aurora.features.player.BackpackItemListener;
import org.charky.aurora.features.player.BackpackManager;
import org.charky.aurora.features.player.ChatShowcase;
import org.charky.aurora.features.player.LevelManager;
import org.charky.aurora.features.player.PlayerManager;
import org.charky.aurora.features.player.PlayerSessionListener;
import org.charky.aurora.features.player.ScoreboardManager;
import org.charky.aurora.features.player.ServerMessages;
import org.charky.aurora.features.player.TabManager;
import org.charky.aurora.features.portal.PortalListener;
import org.charky.aurora.features.portal.PortalManager;
import org.charky.aurora.features.perk.PerkManager;
import org.charky.aurora.features.cosmetic.Cosmetic;
import org.charky.aurora.features.cosmetic.Cosmetics;
import org.charky.aurora.features.system.HowToPlay;
import org.charky.aurora.features.player.PlayerData;
import org.charky.aurora.features.system.CommandFilter;
import org.charky.aurora.features.rank.RankManager;
import org.charky.aurora.features.spawner.LeftoverMobListener;
import org.charky.aurora.features.spawner.MobDeathListener;
import org.charky.aurora.features.spawner.SpawnerManager;
import org.charky.aurora.features.system.SpawnListener;
import org.charky.aurora.features.system.VoidGenerator;
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
    // The main world, everything in it is built by hand
    public static final String MAIN_WORLD = "aurora_mines";

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
    private ToolManager toolManager;
    private DataRegistry registry;
    private Map<World, Location> spawnMap = new HashMap<>();

    @Override
    public void onEnable() {
        WorldCreator creator = new WorldCreator(MAIN_WORLD);
        creator.generator(new VoidGenerator());
        creator.generateStructures(false);

        // This actually 'wakes up' the world so it shows in /execute
        Bukkit.createWorld(creator);

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

        // Managers
        this.players = new PlayerManager(this, db, new PlayerStore(db));
        this.scoreboard = new ScoreboardManager(players);
        TabManager tabManager = new TabManager(players);
        this.levelManager = new LevelManager(this, players, scoreboard);
        this.rankManager = new RankManager(this, db, new RankStore(db), players, scoreboard, tabManager);
        this.collectionManager = new CollectionManager(this, registry, db, new CollectionStore(db), players, scoreboard);
        registry.registerAction("collections", collectionManager::open);
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
        //  - the Essence shop (RankShopMenu: perks and the Essence ranks), /shop and its menu icon
        //  - the gauntlet (features/gauntlet), /gauntlet and its listener
        //  - fishing (features/fishing), its listener and the rods in gear_other.csv
        // Perks already bought still work, PerkManager stays for them and for /a reset
        registry.registerAction("warps", player -> new WarpMenu(this).open(player));
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
        this.mineManager = new MineManager(this, new MineStore(db));
        this.spawnerManager = new SpawnerManager(this, new SpawnerStore(db, registry));
        this.bossManager = new BossManager(this);
        this.traderManager = new TraderManager(this, new TraderStore(db));
        this.wandManager = new WandManager();
        this.portalManager = new PortalManager(new PortalStore(db));

        // Commands, each one is declared in plugin.yml
        command("aurora", new AuroraCommand(this));
        command("spawn", new SpawnCommand(this));
        command("backpack", new BackpackCommand(backpackManager));
        command("dump", new DumpCommand(this));
        command("refresh", new RefreshCommand(this));
        command("ranks", new OpenMenuCommand(rankManager::openRanks));
        command("customise", new OpenMenuCommand(cosmeticManager::openHub));
        command("leaderboard", new TopCommand(players));
        command("collections", new OpenMenuCommand(collectionManager::open));

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
                new PortalListener(this),
                new KeyItemListener(this),
                new BackpackItemListener(this),
                new ChatShowcase(),
                new ServerMessages(registry, cosmeticManager),
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
