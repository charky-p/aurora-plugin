package org.charky.aurora.features.movement;

import org.bukkit.entity.EntityType;

// Every mount the Essence shop sells, by its perk id without "mount_": what it spawns
// as, how it is steered, and how fast it goes and jumps at levels 1, 2 and 3
enum Mount {
    PIG("pig", EntityType.PIG, Steering.WALK, new double[]{0.32, 0.40, 0.48}),
    CHICKEN("chicken", EntityType.CHICKEN, Steering.WALK, new double[]{0.26, 0.33, 0.40}),
    PHANTOM("phantom", EntityType.PHANTOM, Steering.FLY, new double[]{0.6, 0.8, 1.0}),
    // The level 3 horse keeps its rider in water, the skeleton horse always does
    HORSE("horse", EntityType.HORSE, Steering.VANILLA, new double[]{0.32, 0.37, 0.42}, new double[]{0.9, 1.0, 1.1}, 3),
    SKELETON("skeleton", EntityType.SKELETON_HORSE, Steering.VANILLA, new double[]{0.38, 0.43, 0.48}, new double[]{1.0, 1.05, 1.1}, 1),
    CAMEL("camel", EntityType.CAMEL, Steering.VANILLA, new double[]{0.12, 0.15, 0.18}),
    GHAST("ghast", EntityType.HAPPY_GHAST, Steering.VANILLA_FLYING, new double[]{0.05, 0.07, 0.09});

    // How a mount moves, and so what its speeds mean
    enum Steering {
        // Steered here each tick, walking where the rider looks and presses. Speeds are blocks a tick
        WALK,
        // Steered here each tick, flying where the rider looks. Speeds are blocks a tick
        FLY,
        // Steered the vanilla way. Speeds are its movement speed attribute
        VANILLA,
        // Steered the vanilla way in the air. Speeds are its flying speed attribute
        VANILLA_FLYING
    }

    private static final int NEVER = Integer.MAX_VALUE;

    private final String id;
    private final EntityType type;
    private final Steering steering;
    private final double[] speeds;
    // Its jump strength attribute, empty for a mount that keeps its vanilla jump
    private final double[] jumps;
    // The level from which it keeps its rider in deep water
    private final int swimsFrom;

    Mount(String id, EntityType type, Steering steering, double[] speeds) {
        this(id, type, steering, speeds, new double[0], NEVER);
    }

    Mount(String id, EntityType type, Steering steering, double[] speeds, double[] jumps, int swimsFrom) {
        this.id = id;
        this.type = type;
        this.steering = steering;
        this.speeds = speeds;
        this.jumps = jumps;
        this.swimsFrom = swimsFrom;
    }

    // Null for a saddle of a mount that no longer exists
    static Mount byId(String id) {
        for (Mount mount : values()) {
            if (mount.id.equalsIgnoreCase(id)) return mount;
        }
        return null;
    }

    EntityType type() {
        return type;
    }

    Steering steering() {
        return steering;
    }

    double speed(int level) {
        return at(speeds, level);
    }

    boolean hasJump() {
        return jumps.length > 0;
    }

    double jump(int level) {
        return at(jumps, level);
    }

    boolean swims(int level) {
        return level >= swimsFrom;
    }

    // Level 1 to 3 picks that level's value, anything outside counts as the nearest
    private static double at(double[] values, int level) {
        return values[Math.max(1, Math.min(level, values.length)) - 1];
    }
}
