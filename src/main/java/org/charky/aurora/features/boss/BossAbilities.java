package org.charky.aurora.features.boss;

// What a boss does during its fight besides swinging. A boss without any of its
// own still gets the boss bar
public interface BossAbilities {
    BossAbilities NONE = new BossAbilities() {
    };

    // Every BossManager.TICKS while the boss is alive
    default void tick(BossFight fight) {
    }

    // A hit is about to kill the boss. True if it saved itself and set its own health
    default boolean lastStand(BossFight fight) {
        return false;
    }

    // The boss died or is gone, clean up whatever it left behind
    default void end(BossFight fight) {
    }
}
