package org.charky.aurora;

import io.papermc.paper.entity.LookAnchor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.util.Vector;
import org.charky.aurora.core.Aurora;
import org.junit.jupiter.api.*;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import java.util.List;

public class MineCreateTest {
    private ServerMock server;
    private Aurora plugin;
    private PlayerMock player;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(Aurora.class);
        player = server.addPlayer();
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    @Disabled
    void testTabCompleteBlockRecognition() {
        // Player looks at stone block
        Block target = player.getWorld().getBlockAt(2, 0, 0);
        target.setType(Material.STONE);

        Location playerLocation = player.getLocation();
        Vector direction = target.getLocation().toVector().subtract(playerLocation.toVector()).normalize();

        player.teleport(playerLocation.setDirection(direction));

        Command command = plugin.getCommand("aurora");
        // Execute tab complete
        String[] args = {"mine", "create", "test_mine", ""};
        List<String> result = command.tabComplete(player, "aurora", args, null);

        Assertions.assertTrue(result.contains("2"), "Should suggest the X coordinate of the looked-at block");
        Assertions.assertTrue(result.contains("~"), "Should suggest ~, the relative position");
    }

}
