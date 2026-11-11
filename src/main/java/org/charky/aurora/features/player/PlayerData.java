package org.charky.aurora.features.player;

public class PlayerData {
    private String rank;
    private int level;
    private int kills;
    private int mined;
    private int essence;
    private boolean dirty = false;

    public PlayerData(String rank, int level, int kills, int mined, int essence) {
        this.rank = rank;
        this.level = level;
        this.kills = kills;
        this.mined = mined;
        this.essence = essence;
    }

    public synchronized String getRank() {
        return this.rank;
    }

    public synchronized int getLevel() {
        return this.level;
    }

    public synchronized int getKills() {
        return this.kills;
    }

    public synchronized int getMined() {
        return this.mined;
    }

    public synchronized int getEssence() {
        return this.essence;
    }

    public synchronized boolean isDirty() {
        return dirty;
    }

    public synchronized void resetDirty() {
        this.dirty = false;
    }

    // A save that failed puts the flag back so the next autosave retries it
    public synchronized void markDirty() {
        this.dirty = true;
    }

    public synchronized void addKills(int kills) {
        this.kills += kills;
        this.dirty = true;
    }

    public synchronized void addMined(int mined) {
        this.mined += mined;
        this.dirty = true;
    }

    public synchronized void addEssence(int essence) {
        this.essence += essence;
        this.dirty = true;
    }

    public synchronized void setRank(String rank) {
        this.rank = rank;
        this.dirty = true;
    }

    public synchronized void setLevel(int level) {
        this.level = level;
        this.dirty = true;
    }

    public synchronized void setKills(int kills) {
        this.kills = kills;
        this.dirty = true;
    }

    public synchronized void setMined(int mined) {
        this.mined = mined;
        this.dirty = true;
    }

    public synchronized void setEssence(int essence) {
        this.essence = essence;
        this.dirty = true;
    }
}
