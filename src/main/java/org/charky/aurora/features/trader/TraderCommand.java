package org.charky.aurora.features.trader;

import org.charky.aurora.core.Aurora;
import org.charky.aurora.features.system.commands.SubCommandRouter;
import org.charky.aurora.features.trader.commands.*;

/**
 * Trader commands. Traders are saved by spot in the traders table and respawn there,
 * they can also be removed by right clicking them with the wand
 */
public class TraderCommand extends SubCommandRouter {
    public TraderCommand(Aurora plugin) {
        super("trader", "Place traders that respawn at their spot");
        register(new TraderCreateSub(plugin));
        register(new TraderDeleteSub(plugin));
        register(new TraderListSub(plugin));
        register(new TraderTpSub(plugin));
    }
}
