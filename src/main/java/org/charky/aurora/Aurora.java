package org.charky.aurora;

import org.bukkit.NamespacedKey;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.charky.aurora.commands.*;
import org.charky.aurora.listeners.MineListener;
import org.charky.aurora.listeners.MobDeathListener;
import org.charky.aurora.listeners.ScoreboardListener;
import org.charky.aurora.listeners.WeaponListener;
import org.jetbrains.annotations.NotNull;

import java.sql.SQLException;

public final class Aurora extends JavaPlugin {
    private Database db;
    private MineManager manager;
    private ScoreboardListener scoreboard;
    private MobManager mobManager;

    public static NamespacedKey SPAWNER_ID_KEY;

    @Override
    public void onEnable() {
        // Spawner id key
        SPAWNER_ID_KEY = new NamespacedKey(this, "spawner_id");

        // Database setup
        if (!getDataFolder().exists()) {
            getDataFolder().mkdirs();
        }

        db = new Database(getDataFolder().getAbsolutePath());
        db.setup();
        getLogger().info("SQLite Database setup complete!");

        // Managers
        this.scoreboard = new ScoreboardListener(this, db);
        this.manager = new MineManager(this, db, scoreboard);
        this.mobManager = new MobManager(this, db);

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

        if (this.getCommand("mine") != null) {
            this.getCommand("mine").setExecutor(new MineCommand(manager));
        }

        if (this.getCommand("mobspawner") != null) {
            this.getCommand("mobspawner").setExecutor(new MobCommand(mobManager));
        }

        // Listeners
        getServer().getPluginManager().registerEvents(new WeaponListener(), this);
        getServer().getPluginManager().registerEvents(new MineListener(manager, scoreboard), this);
        getServer().getPluginManager().registerEvents(scoreboard, this);
        getServer().getPluginManager().registerEvents(new MobDeathListener(mobManager), this);

        // Mines setup
        getLogger().info("AuroraMines has been enabled!");
    }

    public MobManager getMobManager() {
        return this.mobManager;
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
