package org.charky.aurora.features.mine;

import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.charky.aurora.core.Aurora;
import org.charky.aurora.features.build.BuildManager;

import java.util.Set;

// Players can't change the world: no doors, trapdoors, gates, buttons or levers, no
// trampled farmland, and no stripping logs, making paths or tilling with their tools.
// Whoever breaks blocks for real is free to, as are builders in a build world.
// HIGHEST, after every other listener has had the click, since denying the block makes
// the event count as cancelled for the ones that ignore cancelled events
public class BlockUseListener implements Listener {
    private static final Set<Material> SWITCHES = Set.of(Material.LEVER, Material.REPEATER, Material.COMPARATOR,
            Material.DAYLIGHT_DETECTOR, Material.NOTE_BLOCK, Material.FLOWER_POT, Material.COMPOSTER,
            Material.CAKE, Material.BELL);

    private final MineManager mines;
    private final BuildManager build;

    public BlockUseListener(Aurora plugin) {
        this.mines = plugin.getMineManager();
        this.build = plugin.getBuildManager();
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onUse(PlayerInteractEvent event) {
        Block block = event.getClickedBlock();
        if (block == null || free(event.getPlayer())) return;

        if (event.getAction() == Action.PHYSICAL) {
            if (block.getType() == Material.FARMLAND) event.setUseInteractedBlock(Event.Result.DENY);
            return;
        }
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        if (switches(block.getType())) event.setUseInteractedBlock(Event.Result.DENY);
        ItemStack item = event.getItem();
        if (item != null && reshapes(item.getType())) event.setUseItemInHand(Event.Result.DENY);
    }

    private boolean free(Player player) {
        if (build.isBuildWorld(player.getWorld())) return build.canBuild(player, player.getWorld());
        return mines.breaksForReal(player);
    }

    private static boolean switches(Material type) {
        return Tag.DOORS.isTagged(type) || Tag.TRAPDOORS.isTagged(type) || Tag.FENCE_GATES.isTagged(type)
                || Tag.BUTTONS.isTagged(type) || Tag.CANDLES.isTagged(type) || SWITCHES.contains(type);
    }

    // Axes strip and scrape, shovels make paths, hoes till
    private static boolean reshapes(Material type) {
        return Tag.ITEMS_AXES.isTagged(type) || Tag.ITEMS_SHOVELS.isTagged(type) || Tag.ITEMS_HOES.isTagged(type);
    }
}
