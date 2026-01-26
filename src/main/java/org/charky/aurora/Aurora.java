package org.charky.aurora;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.charky.aurora.commands.*;
import org.charky.aurora.listeners.BlastListener;
import org.charky.aurora.listeners.MineListener;
import org.charky.aurora.listeners.ScoreboardListener;
import org.charky.aurora.listeners.WeaponListener;
import org.jetbrains.annotations.NotNull;

import java.sql.SQLException;

public final class Aurora extends JavaPlugin {
    private Database db;
    private MineManager manager;
    private ScoreboardListener scoreboard;

    @Override
    public void onEnable() {
        // Database setup
        if (!getDataFolder().exists()) {
            getDataFolder().mkdirs();
        }

        db = new Database(getDataFolder().getAbsolutePath());
        db.setup();
        getLogger().info("SQLite Database setup complete!");

        // Managers
        this.scoreboard = new ScoreboardListener(this, db);
        manager = new MineManager(this, db, scoreboard);

        // Plugin startup logic
        if (this.getCommand("hi") != null) {
            this.getCommand("hi").setExecutor(this);
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

        // Listeners
        getServer().getPluginManager().registerEvents(new BlastListener(manager), this);
        getServer().getPluginManager().registerEvents(new WeaponListener(), this);
        getServer().getPluginManager().registerEvents(new MineListener(manager), this);
        getServer().getPluginManager().registerEvents(scoreboard, this);

        // Mines setup
        getLogger().info("AuroraMines has been enabled!");
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {

        if (sender instanceof Player player) {
            player.sendMessage("§b[Aurora] §fHello, §b" + player.getName() + "§f! Welcome to the mines.");
        } else {
            sender.sendMessage("Console says hello back!");
        }

        return true;
    }

    @Override
    public void onDisable() {
        if (manager != null) manager.shutdown();
        if (scoreboard != null) scoreboard.shutdown();

        try {
            db.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }

        getLogger().info("AuroraMines shutting down...");
    }
}
