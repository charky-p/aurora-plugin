package org.charky.aurora.features.tool;

import net.kyori.adventure.text.Component;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.charky.aurora.common.util.TextUtils;
import org.charky.aurora.core.Aurora;
import org.charky.aurora.core.DataRegistry;
import org.charky.aurora.features.player.BackpackData;
import org.charky.aurora.features.tool.Realms.Realm;

import java.util.List;
import java.util.function.BiConsumer;

// The zones' upgrade shops: what a player can buy next for a tool, why a step is
// locked, buying it, and whether a zone, a region or the whole game is maxed. The
// levels themselves are ToolManager's
public class UpgradeShop {
    private final Aurora plugin;
    private final DataRegistry registry;
    private final ToolManager tools;

    // Told when a player buys the last upgrade a zone sells
    private ZoneMaxed onMaxed = (player, zoneId, region) -> {};
    // Told when a player opens a zone's upgrades, and when they buy one. The tutorial listens
    private BiConsumer<Player, String> onOpened = (player, zoneId) -> {};
    private BiConsumer<Player, String> onBought = (player, zoneId) -> {};

    public interface ZoneMaxed {
        void accept(Player player, String zoneId, String region);
    }

    public UpgradeShop(Aurora plugin, ToolManager tools) {
        this.plugin = plugin;
        this.registry = plugin.getRegistry();
        this.tools = tools;
    }

    public void setOnMaxed(ZoneMaxed onMaxed) {
        this.onMaxed = onMaxed;
    }

    public void setOnOpened(BiConsumer<Player, String> onOpened) {
        this.onOpened = onOpened;
    }

    public void setOnBought(BiConsumer<Player, String> onBought) {
        this.onBought = onBought;
    }

    public ToolManager tools() {
        return tools;
    }

    // Null while the backpack is still loading
    public BackpackData backpackOf(Player player) {
        return plugin.getBackpackManager().get(player.getUniqueId());
    }

    // A zone's steps count against what the player has in the zone's own realm. The
    // unlock is 1 once the tool is theirs, in any realm
    private int own(Player player, UpgradeZone zone, String tool, String stat) {
        if (stat.equals(ToolLevels.UNLOCK)) return tools.owns(player.getUniqueId(), tool) ? 1 : 0;
        return tools.level(player.getUniqueId(), zone.realm(), tool, stat);
    }

    public UpgradeZone.Step next(Player player, UpgradeZone zone, String tool) {
        return zone.next(tool, stat -> own(player, zone, tool, stat));
    }

    // Behind while a zone played before this one still sells the tool something
    public UpgradeZone.Status status(Player player, UpgradeZone zone, String tool) {
        UpgradeZone.Status status = zone.status(tool, stat -> own(player, zone, tool, stat));
        return status == UpgradeZone.Status.OPEN && unfinished(player, zone, tool) != null ? UpgradeZone.Status.BEHIND : status;
    }

    // The first zone before this one with a step of the tool not bought yet, null when
    // there is none. A released zone never waits on one not out yet, players cannot buy
    // there. In a zone not out yet, which only admins reach, every zone before it counts
    public UpgradeZone unfinished(Player player, UpgradeZone zone, String tool) {
        boolean released = registry.isZoneReleased(zone.id());
        for (String id : zone.before()) {
            UpgradeZone earlier = registry.getUpgradeZone(id);
            if (earlier == null || (released && !registry.isZoneReleased(id))) continue;
            if (next(player, earlier, tool) != null) return earlier;
        }
        return null;
    }

    // Why a tool is behind, for the shop to say. Only behind while an earlier zone is
    // unfinished, so there is always one to name
    public String behindReason(Player player, UpgradeZone zone, ToolType tool) {
        UpgradeZone earlier = unfinished(player, zone, tool.id());
        return "Buy every " + tool.name() + " upgrade at " + (earlier == null ? "the zones before" : earlier.name()) + " first";
    }

    // How many steps of a tool's chain in a zone are bought
    public int bought(Player player, UpgradeZone zone, String tool) {
        int bought = 0;
        for (UpgradeZone.Step step : zone.chain(tool)) {
            if (own(player, zone, tool, step.stat()) >= step.level()) bought++;
        }
        return bought;
    }

