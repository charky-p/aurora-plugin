package org.charky.aurora.features.cosmetic;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;

// What each kill effect looks and sounds like, played where the mob fell. All of
// it is for show, the lightning is the effect only and hurts nothing
final class KillEffects {
    private KillEffects() {
    }

    static void play(String effect, Location at) {
        World world = at.getWorld();
        if (world == null) return;
        Location centre = at.clone().add(0, 1, 0);

        switch (effect) {
            case "flames" -> {
                world.spawnParticle(Particle.FLAME, centre, 40, 0.3, 0.5, 0.3, 0.05);
                world.playSound(centre, Sound.ENTITY_BLAZE_SHOOT, 0.5f, 1.2f);
            }
            case "hearts" -> {
                world.spawnParticle(Particle.HEART, centre, 10, 0.4, 0.5, 0.4, 0);
                world.playSound(centre, Sound.ENTITY_CHICKEN_EGG, 0.6f, 1.6f);
            }
            case "soul" -> {
                world.spawnParticle(Particle.SOUL, centre, 25, 0.3, 0.6, 0.3, 0.04);
                world.playSound(centre, Sound.PARTICLE_SOUL_ESCAPE, 1f, 1f);
            }
            case "firework" -> {
                world.spawnParticle(Particle.FIREWORK, centre, 60, 0.2, 0.2, 0.2, 0.15);
                world.playSound(centre, Sound.ENTITY_FIREWORK_ROCKET_BLAST, 0.6f, 1f);
            }
            case "explosion" -> {
                world.spawnParticle(Particle.EXPLOSION, centre, 3, 0.3, 0.3, 0.3, 0);
                world.playSound(centre, Sound.ENTITY_GENERIC_EXPLODE, 0.4f, 1.3f);
            }
            case "lightning" -> world.strikeLightningEffect(at);
            default -> { }
        }
    }
}
