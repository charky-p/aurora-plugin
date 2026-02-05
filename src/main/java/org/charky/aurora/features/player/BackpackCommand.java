package org.charky.aurora.features.player;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.charky.aurora.common.util.TextUtils;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public class BackpackCommand implements CommandExecutor {
    private final BackpackManager backpackManager;

    public BackpackCommand(BackpackManager backpackManager) {
        this.backpackManager = backpackManager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String s, @NotNull String @NotNull [] args) {
        if (!(sender instanceof Player player)) {
            return false;
        }

        BackpackData backpack = backpackManager.get(player.getUniqueId());
        if (backpack == null) {
            TextUtils.error(player, "Something went wrong while trying to open the backpack.");
            return true;
        }

        backpack.openInventory(player);
        TextUtils.success(player, "Backpack opened.");
        return true;
    }
}
