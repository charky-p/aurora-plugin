package org.charky.aurora.core.database;

import org.bukkit.Material;
import org.charky.aurora.features.mine.MineBlock;
import org.charky.aurora.features.mine.MineData;
import org.charky.aurora.features.player.PlayerData;
import org.charky.aurora.features.portal.Portal;
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
        store.save(mine("coal"));
        store.setBlocks("coal", List.of(new MineBlock(Material.COAL_ORE, 60, "coal_ore")));

        store.rename("coal", "Coal");

        assertEquals(List.of("Coal"), store.loadAll().stream().map(MineData::getName).toList());
        assertEquals(1, store.loadBlocks().get("Coal").size());
        assertNull(store.loadBlocks().get("coal"));
    }

    @Test
    void removedMineTakesItsBlocks() {
        MineStore store = new MineStore(db);
        store.save(mine("coal"));
        store.setBlocks("coal", List.of(new MineBlock(Material.COAL_ORE, 60, "coal_ore")));

        store.remove("coal");

        assertTrue(store.loadAll().isEmpty());
        assertTrue(store.loadBlocks().isEmpty());
    }

    @Test
    void worldsAreStoredOnceAndRemoved() {
        WorldStore store = new WorldStore(db);
        store.add("gormak");
        store.add("gormak");
        store.add("verdant");
        assertEquals(List.of("gormak", "verdant"), store.loadAll());

        store.remove("gormak");
        assertEquals(List.of("verdant"), store.loadAll());
    }

    @Test
    void portalWithoutDestinationRoundTrips() {
        PortalStore store = new PortalStore(db);
        store.save(new Portal("to_gormak", "aurora_mines", 5, 70, 5, 3, 72, 6));

        Portal loaded = store.loadAll().getFirst();
        assertEquals("to_gormak", loaded.getName());
        assertEquals(3, loaded.getX1());
        assertEquals(5, loaded.getX2());
        assertFalse(loaded.hasDestination());
    }

    @Test
    void portalRequiredTagRoundTripsAndClears() {
        PortalStore store = new PortalStore(db);
        Portal portal = new Portal("to_gormak", "aurora_mines", 0, 70, 0, 2, 72, 0);
        assertNull(store.loadAll().stream().findFirst().map(Portal::getRequiredTag).orElse(null));

        portal.setRequiredTag("Gormak");
        store.save(portal);
        assertEquals("gormak", store.loadAll().getFirst().getRequiredTag());

        portal.setRequiredTag(null);
        store.save(portal);
        assertNull(store.loadAll().getFirst().getRequiredTag());
    }

    @Test
    void portalDestinationRoundTripsAndRenames() {
        PortalStore store = new PortalStore(db);
        Portal portal = new Portal("to_gormak", "aurora_mines", 0, 70, 0, 2, 72, 0);
        portal.setDestination("gormak", 10.5, 64, -3.5, 90f, 10f);
        store.save(portal);
        store.rename("to_gormak", "gormak_gate");

        Portal loaded = store.loadAll().getFirst();
        assertEquals("gormak_gate", loaded.getName());
        assertEquals("gormak", loaded.getDestWorld());
        assertEquals(10.5, loaded.getDestX());
        assertEquals(-3.5, loaded.getDestZ());
        assertEquals(90f, loaded.getDestYaw());
    }

    @Test
    void portalBoxIncludesItsEdges() {
        Portal portal = new Portal("p", "world", 2, 70, 2, 0, 72, 0);
        assertTrue(portal.contains("world", 0, 70, 0));
        assertTrue(portal.contains("world", 2, 72, 2));
        assertFalse(portal.contains("world", 3, 70, 0));
        assertFalse(portal.contains("gormak", 1, 71, 1));
    }

    private static MineData mine(String name) {
        return new MineData(name, "aurora_mines", 0, 0, 0, 5, 5, 5, 50, 600, 0, 1, 0);
    }
}
