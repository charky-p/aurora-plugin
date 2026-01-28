package org.charky.aurora;

import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.plugin.java.JavaPlugin;
import org.charky.aurora.commands.*;
import org.charky.aurora.listeners.*;
import org.charky.aurora.managers.*;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

public final class Aurora extends JavaPlugin {
    private Database db;
    private MineManager manager;
    private ScoreboardManager scoreboard;
    private MobManager mobManager;
    private TabManager tabManager;
    private TraderManager traderManager;
    private InventoryManager inventoryManager;
    private Map<World, Location> spawnMap;

    public static NamespacedKey SPAWNER_ID_KEY;
    public static NamespacedKey MOB_ID_KEY;

    @Override
    public void onEnable() {
        // Spawner id key
        SPAWNER_ID_KEY = new NamespacedKey(this, "spawner_id");
        MOB_ID_KEY = new NamespacedKey(this, "mob_id");

        // Database setup
        if (!getDataFolder().exists()) {
            getDataFolder().mkdirs();
        }

        db = new Database(getDataFolder().getAbsolutePath());
        db.setup();
        getLogger().info("SQLite Database setup complete!");

        // Spawn map
        this.spawnMap = db.getSpawns();

        // Managers
        this.tabManager = new TabManager(this);
        this.scoreboard = new ScoreboardManager(this, db, tabManager);
        this.manager = new MineManager(this, db, scoreboard);
        this.mobManager = new MobManager(this, db);
        this.traderManager = new TraderManager(this);
        this.inventoryManager = new InventoryManager();

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

        if (this.getCommand("setspawn") != null) {
            this.getCommand("setspawn").setExecutor(new SetSpawnCommand(this));
        }

        // Listeners
        getServer().getPluginManager().registerEvents(new WeaponListener(), this);
        getServer().getPluginManager().registerEvents(new MineListener(manager, scoreboard, inventoryManager), this);
        getServer().getPluginManager().registerEvents(new ScoreboardListener(scoreboard), this);
        getServer().getPluginManager().registerEvents(new MobDeathListener(mobManager, inventoryManager), this);
        getServer().getPluginManager().registerEvents(new TraderListener(traderManager), this);
        getServer().getPluginManager().registerEvents(new WandListener(this), this);
        getServer().getPluginManager().registerEvents(new SpawnListener(this), this);

        // Mines setup
        getLogger().info("AuroraMines has been enabled!");
    }

    public MobManager getMobManager() {
        return this.mobManager;
    }

    public MineManager getMineManager() {
        return this.manager;
    }

    public TraderManager getTraderManager() {
        return this.traderManager;
    }

    public Location getSpawn(World world) {
        if (spawnMap.containsKey(world)) return spawnMap.get(world);
        return null;
    }

    public void setSpawn(Location location) {
        spawnMap.put(location.getWorld(), location);
        db.saveSpawn(location);
    }

    @Override
    public void onDisable() {
        if (manager != null) manager.shutdown();
        if (scoreboard != null) scoreboard.shutdown();
        if (mobManager != null) mobManager.shutdown();

        try {
            db.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }

        getLogger().info("AuroraMines shutting down...");
    }
}
