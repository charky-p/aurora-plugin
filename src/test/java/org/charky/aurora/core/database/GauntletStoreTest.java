package org.charky.aurora.core.database;

import org.charky.aurora.features.gauntlet.GauntletProfile;
import org.charky.aurora.features.gauntlet.RunRecord;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

// The Gauntlet's run history against a real SQLite file, created by the real Schema
class GauntletStoreTest {
    @TempDir
    File dir;
    private Database db;

    @BeforeEach
    void open() {
        db = new Database(new File(dir, "test.db"), Logger.getLogger("GauntletStoreTest"));
        Schema.create(db);
    }

    @AfterEach
    void close() throws Exception {
        db.close();
    }

    private static RunRecord run(long ended) {
        return new RunRecord(ended, 12, 11, 1, 2, 3, 300, 40, 500, 1, ended % 2 == 0, "might:2,cleave:1");
    }

    @Test
    void theHistoryKeepsTheNewestRunsNewestFirst() {
        GauntletStore store = new GauntletStore(db);
        UUID uuid = UUID.randomUUID();
        store.save(uuid, new GauntletProfile());
        for (long ended = 1; ended <= GauntletProfile.HISTORY + 2; ended++) store.saveRun(uuid, run(ended));

        List<RunRecord> history = store.loadAll().get(uuid).history;
        assertEquals(GauntletProfile.HISTORY, history.size());
        assertEquals(run(GauntletProfile.HISTORY + 2), history.getFirst());
        assertEquals(3, history.getLast().ended(), "the two oldest are gone");
    }

    @Test
    void aResetWipesTheHistory() {
        GauntletStore store = new GauntletStore(db);
        UUID uuid = UUID.randomUUID();
        store.save(uuid, new GauntletProfile());
        store.saveRun(uuid, run(1));
        store.reset(uuid);
        store.save(uuid, new GauntletProfile());
        assertTrue(store.loadAll().get(uuid).history.isEmpty());
    }
}
