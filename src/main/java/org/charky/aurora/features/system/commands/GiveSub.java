package org.charky.aurora.features.system.commands;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.charky.aurora.common.util.TextUtils;
import org.charky.aurora.core.Aurora;
import org.charky.aurora.core.DataRegistry;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Aurora admin command to give an item by registry id, gear and resources from the
 * CSVs, or a vanilla material like cooked_beef. What does not fit drops at their feet
 */
public class GiveSub implements AuroraSubCommand {
    private static final int MAX_AMOUNT = 2304; // a full inventory of 64s

    private final DataRegistry registry;

    public GiveSub(Aurora plugin) {
        this.registry = plugin.getRegistry();
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length < 2) {
            TextUtils.usage(sender, getFullUsage());
            return;
        }

        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) {
            TextUtils.error(sender, "Player not found: " + args[0]);
            return;
        }

        String id = args[1].toLowerCase();
        ItemStack template = registry.getAllItems().get(id);
        if (template == null) {
            Material material = Material.matchMaterial(id);
            if (material == null || !material.isItem() || material.isAir()) {
                TextUtils.error(sender, "No item '" + id + "' in the registry or vanilla");
                return;
            }
            template = new ItemStack(material);
        }

        int amount = 1;
        if (args.length > 2) {
            try {
                amount = Integer.parseInt(args[2]);
            } catch (NumberFormatException e) {
                TextUtils.error(sender, "Invalid amount: " + args[2]);
                return;
            }
            if (amount < 1 || amount > MAX_AMOUNT) {
                TextUtils.error(sender, "Amount must be 1 to " + MAX_AMOUNT);
                return;
            }
        }

        List<ItemStack> stacks = new ArrayList<>();
        for (int left = amount; left > 0; ) {
            ItemStack stack = template.clone();
            stack.setAmount(Math.min(left, template.getMaxStackSize()));
            left -= stack.getAmount();
            stacks.add(stack);
        }

        Map<Integer, ItemStack> overflow = target.getInventory().addItem(stacks.toArray(ItemStack[]::new));
        overflow.values().forEach(stack -> target.getWorld().dropItem(target.getLocation(), stack));

        TextUtils.success(sender, "Gave " + amount + " " + id + " to " + target.getName()
                + (overflow.isEmpty() ? "" : ", some dropped at their feet"));
    }

    @Override
    public List<String> tabComplete(Player player, String[] args) {
        if (args.length == 1) return TextUtils.playerCompletions(args[0]);
        if (args.length == 2) return TextUtils.searchCompletions(args[1], registry.getAllItems().keySet());
        return List.of();
    }

    @Override
    public String getUsage() {
        return "<player> <item> [amount]";
    }

    @Override
    public String getName() {
        return "give";
    }

    @Override
    public String getDescription() {
        return "Give an item by registry id, or a vanilla material";
    }
}
