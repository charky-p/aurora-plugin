package org.charky.aurora.features.spawner;

import org.charky.aurora.core.Aurora;
import org.charky.aurora.features.spawner.commands.*;
import org.charky.aurora.features.system.commands.SubCommandRouter;

/**
 * Main command for all /aurora spawner commands
 */
// TODO: no mob or trade regions, where mobs will be deleted (not traders)
public class SpawnerCommand extends SubCommandRouter {
    public SpawnerCommand(Aurora plugin) {
        super("spawner", "Create custom spawners with custom entities");
        register(new SpawnerCreateSub(plugin));
        register(new SpawnerDeleteSub(plugin));
        register(new SpawnerListSub(plugin));
        register(new SpawnerTpSub(plugin));
        register(new SpawnerInfoSub(plugin));
        register(new SpawnerSetMobSub(plugin));
        register(new SpawnerSetRadiusSub(plugin));
        register(new SpawnerSetLimitSub(plugin));
        register(new SpawnerSetDelaySub(plugin));
        register(new SpawnerRenameSub(plugin));
        register(new SpawnerRedefineSub(plugin));
        register(new SpawnerToggleSub(plugin));
    }
}
