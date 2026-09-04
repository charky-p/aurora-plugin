package org.charky.aurora.features.player;

import org.charky.aurora.core.DataRegistry;
import org.charky.aurora.core.Database;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class BackpackManager {
    private Map<UUID, BackpackData> backpacks;
    private final DataRegistry registry;
    private Database db;

    public BackpackManager(DataRegistry registry) {
        this.registry = registry;
    }

    public void loadBackpacks(Database db) {
        this.backpacks = db.loadAllBackpacks(registry);
        this.db = db;
    }

    public BackpackData get(UUID uuid) {
        return backpacks.computeIfAbsent(uuid, k -> new BackpackData(registry));
    }

    public void clear(UUID uuid) {
        BackpackData data = backpacks.get(uuid);
        if (data != null) data.clear();
    }

    public void shutdown() {
        db.saveBackpacks(backpacks);
    }
}
