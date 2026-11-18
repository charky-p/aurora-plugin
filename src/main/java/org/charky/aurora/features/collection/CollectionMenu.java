package org.charky.aurora.features.collection;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.charky.aurora.common.loot.MenuItem;
import org.charky.aurora.common.menu.PagedMenu;
import org.charky.aurora.common.util.TextUtils;
import org.charky.aurora.core.DataRegistry;
import org.charky.aurora.features.armory.ArmoryManager;
import org.charky.aurora.features.tool.ToolManager;
import org.charky.aurora.features.tool.ToolType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

// Two tabs. Resources: every resource in progression order with how many the player
// has ever gathered, the tier that total has reached and what the next one needs and
// pays. Gear: the player's own tools and armour as they are now, then what mobs drop,
// found or not, then keys. Clicking a piece of gear that is yours takes a copy out
public class CollectionMenu extends PagedMenu {
    // In the top border, either side of the middle
    private static final int RESOURCES_TAB = 3;
    private static final int GEAR_TAB = 5;

    private final CollectionManager collections;
    private boolean gearTab = false;
    // What each entry of the Gear tab is: a gear id, or tool:<id>
    private List<String> gearIds = List.of();

    public CollectionMenu(CollectionManager collections, DataRegistry registry) {
        super(registry, "Collections");
        this.collections = collections;
    }

    // Opens on the Gear tab, for the button in the backpack
    public CollectionMenu onGear() {
        gearTab = true;
        return this;
    }

    @Override
    protected List<ItemStack> entries(Player player) {
        return gearTab ? gear(player) : resources(player);
    }

    // Only what is from a released region, admins see everything
    private boolean shown(Player player, String id) {
        return player.hasPermission("aurora.admin") || registry.isItemReleased(id);
    }

    private List<ItemStack> resources(Player player) {
        List<ItemStack> items = new ArrayList<>();
        for (MenuItem resource : registry.getItems()) {
            if (!registry.isLootable(resource.getId()) || !shown(player, resource.getId())) continue;

            long amount = collections.get(player.getUniqueId(), resource.getId());
            // Something is there, but not what, until the player has found one
            if (amount <= 0) {
                items.add(unknown(TextUtils.parse("<!italic><gray>Not found yet")));
                continue;
            }
            int base = registry.getCollectionBase(resource.getId());
            int tier = CollectionTiers.tier(base, amount);
            long next = CollectionTiers.nextThreshold(base, amount);

            ItemStack display = resource.getItem();
            display.editMeta(meta -> {
                List<Component> lore = new ArrayList<>();
                lore.add(TextUtils.parse("<!italic><gray>Collected: <white>" + String.format("%,d", amount)));
                lore.add(TextUtils.parse("<!italic><gray>Tier: " + (tier == 0 ? "<dark_gray>None yet"
                        : "<yellow>" + CollectionTiers.numeral(tier) + " <dark_gray>of " + CollectionTiers.numeral(CollectionTiers.count()))));
                lore.add(Component.empty());
                lore.add(TextUtils.parse(next < 0 ? "<!italic><green>Complete"
                        : "<!italic><gray>Next: <white>" + String.format("%,d", next)
                        + " <dark_gray>(<gold>+" + CollectionTiers.nextReward(base, amount) + " Essence<dark_gray>)"));
                meta.lore(lore);
                if (next < 0) meta.setEnchantmentGlintOverride(true);
            });
            items.add(display);
        }
        return items;
    }

    private List<ItemStack> gear(Player player) {
        List<ItemStack> items = new ArrayList<>();
        ArmoryManager armory = registry.getArmoryManager();
        gearIds = new ArrayList<>();

        // The tools and armour that level up, drawn as they are in this realm
        ToolManager tools = armory.tools();
        if (tools != null) {
            for (String id : registry.getToolIds()) {
                ToolType tool = registry.getToolType(id);
                gearIds.add("tool:" + id);
                items.add(!tools.has(player, tool) ? unknown(TextUtils.parse("<!italic><gray>Not yours yet"))
                        : noted(tools.build(player, tool), tools.carries(player, tool)
                        ? "<gray>You have this one" : "<yellow>Click to take it"));
            }
        }

        for (Map.Entry<String, Component> drop : registry.getArmory().entrySet()) {
            if (!shown(player, drop.getKey())) continue;
            gearIds.add(drop.getKey());
            if (!armory.isUnlocked(player.getUniqueId(), drop.getKey())) {
                items.add(unknown(TextUtils.parse("<!italic><gray>Dropped by ").append(drop.getValue())));
                continue;
            }
            items.add(noted(registry.getAllItems().get(drop.getKey()).clone(),
                    armory.carries(player, drop.getKey()) ? "<gray>You have this one" : "<yellow>Click to take one"));
        }

        // Keys the player has had come last, the Keyring perk lets them be taken again
        boolean keyring = armory.hasKeyring(player);
        for (String id : registry.getKeyIds()) {
            if (!shown(player, id)) continue;
            gearIds.add(id);
            if (!armory.isUnlocked(player.getUniqueId(), id)) {
                items.add(unknown(TextUtils.parse("<!italic><gray>A key you have not found yet")));
                continue;
            }
            items.add(noted(registry.getAllItems().get(id).clone(), !keyring ? "<gray>Found. Used up when it opens the way"
                    : armory.carries(player, id) ? "<gray>You have this one" : "<yellow>Click to take one"));
        }
        return items;
    }

