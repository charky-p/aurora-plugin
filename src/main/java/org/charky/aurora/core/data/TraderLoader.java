package org.charky.aurora.core.data;

import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Keyed;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.charky.aurora.core.models.TraderModel;
import org.charky.aurora.features.trader.Trader;
import org.charky.aurora.features.trader.TraderMenu;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.logging.Logger;

// Builds the trader NPC definitions. A trader opens a menu from the GUI files, or
// runs an action such as upgrades:coal, which opens that zone's upgrades directly
public class TraderLoader {
    private final Logger logger;

    public TraderLoader(Logger logger) {
        this.logger = logger;
    }

    public Map<String, Trader> load(Map<String, TraderModel> data, Map<String, TraderMenu> guis, MenuActions actions) {
        Map<String, Trader> traders = new HashMap<>();
        data.forEach((id, model) -> {
            NamespacedKey key = NamespacedKey.fromString("minecraft:" + model.entity.toLowerCase());
            EntityType type = (key != null) ? Registry.ENTITY_TYPE.get(key) : null;
            if (type == null) {
                logger.warning("Invalid EntityType '" + model.entity + "' for trader: " + id);
                type = EntityType.VILLAGER;
            }

            TraderMenu menu = guis.get(model.gui);
            Consumer<Player> opens = menu != null ? menu::onClick : actions.get(model.gui);
            if (opens == null) {
                logger.severe("TRADER ERROR: '" + id + "' opens '" + model.gui + "', which is no menu and no action");
                return;
            }

            traders.put(id, new Trader(id, MiniMessage.miniMessage().deserialize(model.display), type, opens,
                    lookup(Registry.VILLAGER_PROFESSION, model.profession, id),
                    lookup(Registry.VILLAGER_TYPE, model.biome, id)));
        });
        return traders;
    }

    // A vanilla registry entry by its plain name, null and a warning if there is none
    private <T extends Keyed> T lookup(Registry<T> registry, String name, String trader) {
        if (name == null || name.isBlank()) return null;
        NamespacedKey key = NamespacedKey.fromString("minecraft:" + name.toLowerCase());
        T found = key == null ? null : registry.get(key);
        if (found == null) logger.warning("Trader '" + trader + "': no villager look '" + name + "'");
        return found;
    }
}
