package org.charky.aurora.core;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
//import org.charky.aurora.features.mining.BlockPresets;
import org.charky.aurora.common.loot.InventoryManager;
import org.charky.aurora.common.util.DataKeys;
import org.charky.aurora.features.system.VoidGenerator;
import org.charky.aurora.features.system.commands.AuroraCommand;
import org.charky.aurora.features.system.commands.GiveCustomsCommand;
import org.charky.aurora.features.system.commands.HealCommand;
import org.charky.aurora.features.player.ScoreboardListener;
import org.charky.aurora.features.player.ScoreboardManager;
import org.charky.aurora.features.player.TabManager;
import org.charky.aurora.features.spawner.WeaponListener;
import org.charky.aurora.features.system.commands.SpawnCommand;
import org.charky.aurora.features.system.SpawnListener;
import org.charky.aurora.features.system.WandListener;
import org.charky.aurora.features.system.WandManager;
import org.charky.aurora.features.mine.MineListener;
import org.charky.aurora.features.mine.MineManager;
import org.charky.aurora.features.spawner.MobDeathListener;
import org.charky.aurora.features.spawner.SpawnerManager;
import org.charky.aurora.features.trader.TraderListener;
import org.charky.aurora.features.trader.TraderManager;
import org.charky.aurora.common.util.ItemFactory;

import java.sql.SQLException;
import java.util.Map;

public class Aurora extends JavaPlugin implements Listener {
    private Database db;
    private MineManager manager;
    private ScoreboardManager scoreboard;
    private SpawnerManager spawnerManager;
    private TabManager tabManager;
    private TraderManager traderManager;
    private InventoryManager inventoryManager;
    private WandManager wandManager;
    private ItemFactory itemFactory = new ItemFactory();
    private DataRegistry registry;
    private Map<World, Location> spawnMap;

    @Override
    public void onEnable() {
        WorldCreator creator = new WorldCreator("aurora_mines");
        creator.generator(new VoidGenerator());
        creator.generateStructures(false);

        // This actually 'wakes up' the world so it shows in /execute
        Bukkit.createWorld(creator);

        // Register preset data
        this.registry = new DataRegistry(this);
        registry.loadAll();

        // Database setup
        if (!getDataFolder().exists()) {
            getDataFolder().mkdirs();
        }

        db = new Database(this, getDataFolder().getAbsolutePath());
        db.setup();
        getLogger().info("SQLite Database setup complete!");

        // Spawn map
        this.spawnMap = db.getSpawns();

        // Managers
        this.tabManager = new TabManager(this);
        this.scoreboard = new ScoreboardManager(this, db, tabManager);
        this.manager = new MineManager(this, db, scoreboard);
        this.spawnerManager = new SpawnerManager(this, db);
        this.traderManager = new TraderManager(this);
        this.inventoryManager = new InventoryManager();
        this.wandManager = new WandManager();

        // Plugin startup logic
        if (this.getCommand("refresh") != null) {
            this.getCommand("refresh").setExecutor(this);
        }

        if (this.getCommand("getCustoms") != null) {
            this.getCommand("getCustoms").setExecutor(new GiveCustomsCommand());
        }

        if (this.getCommand("heal") != null) {
            this.getCommand("heal").setExecutor(new HealCommand());
        }

        if (this.getCommand("aurora") != null) {
            this.getCommand("aurora").setExecutor(new AuroraCommand(this));
        }

        if (this.getCommand("spawn") != null) {
            this.getCommand("spawn").setExecutor(new SpawnCommand(this));
        }

        // Listeners
        getServer().getPluginManager().registerEvents(new WeaponListener(), this);
        getServer().getPluginManager().registerEvents(new MineListener(this), this);
        getServer().getPluginManager().registerEvents(new ScoreboardListener(scoreboard), this);
        getServer().getPluginManager().registerEvents(new MobDeathListener(this), this);
        getServer().getPluginManager().registerEvents(new TraderListener(this), this);
        getServer().getPluginManager().registerEvents(new WandListener(this), this);
        getServer().getPluginManager().registerEvents(new SpawnListener(this), this);
        getServer().getPluginManager().registerEvents(this, this);

        for (World world : Bukkit.getWorlds()) {
            cleanupMobs(world);
        }

        // Mines setup
        getLogger().info("AuroraMines has been enabled!");
    }

    public SpawnerManager getMobManager() {
        return this.spawnerManager;
    }

    public MineManager getMineManager() {
        return this.manager;
    }

    public ScoreboardManager getScoreboardManager() {
        return scoreboard;
    }

    public TraderManager getTraderManager() {
        return this.traderManager;
    }

    public InventoryManager getInventoryManager() {
        return inventoryManager;
    }

    public WandManager getWandManager() {
        return wandManager;
    }

    public DataRegistry getRegistry() {
        return registry;
    }

    public ItemFactory getItemFactory() {
        return itemFactory;
    }

    public Location getSpawn(World world) {
        if (spawnMap.containsKey(world)) return spawnMap.get(world);
        return null;
    }

    public void setSpawn(Location location) {
        spawnMap.put(location.getWorld(), location);
        db.saveSpawn(location);
    }

    private void cleanupMobs(World world) {
        // Refresh spawned mobs
        int count = 0;
        for (Entity entity : world.getEntities()) {
            if (entity.getPersistentDataContainer().has(DataKeys.SPAWNER_KEY, PersistentDataType.STRING)) {
                entity.remove();
                count++;
            }
        }
        if (count > 0) {
            getLogger().info("Removed " + count + " orphaned mobs from " + world.getName());
        }
    }

    @EventHandler
    public void onWorldLoad(WorldLoadEvent event) {
        cleanupMobs(event.getWorld());
    }

    @Override
    public void onDisable() {
        if (manager != null) manager.shutdown();
        if (scoreboard != null) scoreboard.shutdown();
        if (spawnerManager != null) spawnerManager.shutdown();

        try {
            db.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }

        getLogger().info("AuroraMines shutting down...");
    }
}
