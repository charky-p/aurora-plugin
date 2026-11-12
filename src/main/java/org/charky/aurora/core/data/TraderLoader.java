package org.charky.aurora.core.data;

import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.entity.EntityType;
import org.charky.aurora.core.models.TraderModel;
import org.charky.aurora.features.trader.Trader;
import org.charky.aurora.features.trader.TraderMenu;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

// Builds the trader NPC definitions, each opening one menu
public class TraderLoader {
    private final Logger logger;

    public TraderLoader(Logger logger) {
        this.logger = logger;
    }

    public Map<String, Trader> load(Map<String, TraderModel> data, Map<String, TraderMenu> guis) {
        Map<String, Trader> traders = new HashMap<>();
        data.forEach((id, model) -> {
            NamespacedKey key = NamespacedKey.fromString("minecraft:" + model.entity.toLowerCase());
            EntityType type = (key != null) ? Registry.ENTITY_TYPE.get(key) : null;
            if (type == null) {
                logger.warning("Invalid EntityType '" + model.entity + "' for trader: " + id);
                type = EntityType.VILLAGER;
            }

            traders.put(id, new Trader(id,
                    MiniMessage.miniMessage().deserialize(model.display), type, guis.get(model.gui)));
        });
        return traders;
    }
}
