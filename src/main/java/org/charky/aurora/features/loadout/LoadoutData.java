package org.charky.aurora.features.loadout;

import org.bukkit.entity.Player;

import java.util.UUID;

// Anything that keeps per-player data belonging to a loadout. It swaps what it holds in
// memory under the player's own id with what it holds under the id the other loadout is
// put away under, or copies it there for a new loadout. The database rows behind it are
// swapped and copied by the LoadoutStore
public interface LoadoutData {
    void swapLoadout(Player player, UUID other);

    // A copy of what the player has in play, under other, sharing nothing with it
    void copyLoadout(Player player, UUID other);
}
