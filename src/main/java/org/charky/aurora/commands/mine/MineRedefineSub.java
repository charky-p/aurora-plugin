package org.charky.aurora.commands.mine;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.charky.aurora.Aurora;
import org.charky.aurora.commands.AuroraSubCommand;
import org.charky.aurora.util.TextUtils;

import java.util.List;

public class MineRedefineSub extends MineSub {
    public MineRedefineSub(Aurora plugin) {
        super(plugin);
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        // TODO: Implement this properly, there r 2 redefines, manual and with wand
        return;
//        if (!(sender instanceof Player player)) return;
//        if (args.length < 2) {
//            TextUtils.usage(sender, "mine redefine <name>");
//            return;
//        }
//
//        String name = args[1];
//        if (!mineManager.exists(name)) {
//            TextUtils.error(sender, "Mine not found.");
//            return;
//        }

//        if (!wandManager.hasBothPositions(player.getUniqueId())) {
//            TextUtils.error(sender, "Select two new points with your wand first!");
//            return;
//        }
//
//        // Update the existing object
//        manager.redefineMine(name, wandManager.getPos1(player.getUniqueId()), wandManager.getPos2(player.getUniqueId()));
//
//        TextUtils.send(sender, "Redefined boundaries for <white>" + name);
//        manager.resetMine(name);
    }

    @Override
    public List<String> tabComplete(Player player, String[] args) {
        return List.of();
    }

    @Override
    public String getUsage() {
        return "TODO";
    }

    @Override
    public String getName() {
        return "redefine";
    }

    @Override
    public String getDescription() {
        return "TODO";
    }
}
