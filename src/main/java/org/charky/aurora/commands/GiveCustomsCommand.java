package org.charky.aurora.commands;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class GiveCustomsCommand implements CommandExecutor {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) return true;

        // Create the item
        ItemStack pick = new ItemStack(Material.DIAMOND_PICKAXE);
        ItemMeta meta = pick.getItemMeta();

        if (meta != null) {
            meta.displayName(Component.text("Aurora Blast Pickaxe")
                    .color(NamedTextColor.GOLD)
                    .decoration(TextDecoration.BOLD, true)
                    .decoration(TextDecoration.ITALIC, false));
            List<Component> lore = new ArrayList<>();
            lore.add(Component.text("Blast I").color(NamedTextColor.GRAY));
            meta.lore(lore);
            pick.setItemMeta(meta);
        }

        player.getInventory().addItem(pick);
        player.sendMessage(Component.text("You received a Blast Pickaxe!").color(NamedTextColor.AQUA));

        // Efficiency 100 pick
        pick = new ItemStack(Material.IRON_PICKAXE);
        meta = pick.getItemMeta();

        if (meta != null) {
            meta.displayName(Component.text("Fast Pick"));
            meta.addEnchant(Enchantment.EFFICIENCY, 100, true);
            pick.setItemMeta(meta);
        }

        player.getInventory().addItem(pick);
        player.sendMessage(Component.text("You received a Fast Pickaxe!").color(NamedTextColor.AQUA));

        // Fast blast
        pick = new ItemStack(Material.WOODEN_PICKAXE);
        meta = pick.getItemMeta();

        if (meta != null) {
            meta.displayName(Component.text("Aurora Blast Pickaxe")
                    .color(NamedTextColor.GOLD)
                    .decoration(TextDecoration.BOLD, true)
                    .decoration(TextDecoration.ITALIC, false));
            List<Component> lore = new ArrayList<>();
            lore.add(Component.text("Blast V").color(NamedTextColor.GRAY));
            meta.lore(lore);
            meta.addEnchant(Enchantment.EFFICIENCY, 50, true);
            meta.setUnbreakable(true);
            pick.setItemMeta(meta);
        }

        player.getInventory().addItem(pick);
        player.sendMessage(Component.text("You received a Fast Blast Pickaxe!").color(NamedTextColor.AQUA));


        // Gravity blade
        ItemStack sword = new ItemStack(Material.DIAMOND_SWORD);
        meta = sword.getItemMeta();

        if (meta != null) {
            meta.displayName(Component.text("Gravity Blade").color(NamedTextColor.BLUE));
            List<Component> lore = new ArrayList<>();
            lore.add(Component.text("Whirl I"));
            meta.lore(lore);
            sword.setItemMeta(meta);
        }

        player.getInventory().addItem(sword);
        player.sendMessage(Component.text("You received a Gravity Blade!").color(NamedTextColor.BLUE));

        return true;
    }
}