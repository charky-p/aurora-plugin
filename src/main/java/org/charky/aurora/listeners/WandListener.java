package org.charky.aurora.listeners;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.charky.aurora.Aurora;
import org.charky.aurora.features.loot.IndependentLootPool;
import org.charky.aurora.features.mob.MobData;
import org.charky.aurora.managers.WandManager;
import org.charky.aurora.util.MessageType;
import org.charky.aurora.util.TextUtils;

import java.util.*;

public class WandListener implements Listener {
    private final NamespacedKey dataKey;
    private final NamespacedKey wandKey;
    private final Map<UUID, MobData> clipboard = new HashMap<>();
    private final WandManager wandManager;

    public WandListener(Aurora plugin) {
        this.dataKey = new NamespacedKey(plugin, "copied_entity");
        this.wandKey = new NamespacedKey(plugin, "is_wand");
        this.wandManager = plugin.getWandManager();
    }

//    @EventHandler
//    public void onCopy(EntityDamageByEntityEvent event) {
//        if (!(event.getDamager() instanceof Player player)) return;
//        ItemStack item = player.getInventory().getItemInMainHand();
//        if (!isWand(item)) return;
//
//        event.setCancelled(true);
//        Entity target = event.getEntity();
//
//        if (target instanceof LivingEntity living) {
//            UUID tempId = UUID.randomUUID();
//            Component name = living.customName();
//            if (name == null) {
//                name = Component.translatable(living.getType().translationKey());
//            }
//
//            Map<EquipmentSlot, ItemStack> equipment = new HashMap<>();
//            for (EquipmentSlot slot : EquipmentSlot.values()) {
//                ItemStack equipped = living.getEquipment().getItem(slot);
//
//                if (equipped.getType() != Material.AIR) {
//                    equipment.put(slot, equipped.clone());
//                }
//            }
//
//            Map<Attribute, Double> attributes = new HashMap<>();
//            for (Attribute attr : Registry.ATTRIBUTE) {
//                var instance = living.getAttribute(attr);
//                if (instance != null) {
//                    attributes.put(attr, instance.getBaseValue());
//                }
//            }
//
//            MobData data = new MobData(tempId.toString(), living.getType(), name, equipment, attributes, new IndependentLootPool());
//            clipboard.put(tempId, data);
//
//            item.editMeta(meta -> {
//                meta.getPersistentDataContainer().set(dataKey, PersistentDataType.STRING, tempId.toString());
//                meta.lore(List.of(Component.text("Clipboard ID: " + tempId.toString().substring(0, 8)).color(NamedTextColor.YELLOW)));
//            });
//
//            player.sendMessage("Entity copied to wand clipboard!");
//        }
//    }

    @EventHandler
    public void onPaste(PlayerInteractEvent event) {
//        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        ItemStack wand = event.getItem();
        if (!isWand(wand)) return;

//        String idString = wand.getItemMeta().getPersistentDataContainer().get(dataKey, PersistentDataType.STRING);
//        if (idString == null) return;
//
//        UUID tempId = UUID.fromString(idString);
//        MobData data = clipboard.get(tempId);
//
//        if (data == null) {
//            event.getPlayer().sendMessage(Component.text("Clipboard data expired or not found!").color(NamedTextColor.RED));
//            return;
//        }

        event.setCancelled(true);

        if (event.getClickedBlock() == null) return;
        Player player = event.getPlayer();
        Location loc = event.getClickedBlock().getLocation();

        if (event.getAction() == Action.LEFT_CLICK_BLOCK) {
            wandManager.setPos1(player.getUniqueId(), loc);
            TextUtils.format(player, "Position 1 set to: " + loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ(), MessageType.SUCCESS);
        } else if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            wandManager.setPos2(player.getUniqueId(), loc);
            TextUtils.format(player, "Position 2 set to: " + loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ(), MessageType.SUCCESS);
        }
        // For paste, temporarily adding other thing
//        Location spawnLoc = event.getClickedBlock().getRelative(event.getBlockFace()).getLocation().add(0.5, 0, 0.5);
//
//        Entity spawned = spawnLoc.getWorld().spawnEntity(spawnLoc, data.type());
//        if (spawned instanceof LivingEntity living) {
//            data.apply(living);
//            event.getPlayer().sendMessage(Component.text("Entity pasted successfully!").color(NamedTextColor.GREEN));
//        }
    }

    private boolean isWand(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        return item.getItemMeta().getPersistentDataContainer().has(wandKey, PersistentDataType.BOOLEAN);
    }
}