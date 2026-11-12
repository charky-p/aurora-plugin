package org.charky.aurora.features.mine;

import org.charky.aurora.core.Aurora;
import org.charky.aurora.features.mine.commands.*;
import org.charky.aurora.features.system.commands.SubCommandRouter;

/**
 * Main command for all /aurora mine commands
 */
public class MineCommand extends SubCommandRouter {
    public MineCommand(Aurora plugin) {
        super("mine", "Create regenerating mines with custom or vanilla blocks");
        register(new MineCreateSub(plugin));
        register(new MineDeleteSub(plugin));
        register(new MineListSub(plugin));
        register(new MineSetBlocksSub(plugin));
        register(new MineResetSub(plugin));
        register(new MineResetAllSub(plugin));
        register(new MineTpSub(plugin));
        register(new MineSetTpSub(plugin));
        register(new MineInfoSub(plugin));
        register(new MineSetThresholdSub(plugin));
        register(new MineSetDelaySub(plugin));
        register(new MineRedefineSub(plugin));
        register(new MineRenameSub(plugin));
        register(new MineWandCreateSub(plugin));
    }
}
