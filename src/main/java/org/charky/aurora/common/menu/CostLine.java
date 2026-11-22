package org.charky.aurora.common.menu;

import net.kyori.adventure.text.Component;
import org.charky.aurora.common.loot.MenuItem;
import org.charky.aurora.common.util.TextUtils;

// One cost in a menu's lore: the resource, then how much the player has of how much
// it takes, green once there is enough. The same in every shop
public final class CostLine {
    private CostLine() {
    }

    public static Component of(MenuItem cost, long have) {
        String colour = have >= cost.getAmount() ? "<green>" : "<red>";
        return TextUtils.parse("<!italic><gray>» ").append(cost.getItem().effectiveName())
                .append(TextUtils.parse(" " + colour + "(" + String.format("%,d", have) + "/"
                        + String.format("%,d", cost.getAmount()) + ")"));
    }
}
