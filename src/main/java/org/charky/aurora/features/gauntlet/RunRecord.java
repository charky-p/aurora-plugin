package org.charky.aurora.features.gauntlet;

// One finished run in a player's history: when it ended, the floor it ended on and the
// floors cleared, where it started, its heat and party size, how long it took, the
// player's kills, what they banked, whether they fell or left, and their boons as
// "might:2,cleave:1"
public record RunRecord(long ended, int floor, int cleared, int startFloor, int heat, int party, int seconds, int kills,
                        int shards, int cores, boolean died, String boons) {
}
