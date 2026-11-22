package org.charky.aurora.features.gauntlet;

// Duel ratings, the chess way: a win takes points from the loser, more the less the
// winner was expected to win, at most k. Every player starts at START
public final class GauntletElo {
    public static final int START = 1000;

    private GauntletElo() {
    }

    // The winner's and the loser's ratings after a duel
    public static int[] after(int winner, int loser, double k) {
        double expected = 1 / (1 + Math.pow(10, (loser - winner) / 400.0));
        int change = (int) Math.max(1, Math.round(k * (1 - expected)));
        return new int[]{winner + change, loser - change};
    }
}
