package org.charky.aurora.features.mine.commands;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.charky.aurora.core.Aurora;
import org.charky.aurora.features.mine.MineBlock;
import org.charky.aurora.common.util.TextUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Mine sub command to set blocks of a mine from the registry.
 * Only registry blocks are accepted, plain vanilla block names were dropped
 */
public class MineSetBlocksSub extends MineSub {
    public MineSetBlocksSub(Aurora plugin) {
        super(plugin);
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length < 3 || args.length % 2 != 1) {
            TextUtils.usage(sender, getFullUsage());
            return;
        }

        String mineName = args[0];
        if (find(sender, mineName) == null) return;

        List<MineBlock> blocks = new ArrayList<>();
        int totalPercent = 0;

        for (int i = 1; i < args.length; i += 2) {
            try {
                int percent = Integer.parseInt(args[i + 1]);
                if (percent < 0) {
                    TextUtils.error(sender, "Percentage for '" + args[i] + "' must be positive");
                    return;
                }

                totalPercent += percent;
                if (!registry.hasBlock(args[i])) {
                    TextUtils.error(sender, "Invalid block '" + args[i] + "', block must be in the registry");
                    return;
                }

                blocks.add(new MineBlock(registry.getMaterial(args[i]), percent, args[i]));
            } catch (NumberFormatException e) {
                TextUtils.error(sender, "Invalid Percentage: '" + args[i + 1] + "'");
                return;
            }
        }

        if (totalPercent != 100) {
            TextUtils.error(sender, "Percentages must add up to 100");
            return;
        }

        mineManager.setBlocks(mineName, blocks);
        TextUtils.success(sender, "Mine '" + mineName + "' successfully updated");

        // Reset mine immediately
        mineManager.resetMine(mineName);
    }

    @Override
    public List<String> tabComplete(Player player, String[] args) {
        if (args.length == 1) return TextUtils.getCompletions(args[0], mineManager.getMines());

        if (args.length % 2 == 0) return registry.getBlocks(args[args.length - 1].toLowerCase());

        return List.of("10", "25", "50", "100");
    }

    @Override
    public String getUsage() {
        return "<mine> <block> <%> [<block> <%>]...";
    }

    @Override
    public String getName() {
        return "setblocks";
    }

    @Override
    public String getDescription() {
        return "Set blocks from the registry of a mine";
    }
}
