package org.charky.aurora;

import org.junit.jupiter.api.*;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class JoinTest {
    private ServerMock server;
    private Aurora plugin;

    @BeforeEach
    public void setUp() {
        // Start the fake server
        server = MockBukkit.mock();
        // Load your plugin
        plugin = MockBukkit.load(Aurora.class);
    }

    @AfterEach
    public void tearDown() {
        // Stop the server after every test
        MockBukkit.unmock();
    }

    @Test
    @DisplayName("First join test")
    public void joinTest() {
        PlayerMock player = server.addPlayer();

        assertTrue(player.isOnline(), "The player should be online");
    }

}
