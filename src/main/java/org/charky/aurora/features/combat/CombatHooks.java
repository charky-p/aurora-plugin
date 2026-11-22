package org.charky.aurora.features.combat;

import org.bukkit.entity.Player;

// Lets a game mode change hits without the damage rules knowing about it: the
// gauntlet's run upgrades and weapons. Both default to leaving the hit as it is
public final class CombatHooks {
    public interface Outgoing {
        // What a player's hit on a mob is multiplied by. Ranged for an arrow or a thrown trident
        double multiplier(Player attacker, boolean ranged);
    }

    public interface Incoming {
        // A mob's hit on a player, in health points, after armour. 0 or less and it misses
        double damage(Player victim, double damage);
    }

    private static Outgoing outgoing = (attacker, ranged) -> 1.0;
    private static Incoming incoming = (victim, damage) -> damage;

    private CombatHooks() {
    }

    public static void set(Outgoing out, Incoming in) {
        outgoing = out;
        incoming = in;
    }

    static double outgoing(Player attacker, boolean ranged) {
        return outgoing.multiplier(attacker, ranged);
    }

    static double incoming(Player victim, double damage) {
        return incoming.damage(victim, damage);
    }
}
