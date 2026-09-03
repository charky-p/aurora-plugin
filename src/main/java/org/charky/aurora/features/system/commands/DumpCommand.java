package org.charky.aurora.features.system.commands;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.block.ShulkerBox;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.charky.aurora.common.util.TextUtils;
import org.charky.aurora.core.Aurora;
import org.charky.aurora.core.DataRegistry;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * /dump [items|icons|all]
 *
 * Packs every ItemStack the registry knows about into shulker boxes and hands
 * them over. Each item gets its registry id appended as lore, since display
 * names do not always match the id you need for blocks.yml or a GUI component.
 */
public class DumpCommand implements CommandExecutor, TabCompleter {
    private static final int SHULKER_SLOTS = 27;

    private final DataRegistry registry;

    public DumpCommand(Aurora plugin) {
        this.registry = plugin.getRegistry();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String @NotNull [] args) {
        if (!(sender instanceof Player player)) {
            TextUtils.error(sender, "Players only.");
            return true;
        }
        if (!player.hasPermission("aurora.admin")) {
            TextUtils.error(player, "This is an administrator-only command.");
            return true;
        }

        String mode = args.length > 0 ? args[0].toLowerCase() : "items";

        List<Map.Entry<String, ItemStack>> source = new ArrayList<>();
        switch (mode) {
            case "items" -> source.addAll(registry.getAllItems().entrySet());
            case "icons" -> source.addAll(registry.getAllIcons().entrySet());
            case "all" -> {
                source.addAll(registry.getAllItems().entrySet());
                source.addAll(registry.getAllIcons().entrySet());
            }
            default -> {
                TextUtils.usage(player, "/" + label + " [items|icons|all]");
                return true;
            }
        }

        if (source.isEmpty()) {
            TextUtils.warn(player, "Registry has no entries for '" + mode + "'.");
            return true;
        }

        int given = 0;
        int dropped = 0;

        for (int start = 0; start < source.size(); start += SHULKER_SLOTS) {
            int end = Math.min(start + SHULKER_SLOTS, source.size());
            ItemStack shulker = buildShulker(source.subList(start, end), mode, start, end);

            // addItem returns whatever would not fit; drop that rather than
            // silently losing it.
            Map<Integer, ItemStack> leftover = player.getInventory().addItem(shulker);
            for (ItemStack overflow : leftover.values()) {
                player.getWorld().dropItemNaturally(player.getLocation(), overflow);
                dropped++;
            }
            given++;
        }

        TextUtils.success(player, "Dumped <white>" + source.size() + "<gray> "
                + mode + " into <white>" + given + "<gray> shulker(s)."
                + (dropped > 0 ? " <yellow>" + dropped + " dropped (inventory full)." : ""));
        return true;
    }

    private ItemStack buildShulker(List<Map.Entry<String, ItemStack>> batch,
                                   String mode, int start, int end) {
        ItemStack shulker = new ItemStack(Material.SHULKER_BOX);
        BlockStateMeta meta = (BlockStateMeta) shulker.getItemMeta();
        ShulkerBox box = (ShulkerBox) meta.getBlockState();

        for (int i = 0; i < batch.size(); i++) {
            Map.Entry<String, ItemStack> entry = batch.get(i);

            // Clone: these are the registry's own stacks, and tagging the id
            // onto the original would corrupt every future copy of the item.
            ItemStack copy = entry.getValue().clone();
            copy.editMeta(m -> {
                List<Component> lore = m.hasLore() ? new ArrayList<>(m.lore()) : new ArrayList<>();
                lore.add(Component.text("id: " + entry.getKey(), NamedTextColor.DARK_GRAY)
                        .decoration(TextDecoration.ITALIC, false));
                m.lore(lore);
            });
            box.getInventory().setItem(i, copy);
        }

        meta.setBlockState(box);
        meta.displayName(Component.text(mode + " " + (start + 1) + "-" + end,
                        NamedTextColor.LIGHT_PURPLE)
                .decoration(TextDecoration.ITALIC, false));
        shulker.setItemMeta(meta);
        return shulker;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                @NotNull String label, @NotNull String @NotNull [] args) {
        if (args.length == 1) {
            return TextUtils.getCompletions(args[0], List.of("items", "icons", "all"));
        }
        return List.of();
    }
}