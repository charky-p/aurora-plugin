package org.charky.aurora;

import org.bukkit.scheduler.BukkitTask;
import org.charky.aurora.models.MineBlock;
import org.charky.aurora.models.MineData;

import java.util.List;

public class Mine {
    private final MineData data;
    private List<MineBlock> composition;
    private BukkitTask task;
    private int blocksBroken;

    public Mine(MineData data, List<MineBlock> composition) {
        this.data = data;
        this.composition = composition;
    }


}
