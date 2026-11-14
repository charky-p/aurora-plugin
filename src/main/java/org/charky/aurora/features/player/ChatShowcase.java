package org.charky.aurora.features.player;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextReplacementConfig;
import net.kyori.adventure.text.event.ClickCallback;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.charky.aurora.common.util.TextUtils;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.regex.Pattern;

// Showing things off in chat. [item] or \item in a message becomes the held item,
// hover it to read it. [inv] or \inv becomes a link anyone can click to look at the
// sender's inventory as it was when they sent the message
public class ChatShowcase implements Listener {
    private static final Pattern ITEM = Pattern.compile("(?i)\\[(?:item|hand)]|\\\\(?:item|hand)\\b");
    private static final Pattern INVENTORY = Pattern.compile("(?i)\\[(?:inv|inventory)]|\\\\inv(?:entory)?\\b");
    // How long an inventory link in chat stays clickable
    private static final Duration LINK_LIFETIME = Duration.ofMinutes(10);

    // The copy laid out like the real thing: storage on top, then the hotbar,
    // then armour and the off hand
    private static final int VIEW_SIZE = 45;
    private static final int HOTBAR_ROW = 27;
    private static final int ARMOUR_ROW = 36;
    private static final int OFF_HAND_SLOT = 44;

    // LOW, so the rank renderer set later still wraps the changed message
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onChat(AsyncChatEvent event) {
        Player player = event.getPlayer();
        Component message = event.message();

        ItemStack held = player.getInventory().getItemInMainHand();
        if (!held.getType().isAir()) {
            Component shown = itemComponent(held.clone());
            message = message.replaceText(TextReplacementConfig.builder().match(ITEM).replacement(shown).build());
        }

        // Checked first, a link holds a copy of the inventory for as long as it lives
        if (INVENTORY.matcher(PlainTextComponentSerializer.plainText().serialize(message)).find()) {
            Component link = inventoryLink(player);
            message = message.replaceText(TextReplacementConfig.builder().match(INVENTORY).once()
                    .replacement(link).build());
        }

        event.message(message);
    }

    private static Component itemComponent(ItemStack item) {
        Component name = item.effectiveName();
        if (item.getAmount() > 1) name = name.append(Component.text(" x" + item.getAmount(), NamedTextColor.GRAY));

        return Component.text("[", NamedTextColor.DARK_GRAY)
                .append(name)
                .append(Component.text("]", NamedTextColor.DARK_GRAY))
                .hoverEvent(item.asHoverEvent());
    }

    private static Component inventoryLink(Player player) {
        String title = player.getName() + "'s Inventory";
        ItemStack[] copy = snapshot(player.getInventory());

        return Component.text("[" + title + "]", NamedTextColor.AQUA)
                .hoverEvent(Component.text("Click to look", NamedTextColor.GRAY))
                .clickEvent(ClickEvent.callback(viewer -> {
                    if (viewer instanceof Player looking) looking.openInventory(new View(title, copy).getInventory());
                }, ClickCallback.Options.builder().uses(ClickCallback.UNLIMITED_USES).lifetime(LINK_LIFETIME).build()));
    }

    private static ItemStack[] snapshot(PlayerInventory inventory) {
        ItemStack[] copy = new ItemStack[VIEW_SIZE];
        // Storage is slots 9 to 35, the hotbar 0 to 8
        for (int slot = 9; slot < 36; slot++) copy[slot - 9] = clone(inventory.getItem(slot));
        for (int slot = 0; slot < 9; slot++) copy[HOTBAR_ROW + slot] = clone(inventory.getItem(slot));

        copy[ARMOUR_ROW] = clone(inventory.getHelmet());
        copy[ARMOUR_ROW + 1] = clone(inventory.getChestplate());
        copy[ARMOUR_ROW + 2] = clone(inventory.getLeggings());
        copy[ARMOUR_ROW + 3] = clone(inventory.getBoots());
        copy[OFF_HAND_SLOT] = clone(inventory.getItemInOffHand());
        return copy;
    }

    private static ItemStack clone(ItemStack item) {
        return item == null || item.getType().isAir() ? null : item.clone();
    }

    // Look, don't touch
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onClick(InventoryClickEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof View) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDrag(InventoryDragEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof View) event.setCancelled(true);
    }

    // A read only copy of someone's inventory
    private static final class View implements InventoryHolder {
        private final Inventory inventory;

        private View(String title, ItemStack[] contents) {
            this.inventory = Bukkit.createInventory(this, VIEW_SIZE, TextUtils.parse("<dark_gray>" + title));
            for (int slot = 0; slot < contents.length; slot++) {
                if (contents[slot] != null) inventory.setItem(slot, contents[slot].clone());
            }
        }

        @Override
        public @NotNull Inventory getInventory() {
            return inventory;
        }
    }
}