    @Override
    protected void onEntryClick(Player player, int index) {
        if (!gearTab || index >= gearIds.size()) return;

        ArmoryManager armory = registry.getArmoryManager();
        String id = gearIds.get(index);
        if (id.startsWith("tool:")) {
            ToolType tool = registry.getToolType(id.substring("tool:".length()));
            if (tool != null && armory.tools() != null && armory.tools().has(player, tool)) armory.tools().give(player, tool);
        } else {
            armory.retrieve(player, id);
        }
        render(player);
    }

    private static ItemStack unknown(Component line) {
        ItemStack item = button(Material.GRAY_DYE, "<dark_gray>???");
        item.editMeta(meta -> meta.lore(List.of(line)));
        return item;
    }

    // The item with one more line under its own lore
    private static ItemStack noted(ItemStack item, String line) {
        item.editMeta(meta -> {
            List<Component> lore = meta.hasLore() ? new ArrayList<>(meta.lore()) : new ArrayList<>();
            lore.add(Component.empty());
            lore.add(TextUtils.parse("<!italic>" + line));
            meta.lore(lore);
        });
        return item;
    }

    @Override
    protected void decorate(Player player, Inventory inventory) {
        inventory.setItem(RESOURCES_TAB, tab(Material.CHEST, "Resources", !gearTab));
        inventory.setItem(GEAR_TAB, tab(Material.IRON_CHESTPLATE, "Gear", gearTab));
    }

    private static ItemStack tab(Material material, String name, boolean open) {
        ItemStack item = button(material, (open ? "<green><b>" : "<yellow>") + name);
        item.editMeta(meta -> {
            meta.lore(List.of(TextUtils.parse(open ? "<!italic><gray>You are here" : "<!italic><gray>Click to open")));
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            if (open) meta.setEnchantmentGlintOverride(true);
        });
        return item;
    }

    @Override
    protected void onButtonClick(Player player, int slot) {
        if (slot != RESOURCES_TAB && slot != GEAR_TAB) return;
        boolean gear = slot == GEAR_TAB;
        if (gear == gearTab) return;

        gearTab = gear;
        firstPage();
        render(player);
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1f);
    }

    private long found(Player player) {
        return registry.getItems().stream().filter(resource -> registry.isLootable(resource.getId())
                && shown(player, resource.getId()) && collections.get(player.getUniqueId(), resource.getId()) > 0).count();
    }

    @Override
    protected List<Component> info(Player player, List<ItemStack> entries) {
        if (gearTab) {
            ArmoryManager armory = registry.getArmoryManager();
            return List.of(
                    TextUtils.parse("<!italic><gray>Your tools and armour, and every gear drop."),
                    TextUtils.parse("<!italic><gray>Drops found: <white>" + armory.unlockedCount(player.getUniqueId())
                            + "/" + registry.getArmory().keySet().stream().filter(id -> shown(player, id)).count()));
        }

        int reached = 0, all = 0;
        for (MenuItem resource : registry.getItems()) {
            if (registry.isLootable(resource.getId()) && shown(player, resource.getId())) {
                all++;
                reached += CollectionTiers.tier(registry.getCollectionBase(resource.getId()),
                        collections.get(player.getUniqueId(), resource.getId()));
            }
        }
        return List.of(
                TextUtils.parse("<!italic><gray>Every resource you have ever gathered."),
                TextUtils.parse("<!italic><gray>Found: <white>" + found(player) + "/" + all),
                TextUtils.parse("<!italic><gray>Tiers reached: <white>" + reached + "/" + all * CollectionTiers.count()));
    }
}
