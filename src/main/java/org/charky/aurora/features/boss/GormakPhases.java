package org.charky.aurora.features.boss;

// When Gormak the Eternal does what, by how much health he has left. Pure numbers,
// so the pacing of the fight is tested without a server
public final class GormakPhases {
    // What rises beside him in one wave
    public record Wave(int ghouls, int minions, int guardians) {
    }

    // He calls a wave each time his health falls past one of these. By health and not
    // by time, so a strong player still sees every wave
    static final double[] WAVE_THRESHOLDS = {0.75, 0.50, 0.25};
    // Two big stages with smaller waves between: ghouls alone at three quarters and
    // one quarter, his minions with them at half
    static final Wave[] WAVES = {
            new Wave(3, 0, 0),
            new Wave(3, 5, 0),
            new Wave(3, 0, 0),
    };
    // The fourth and last wave, when the totem pops: the second big stage
    static final Wave LAST_STAND = new Wave(3, 5, 2);
    // Never more than this many ghouls alive at once, a wave's ghouls are cut short to fit
    static final int MAX_GHOULS = 6;
    // Share of his health the totem brings him back with
    static final double REVIVE_SHARE = 0.35;

    private GormakPhases() {
    }

    // How many waves he should have called by the time his health is at this ratio
    public static int wavesDue(double healthRatio) {
        int due = 0;
        for (double threshold : WAVE_THRESHOLDS) {
            if (healthRatio <= threshold) due++;
        }
        return due;
    }

    // How many of a wave's ghouls fit under the cap
    public static int ghoulsToSummon(int waveSize, int alive) {
        return Math.clamp(MAX_GHOULS - alive, 0, waveSize);
    }
}
