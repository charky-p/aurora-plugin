package org.charky.aurora.features.combat;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

// Lets a game mode change hits without the damage rules knowing about it: the
// gauntlet's run upgrades and weapons. Every hook leaves the hit as it is by default
public final class CombatHooks {
    public interface Outgoing {
        // What a player's hit on a mob is multiplied by. Ranged for an arrow or a thrown trident
        double multiplier(Player attacker, LivingEntity victim, boolean ranged);
    }

    public interface Incoming {
        // A mob's hit on a player, in health points, after armour. 0 or less and it misses
        double damage(Player victim, LivingEntity source, double damage);
    }

    public interface AfterHit {
        // Told once a player's hit has been dealt to a mob, with what it took off
        void dealt(Player attacker, LivingEntity victim, double dealt, boolean ranged);
    }

    private static Outgoing outgoing = (attacker, victim, ranged) -> 1.0;
    private static Incoming incoming = (victim, source, damage) -> damage;
    private static AfterHit afterHit = (attacker, victim, dealt, ranged) -> {};

    private CombatHooks() {
    }

    public static void set(Outgoing out, Incoming in, AfterHit after) {
        outgoing = out;
        incoming = in;
        afterHit = after;
    }

    static double outgoing(Player attacker, LivingEntity victim, boolean ranged) {
        return outgoing.multiplier(attacker, victim, ranged);
    }

    static double incoming(Player victim, LivingEntity source, double damage) {
        return incoming.damage(victim, source, damage);
    }

    static void afterHit(Player attacker, LivingEntity victim, double dealt, boolean ranged) {
        afterHit.dealt(attacker, victim, dealt, ranged);
    }
}
