package org.charky.aurora.core;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Entity;
import org.bukkit.event.Listener;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.charky.aurora.common.menu.PagedMenuListener;
import org.charky.aurora.common.util.DataKeys;
import org.charky.aurora.common.util.TextUtils;
import org.charky.aurora.features.armory.ArmoryManager;
import org.charky.aurora.features.combat.CombatListener;
import org.charky.aurora.features.combat.SustenanceTask;
import org.charky.aurora.features.gauntlet.GauntletCommand;
import org.charky.aurora.features.gauntlet.GauntletListener;
import org.charky.aurora.features.gauntlet.GauntletManager;
import org.charky.aurora.features.player.*;
import org.charky.aurora.features.system.VoidGenerator;
import org.charky.aurora.features.system.commands.*;
import org.charky.aurora.features.spawner.WeaponListener;
import org.charky.aurora.features.system.SpawnListener;
import org.charky.aurora.features.system.WandListener;
import org.charky.aurora.features.system.WandManager;
import org.charky.aurora.features.mine.MineListener;
import org.charky.aurora.features.mine.MineManager;
import org.charky.aurora.features.spawner.LeftoverMobListener;
import org.charky.aurora.features.spawner.MobDeathListener;
import org.charky.aurora.features.spawner.SpawnerManager;
import org.charky.aurora.features.trader.TraderListener;
import org.charky.aurora.features.trader.TraderManager;
import org.charky.aurora.common.util.ItemFactory;

import java.sql.SQLException;
import org.charky.aurora.core.database.*;

import java.util.Map;
import java.util.logging.Level;

public class Aurora extends JavaPlugin implements Listener {
    private Database db;
    private SpawnStore spawnStore;
    private MineManager manager;
    private ScoreboardManager scoreboard;
    private SpawnerManager spawnerManager;
    private TabManager tabManager;
    private TraderManager traderManager;
    private WandManager wandManager;
    private BackpackManager backpackManager;
    private ArmoryManager armoryManager;
    private final ItemFactory itemFactory = new ItemFactory();
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

        // Spawn map
        this.spawnMap = spawnStore.loadAll();

        // Managers
        this.tabManager = new TabManager(this);
        this.scoreboard = new ScoreboardManager(this, db, new PlayerStore(db), tabManager);
        this.manager = new MineManager(this, new MineStore(db), scoreboard);
        this.spawnerManager = new SpawnerManager(this, new SpawnerStore(db, registry));
        this.traderManager = new TraderManager(this);
        this.wandManager = new WandManager();

        // temp
        GauntletManager gauntletManager = new GauntletManager(this);

        // Plugin startup logic, /refresh is registered further down
        if (this.getCommand("aurora") != null) {
            this.getCommand("aurora").setExecutor(new AuroraCommand(this));
        }

        if (this.getCommand("spawn") != null) {
            this.getCommand("spawn").setExecutor(new SpawnCommand(this));
        }

        if (this.getCommand("gauntlet") != null) {
            this.getCommand("gauntlet").setExecutor(new GauntletCommand(gauntletManager));
        }

        if (this.getCommand("backpack") != null) {
            this.getCommand("backpack").setExecutor(new BackpackCommand(backpackManager));
        }

        if (this.getCommand("dump") != null) {
            this.getCommand("dump").setExecutor(new DumpCommand(this));
        }

        if (this.getCommand("menu") != null) {
            this.getCommand("menu").setExecutor(new MenuCommand(this));
        }

        // Listeners
        getServer().getPluginManager().registerEvents(new WeaponListener(), this);
        getServer().getPluginManager().registerEvents(new MineListener(this), this);
        getServer().getPluginManager().registerEvents(new ScoreboardListener(scoreboard), this);
        getServer().getPluginManager().registerEvents(new MobDeathListener(this), this);
        getServer().getPluginManager().registerEvents(new LeftoverMobListener(), this);
        getServer().getPluginManager().registerEvents(new TraderListener(this), this);
        getServer().getPluginManager().registerEvents(new WandListener(this), this);
        getServer().getPluginManager().registerEvents(new SpawnListener(this), this);
        getServer().getPluginManager().registerEvents(new CombatListener(this), this);
        getServer().getPluginManager().registerEvents(this, this);

        getServer().getPluginManager().registerEvents(new GauntletListener(gauntletManager), this);
        getServer().getPluginManager().registerEvents(new PagedMenuListener(), this);

        SustenanceTask.start(this);
        LeftoverMobListener.sweepLoaded();

        // FIXME: temporary refresh command
        PluginCommand refresh = getCommand("refresh");
        if (refresh != null) {
            refresh.setExecutor((sender, cmd, label, args) -> {
                try {
                    registry.loadAll();
                    spawnerManager.reloadMobs();
                    TextUtils.success(sender, "Registry reloaded");
                } catch (Exception e) {
                    TextUtils.error(sender, "Reload failed: " + e.getMessage());
                    getLogger().log(Level.SEVERE, "Registry reload failed", e);
                }
                return true;
            });
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

    public WandManager getWandManager() {
        return wandManager;
    }

    public BackpackManager getBackpackManager() {
        return backpackManager;
    }

    public ArmoryManager getArmoryManager() {
        return armoryManager;
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
        spawnStore.save(location);
    }

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
        if (manager != null) manager.shutdown();
        if (scoreboard != null) scoreboard.shutdown();
        if (spawnerManager != null) spawnerManager.shutdown();
        if (backpackManager != null) backpackManager.shutdown();

        // Last, so the saves above and any still queued finish first
        try {
            if (db != null) db.close();
        } catch (SQLException e) {
            getLogger().log(Level.SEVERE, "Could not close the database", e);
        }

        getLogger().info("AuroraMines shutting down...");
    }
}
