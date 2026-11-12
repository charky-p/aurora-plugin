package org.charky.aurora.core.database;

import org.bukkit.Material;
import org.charky.aurora.features.mine.MineBlock;
import org.charky.aurora.features.mine.MineData;
import org.charky.aurora.features.player.PlayerData;
import org.charky.aurora.features.trader.TraderSpot;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

// Every store against a real SQLite file, created by the real Schema
class StoresTest {
    @TempDir
    File dir;
    private Database db;

    @BeforeEach
    void open() {
        db = new Database(new File(dir, "test.db"), Logger.getLogger("StoresTest"));
        Schema.create(db);
    }

    @AfterEach
    void close() throws Exception {
        db.close();
    }

    @Test
    void schemaCanRunTwice() {
        // Every startup runs it against an existing database
        assertDoesNotThrow(() -> Schema.create(db));
    }

    @Test
    void traderSpotsGetNewIdsAndLoadInOrder() {
        TraderStore store = new TraderStore(db);
        int first = store.add("starter_trader", "world", 1.5, 64, -2.5, 90f);
        int second = store.add("cobble_trader", "aurora_mines", 10, 70, 10, 0f);

        assertTrue(first > 0);
        assertTrue(second > first);

        List<TraderSpot> spots = store.loadAll();
        assertEquals(List.of(
                new TraderSpot(first, "starter_trader", "world", 1.5, 64, -2.5, 90f),
                new TraderSpot(second, "cobble_trader", "aurora_mines", 10, 70, 10, 0f)), spots);
    }

    @Test
    void removedTraderSpotIsGoneAndItsIdNotReused() {
        TraderStore store = new TraderStore(db);
        int first = store.add("starter_trader", "world", 0, 0, 0, 0f);
        store.remove(first);
        int next = store.add("starter_trader", "world", 0, 0, 0, 0f);

        assertNotEquals(first, next);
        assertEquals(List.of(next), store.loadAll().stream().map(TraderSpot::id).toList());
    }

    @Test
    void backpacksRoundTripWithoutEmptyRows() {
        BackpackStore store = new BackpackStore(db);
        UUID player = UUID.randomUUID();
        store.saveAll(Map.of(player, Map.of("coal", 120L, "iron", 0L)));

        Map<String, Long> loaded = store.loadAll(null).get(player).getItems();
        assertEquals(Map.of("coal", 120L), loaded);
    }

    @Test
    void backpackSaveReplacesTheLastOne() {
        BackpackStore store = new BackpackStore(db);
        UUID player = UUID.randomUUID();
        store.saveAll(Map.of(player, Map.of("coal", 120L)));
        store.saveAll(Map.of(player, Map.of("coal", 5L)));

        assertEquals(Map.of("coal", 5L), store.loadAll(null).get(player).getItems());
    }

    @Test
    void unknownPlayerLoadsAsNewProfile() {
        PlayerData data = new PlayerStore(db).load(UUID.randomUUID());
        assertEquals("MEMBER", data.getRank());
        assertEquals(0, data.getMined());
        assertEquals(0, data.getEssence());
    }

    @Test
    void playerSaveRoundTrips() {
        PlayerStore store = new PlayerStore(db);
        UUID uuid = UUID.randomUUID();
        store.save(uuid, new PlayerData("MEMBER", 2, 30, 400, 7));

        PlayerData loaded = store.load(uuid);
        assertEquals(2, loaded.getLevel());
        assertEquals(30, loaded.getKills());
        assertEquals(400, loaded.getMined());
        assertEquals(7, loaded.getEssence());
    }

    @Test
    void autosaveOnlyWritesChangedProfilesAndClearsThem() {
        PlayerStore store = new PlayerStore(db);
        UUID changed = UUID.randomUUID();
        UUID untouched = UUID.randomUUID();

        PlayerData changedData = new PlayerData("MEMBER", 0, 0, 0, 0);
        changedData.addMined(5);
        ConcurrentMap<UUID, PlayerData> profiles = new ConcurrentHashMap<>();
        profiles.put(changed, changedData);
        profiles.put(untouched, new PlayerData("MEMBER", 0, 99, 0, 0));

        store.saveAll(profiles);

        assertFalse(changedData.isDirty());
        assertEquals(5, store.load(changed).getMined());
        // Never written, so it loads as a new profile
        assertEquals(0, store.load(untouched).getKills());
    }

    @Test
    void unlocksAreStoredOnce() {
        UnlockStore store = new UnlockStore(db);
        UUID uuid = UUID.randomUUID();
        store.add(uuid, "serf_cleaver");
        store.add(uuid, "serf_cleaver");

        assertEquals(Set.of("serf_cleaver"), store.loadAll().get(uuid));
    }

    @Test
    void renamedMineKeepsItsBlocks() {
        MineStore store = new MineStore(db);
        store.add(mine("coal"));
        store.setBlocks("coal", List.of(new MineBlock(Material.COAL_ORE, 60, "coal_ore")));

        store.rename("coal", "Coal");

        assertEquals(List.of("Coal"), store.loadAll().stream().map(MineData::getName).toList());
        assertEquals(1, store.loadBlocks().get("Coal").size());
        assertNull(store.loadBlocks().get("coal"));
    }

    @Test
    void removedMineTakesItsBlocks() {
        MineStore store = new MineStore(db);
        store.add(mine("coal"));
        store.setBlocks("coal", List.of(new MineBlock(Material.COAL_ORE, 60, "coal_ore")));

        store.remove("coal");

        assertTrue(store.loadAll().isEmpty());
        assertTrue(store.loadBlocks().isEmpty());
    }

    private static MineData mine(String name) {
        return new MineData(name, "aurora_mines", 0, 0, 0, 5, 5, 5, 50, 600, 0, 1, 0);
    }
}
