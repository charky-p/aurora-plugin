package org.charky.aurora.features.player;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.charky.aurora.common.loot.MenuItem;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

// Shop purchases against the backpack: what a trader checks and takes
class BackpackDataTest {
    // A player with an empty inventory, so only the backpack pays
    private static Player emptyHanded() {
        PlayerInventory inventory = (PlayerInventory) Proxy.newProxyInstance(
                PlayerInventory.class.getClassLoader(), new Class<?>[]{PlayerInventory.class},
                (proxy, method, args) -> method.getName().equals("getContents") ? new ItemStack[0] : null);
        return (Player) Proxy.newProxyInstance(Player.class.getClassLoader(), new Class<?>[]{Player.class},
                (proxy, method, args) -> method.getName().equals("getInventory") ? inventory : null);
    }

    private static MenuItem cost(String id, int amount) {
        return new MenuItem(id, null, amount);
    }

    @Test
    void purchaseTakesExactlyTheCost() {
        Player player = emptyHanded();
        BackpackData backpack = new BackpackData(null);
        backpack.addItem("cobblestone", 100);
        backpack.addItem("coal", 40);

        List<MenuItem> costs = List.of(cost("cobblestone", 64), cost("coal", 32));
        assertTrue(backpack.hasItems(player, costs));
        backpack.removeItems(player, costs);

        assertEquals(36, backpack.getAmount(player, "cobblestone"));
        assertEquals(8, backpack.getAmount(player, "coal"));
    }

    @Test
    void oneShortCostBlocksThePurchase() {
        Player player = emptyHanded();
        BackpackData backpack = new BackpackData(null);
        backpack.addItem("cobblestone", 100);
        backpack.addItem("coal", 31);

        assertFalse(backpack.hasItems(player, List.of(cost("cobblestone", 64), cost("coal", 32))));
    }

    @Test
    void exactAmountIsEnoughAndLeavesNothing() {
        Player player = emptyHanded();
        BackpackData backpack = new BackpackData(null);
        backpack.addItem("iron", 64);

        List<MenuItem> costs = List.of(cost("iron", 64));
        assertTrue(backpack.hasItems(player, costs));
        backpack.removeItems(player, costs);
        assertEquals(0, backpack.getAmount(player, "iron"));
    }

    @Test
    void neverGoesBelowZero() {
        BackpackData backpack = new BackpackData(null);
        backpack.addItem("gold", 5);
        backpack.removeItem("gold", 50);

        assertEquals(0, backpack.getItems().get("gold"));
    }

    @Test
    void dropsStackOnWhatIsHeld() {
        BackpackData backpack = new BackpackData(null);
        backpack.addItems(List.of(cost("feather", 3), cost("feather", 2), cost("wool", 1)));

        assertEquals(5, backpack.getItems().get("feather"));
        assertEquals(1, backpack.getItems().get("wool"));
    }
}
