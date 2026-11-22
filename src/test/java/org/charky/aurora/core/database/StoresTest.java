package org.charky.aurora.core.database;

import org.bukkit.Material;
import org.charky.aurora.features.loadout.Loadout;
import org.charky.aurora.features.mine.MineBlock;
import org.charky.aurora.features.mine.MineData;
import org.charky.aurora.features.player.PlayerData;
import org.charky.aurora.features.portal.Portal;
import org.charky.aurora.features.suggestion.Suggestion;
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

    @Test
    void playerXpAndPlaytimeRoundTrip() {
        PlayerStore store = new PlayerStore(db);
        UUID uuid = UUID.randomUUID();
        store.save(uuid, new PlayerData("AURORA", 12, 345, 30, 400, 7, 7_260));

        PlayerData loaded = store.load(uuid);
        assertEquals("AURORA", loaded.getRank());
        assertEquals(12, loaded.getLevel());
        assertEquals(345, loaded.getXp());
        assertEquals(7_260, loaded.getPlaytime());
    }

    @Test
    void essenceCanOnlyBeSpentIfThereIsEnough() {
        PlayerData data = new PlayerData("MEMBER", 0, 0, 0, 100);

        assertFalse(data.spendEssence(101));
        assertEquals(100, data.getEssence());
        assertTrue(data.spendEssence(100));
        assertEquals(0, data.getEssence());
    }

    @Test
    void boughtRanksAreStoredOnce() {
        RankStore store = new RankStore(db);
        UUID uuid = UUID.randomUUID();
        store.add(uuid, "EMBER");
        store.add(uuid, "EMBER");
        store.add(uuid, "AURORA");

        assertEquals(Set.of("EMBER", "AURORA"), store.loadAll().get(uuid));
    }

    @Test
    void collectionsSaveOverTheLastTotal() {
        CollectionStore store = new CollectionStore(db);
        UUID uuid = UUID.randomUUID();
        store.saveAll(Map.of(uuid, Map.of("coal", 300L, "iron", 12L)));
        store.saveAll(Map.of(uuid, Map.of("coal", 2_600L)));

        Map<String, Long> loaded = store.loadAll().get(uuid);
        assertEquals(2_600L, loaded.get("coal"));
        // A resource the later save did not mention keeps its total
        assertEquals(12L, loaded.get("iron"));
    }

    @Test
    void suggestionsKeepTheirIdsAndOrder() {
        SuggestionStore store = new SuggestionStore(db);
        UUID uuid = UUID.randomUUID();
        Suggestion first = new Suggestion(1, uuid, "Charky", "More mounts", 1_000L);
        Suggestion second = new Suggestion(7, uuid, "Charky", "A fishing contest", 2_000L);
        store.add(second);
        store.add(first);
        assertEquals(List.of(first, second), store.loadAll());

        store.delete(1);
        assertEquals(List.of(second), store.loadAll());
    }

    // A switch from one loadout to another, the way LoadoutManager does it
    private static void switchLoadout(LoadoutStore loadouts, UUID uuid, Loadout from, Loadout to) {
        loadouts.switchTo(uuid, from.homeOf(uuid), to.homeOf(uuid), to);
    }

    @Test
    void loadoutSwitchTradesRowsBothWays() {
        PlayerStore players = new PlayerStore(db);
        LoadoutStore loadouts = new LoadoutStore(db);
        UUID uuid = UUID.randomUUID();
        UUID mainHome = Loadout.MAIN.homeOf(uuid);
        players.save(uuid, new PlayerData("MEMBER", 30, 100, 5, 900, 70, 0));
        db.update("INSERT INTO tutorial (uuid, step) VALUES (?, ?)", pst -> {
            pst.setString(1, uuid.toString());
            pst.setString(2, "MINE");
        });

        // First time in a test world: the main loadout is put away, nothing comes back
        switchLoadout(loadouts, uuid, Loadout.MAIN, Loadout.EXPERIMENTAL);
        assertEquals(0, players.load(uuid).getLevel());
        assertEquals(30, players.load(mainHome).getLevel());
        assertEquals(Map.of(uuid, Loadout.EXPERIMENTAL), loadouts.loadActive());

        players.save(uuid, new PlayerData("MEMBER", 2, 0, 0, 10, 0, 0));
        switchLoadout(loadouts, uuid, Loadout.EXPERIMENTAL, Loadout.MAIN);
        assertEquals(30, players.load(uuid).getLevel());
        assertEquals(2, players.load(Loadout.EXPERIMENTAL.homeOf(uuid)).getLevel());
        assertEquals(0, players.load(mainHome).getLevel());
        List<String> steps = new ArrayList<>();
        db.query("SELECT step FROM tutorial WHERE uuid = ?", pst -> pst.setString(1, uuid.toString()),
                rs -> steps.add(rs.getString("step")));
        assertEquals(List.of("MINE"), steps);
        assertEquals(Map.of(uuid, Loadout.MAIN), loadouts.loadActive());
    }

    // Four loadouts, each kept apart: main, experimental and slots 2 and 3
    @Test
    void slotsKeepTheirOwnProgress() {
        PlayerStore players = new PlayerStore(db);
        LoadoutStore loadouts = new LoadoutStore(db);
        UUID uuid = UUID.randomUUID();
        players.save(uuid, new PlayerData("MEMBER", 30, 0, 0, 0, 0, 0));
        switchLoadout(loadouts, uuid, Loadout.MAIN, Loadout.EXPERIMENTAL);
        players.save(uuid, new PlayerData("MEMBER", 5, 0, 0, 0, 0, 0));
        switchLoadout(loadouts, uuid, Loadout.EXPERIMENTAL, Loadout.SLOT_2);
        players.save(uuid, new PlayerData("MEMBER", 12, 0, 0, 0, 0, 0));
        switchLoadout(loadouts, uuid, Loadout.SLOT_2, Loadout.SLOT_3);
        assertEquals(0, players.load(uuid).getLevel());
        players.save(uuid, new PlayerData("MEMBER", 7, 0, 0, 0, 0, 0));

        switchLoadout(loadouts, uuid, Loadout.SLOT_3, Loadout.SLOT_2);
        assertEquals(12, players.load(uuid).getLevel());
        switchLoadout(loadouts, uuid, Loadout.SLOT_2, Loadout.EXPERIMENTAL);
        assertEquals(5, players.load(uuid).getLevel());
        switchLoadout(loadouts, uuid, Loadout.EXPERIMENTAL, Loadout.MAIN);
        assertEquals(30, players.load(uuid).getLevel());
        assertEquals(7, players.load(Loadout.SLOT_3.homeOf(uuid)).getLevel());
    }

    // Leaderboards show main loadouts only, under the player's own id, put away or not
    @Test
    void leaderboardsShowMainLoadoutsOnly() {
        PlayerStore players = new PlayerStore(db);
        LoadoutStore loadouts = new LoadoutStore(db);
        UUID uuid = UUID.randomUUID();
        players.save(uuid, new PlayerData("MEMBER", 30, 100, 5, 900, 70, 0));
        switchLoadout(loadouts, uuid, Loadout.MAIN, Loadout.EXPERIMENTAL);
        players.save(uuid, new PlayerData("MEMBER", 1, 0, 0, 0, 0, 0));

        // Main put away while the experimental one is in play: its 30 counts, not the 1
        List<Object[]> levels = players.topLevels(10);
        assertEquals(1, levels.size());
        assertEquals(uuid, levels.getFirst()[0]);
        assertEquals(30, levels.getFirst()[1]);
        assertEquals(900, players.top("mined", "kills", 10).getFirst()[2]);
        assertEquals(1, players.ahead("level", 1));

        // Slot 2 in play with a higher level: still only main's 30
        switchLoadout(loadouts, uuid, Loadout.EXPERIMENTAL, Loadout.SLOT_2);
        players.save(uuid, new PlayerData("MEMBER", 50, 0, 0, 0, 0, 0));
        levels = players.topLevels(10);
        assertEquals(1, levels.size());
        assertEquals(30, levels.getFirst()[1]);
    }

    // What deploy.py does to the host the first time: everyone is marked experimental and
    // nothing moves, so nobody is on a leaderboard until they play their fresh main one
    @Test
    void markedExperimentalIsOffTheLeaderboards() {
        PlayerStore players = new PlayerStore(db);
        UUID uuid = UUID.randomUUID();
        players.save(uuid, new PlayerData("MEMBER", 30, 100, 5, 900, 70, 0));
        db.update("INSERT INTO loadouts (uuid, active) VALUES (?, 'experimental')", pst -> pst.setString(1, uuid.toString()));

        LoadoutStore loadouts = new LoadoutStore(db);
        assertEquals(1, loadouts.migrate());
        assertEquals(List.of(), players.topLevels(10));
        assertEquals(Map.of(uuid, Loadout.EXPERIMENTAL), loadouts.loadActive());

        // Their next join in a main world: put away, a fresh main one in play
        switchLoadout(loadouts, uuid, Loadout.EXPERIMENTAL, Loadout.MAIN);
        players.save(uuid, new PlayerData("MEMBER", 2, 0, 0, 0, 0, 0));
        assertEquals(2, players.topLevels(10).getFirst()[1]);
        assertEquals(30, players.load(Loadout.EXPERIMENTAL.homeOf(uuid)).getLevel());
    }

    // Before slots, main was put away under the shared id while experimental was in play.
    // The move gives it its own id, once, and leaves players on main as they were
    @Test
    void migrateMovesMainToItsOwnId() {
        PlayerStore players = new PlayerStore(db);
        UUID onExperimental = UUID.randomUUID();
        UUID onMain = UUID.randomUUID();
        UUID oldShadow = Loadout.shadowOf(onExperimental);
        players.save(oldShadow, new PlayerData("MEMBER", 30, 0, 0, 0, 0, 0));
        players.save(onExperimental, new PlayerData("MEMBER", 4, 0, 0, 0, 0, 0));
        players.save(onMain, new PlayerData("MEMBER", 20, 0, 0, 0, 0, 0));
        players.save(Loadout.shadowOf(onMain), new PlayerData("MEMBER", 3, 0, 0, 0, 0, 0));
        db.update("INSERT INTO loadouts (uuid, shadow, active) VALUES (?, ?, 'experimental')", pst -> {
            pst.setString(1, onExperimental.toString());
            pst.setString(2, oldShadow.toString());
        });
        db.update("INSERT INTO loadouts (uuid, shadow, active) VALUES (?, ?, 'main')", pst -> {
            pst.setString(1, onMain.toString());
            pst.setString(2, Loadout.shadowOf(onMain).toString());
        });

        LoadoutStore loadouts = new LoadoutStore(db);
        assertEquals(2, loadouts.migrate());
        assertEquals(0, loadouts.migrate());
        assertEquals(30, players.load(Loadout.MAIN.homeOf(onExperimental)).getLevel());
        assertEquals(0, players.load(oldShadow).getLevel());
        assertEquals(3, players.load(Loadout.EXPERIMENTAL.homeOf(onMain)).getLevel());
        List<Integer> levels = new ArrayList<>();
        for (Object[] row : players.topLevels(10)) levels.add(((Number) row[1]).intValue());
        assertEquals(List.of(30, 20), levels);

        switchLoadout(loadouts, onExperimental, Loadout.EXPERIMENTAL, Loadout.MAIN);
        assertEquals(30, players.load(onExperimental).getLevel());
        assertEquals(4, players.load(oldShadow).getLevel());
    }

    // /aurora testworld the first time: the experimental loadout starts as a copy of the
    // main one, then a switch puts the copy in play and the main one away, both intact
    @Test
    void copyStartsAnIdenticalLoadout() {
        PlayerStore players = new PlayerStore(db);
        LoadoutStore loadouts = new LoadoutStore(db);
        UUID uuid = UUID.randomUUID();
        UUID experimental = Loadout.EXPERIMENTAL.homeOf(uuid);
        players.save(uuid, new PlayerData("MEMBER", 30, 100, 5, 900, 70, 0));
        db.update("INSERT INTO tutorial (uuid, step) VALUES (?, ?)", pst -> {
            pst.setString(1, uuid.toString());
            pst.setString(2, "MINE");
        });

        loadouts.copy(uuid, experimental);
        assertEquals(30, players.load(experimental).getLevel());
        switchLoadout(loadouts, uuid, Loadout.MAIN, Loadout.EXPERIMENTAL);
        players.save(uuid, new PlayerData("MEMBER", 31, 0, 5, 950, 70, 0));

        assertEquals(31, players.load(uuid).getLevel());
        assertEquals(30, players.load(Loadout.MAIN.homeOf(uuid)).getLevel());
        List<String> owners = new ArrayList<>();
        db.query("SELECT uuid FROM tutorial ORDER BY uuid", pst -> {}, rs -> owners.add(rs.getString("uuid")));
        assertEquals(2, owners.size());
    }

    // A new per-player table has to be listed, or a switch would leave its rows behind
    @Test
    void everyPlayerTableIsInALoadoutOrShared() {
        List<String> tables = new ArrayList<>();
        db.query("SELECT name FROM sqlite_master WHERE type = 'table' AND name NOT LIKE 'sqlite_%'", pst -> {},
                rs -> tables.add(rs.getString("name")));
        Set<String> withPlayers = new TreeSet<>();
        for (String table : tables) {
            db.query("PRAGMA table_info(" + table + ")", pst -> {}, rs -> {
                if (rs.getString("name").equalsIgnoreCase("uuid")) withPlayers.add(table);
            });
        }
        Set<String> listed = new TreeSet<>(LoadoutStore.TABLES);
        listed.addAll(LoadoutStore.SHARED);
        assertEquals(listed, withPlayers);
    }

    private static MineData mine(String name) {
        return new MineData(name, "aurora_mines", 0, 0, 0, 5, 5, 5, 50, 600, 0, 1, 0);
    }
}
