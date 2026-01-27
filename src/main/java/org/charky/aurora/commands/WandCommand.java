package org.charky.aurora.commands;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.charky.aurora.Aurora;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class WandCommand implements AuroraSubCommand {
    private final Aurora plugin;

    public WandCommand(Aurora plugin) {
        this.plugin = plugin;
    }

    @Override
    public void execute(@NotNull CommandSender sender, @NotNull String @NotNull [] args) {
        if (!(sender instanceof Player player)) return;

        giveWand(player);
    }

    public void giveWand(Player player) {
        ItemStack wand = new ItemStack(Material.BLAZE_ROD);
        wand.editMeta(meta -> {
            meta.displayName(Component.text("Entity Copier Wand").color(NamedTextColor.GOLD));

            // Marker tag so the listener knows this is a wand
            meta.getPersistentDataContainer().set(
                    new NamespacedKey(this.plugin, "is_wand"),
                    PersistentDataType.BOOLEAN,
                    true
            );

            List<Component> lore = new ArrayList<>();
            lore.add(Component.text("Left-Click: Copy Entity").color(NamedTextColor.GRAY));
            lore.add(Component.text("Right-Click: Paste Entity").color(NamedTextColor.GRAY));
            meta.lore(lore);
        });
        player.getInventory().addItem(wand);
    }

    @Override
    public List<String> tabComplete(Player player, String[] args) {
        return List.of();
    }
}
