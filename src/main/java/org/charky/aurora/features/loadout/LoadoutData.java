package org.charky.aurora.features.loadout;

import org.bukkit.entity.Player;

import java.util.UUID;

// Anything that keeps per-player data belonging to a loadout. It swaps what it holds in
// memory under the player's own id with what it holds under the id the other loadout is
// put away under. The database rows behind it are swapped by the LoadoutStore
public interface LoadoutData {
    void swapLoadout(Player player, UUID other);
}