    // Every upgrade the zone sells is bought
    public boolean maxed(Player player, String zoneId) {
        UpgradeZone zone = registry.getUpgradeZone(zoneId);
        if (zone == null || zone.tools().isEmpty()) return false;
        for (String tool : zone.tools()) {
            if (status(player, zone, tool) != UpgradeZone.Status.MAXED) return false;
        }
        return true;
    }

    // Every released zone is maxed, what the Overworld Core needs. The ones not out
    // yet cannot be bought
    public boolean maxedAll(Player player) {
        for (String zone : registry.getReleasedZoneIds()) {
            if (!maxed(player, zone)) return false;
        }
        return true;
    }

    // Every zone of a world is maxed. False for a world with no zones yet
    public boolean maxedRegion(Player player, String region) {
        List<String> zones = registry.getZonesOfRegion(region);
        if (zones.isEmpty()) return false;
        for (String zone : zones) {
            if (!maxed(player, zone)) return false;
        }
        return true;
    }

    // "Fortune 8", or "Iron Pickaxe" for a material. The number is what the tool
    // will have in all once the step is bought, not the level within the zone's
    // realm, since realms add up
    public Component describe(Player player, UpgradeZone zone, ToolType tool, UpgradeZone.Step step) {
        if (step.stat().equals(ToolLevels.MATERIAL) || step.stat().equals(ToolLevels.UNLOCK)) {
            String material = ToolLevels.material(tool.materials(), step.stat().equals(ToolLevels.UNLOCK) ? 0 : step.level());
            if (material.equalsIgnoreCase(ToolLevels.NO_MATERIAL)) return Component.text(tool.name());
            return Component.text(material.charAt(0) + material.substring(1).toLowerCase() + " " + tool.name());
        }
        Realm realm = tools.realmOf(player);
        int total = realm == null ? 0 : tools.effective(player.getUniqueId(), realm, tool.id()).getOrDefault(step.stat(), 0);
        int after = total - own(player, zone, tool.id(), step.stat()) + step.level();
        return TextUtils.parse(registry.getEnchantDisplay(step.stat()) + " " + after);
    }

    // Null if the next step of the tool's chain was bought, otherwise why not
    public String buy(Player player, UpgradeZone zone, String toolId) {
        ToolType tool = registry.getToolType(toolId);
        BackpackData backpack = backpackOf(player);
        if (tool == null || backpack == null) return "Not ready yet, try again in a moment";

        UpgradeZone.Step step = next(player, zone, toolId);
        if (step == null) return "That tool is maxed here";
        if (status(player, zone, toolId) == UpgradeZone.Status.BEHIND) return behindReason(player, zone, tool);
        boolean unlock = step.stat().equals(ToolLevels.UNLOCK);
        if (!unlock && !tools.owns(player.getUniqueId(), toolId)) return "You need to buy the " + tool.name() + " first";
        if (!backpack.hasItems(player, step.costs())) return "Not enough items";

        backpack.removeItems(player, step.costs());
        Component bought = describe(player, zone, tool, step);
        // Bought before it is handed over, so a full inventory never costs the tool
        if (unlock) tools.set(player.getUniqueId(), zone.realm(), toolId, ToolLevels.MATERIAL, 0);
        else tools.set(player.getUniqueId(), zone.realm(), toolId, step.stat(), step.level());
        if (!tools.carries(player, tool) && !tools.give(player, tool)) {
            TextUtils.error(player, "Your inventory is full, click the trader again to take it.");
        }

        player.sendMessage(TextUtils.parse("<gold><b>UPGRADED</b> <gray>" + tool.name() + ": <white>").append(bought));
        player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_USE, 0.5f, 1.4f);
        if (maxed(player, zone.id())) onMaxed.accept(player, zone.id(), registry.getZoneRegion(zone.id()));
        onBought.accept(player, zone.id());
        return null;
    }

    public void open(Player player, String zoneId) {
        UpgradeZone zone = registry.getUpgradeZone(zoneId);
        if (zone == null) {
            TextUtils.error(player, "There are no upgrades here yet.");
            return;
        }
        new UpgradeMenu(this, registry, zone).open(player);
        onOpened.accept(player, zoneId);
    }
}
