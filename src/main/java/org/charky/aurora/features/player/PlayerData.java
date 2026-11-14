package org.charky.aurora.features.player;

// One player's profile. Changed on the main thread and saved from the database
// thread, so every method is synchronized
public class PlayerData {
    // The id of the rank shown by their name, see Ranks
    private String rank;
    private int level;
    // XP into the current level, not a lifetime total
    private long xp;
    private int kills;
    private int mined;
    private int essence;
    // Seconds online, what the playtime ranks are earned with
    private long playtime;
    private boolean dirty = false;

    public PlayerData(String rank, int level, int kills, int mined, int essence) {
        this(rank, level, 0, kills, mined, essence, 0);
    }

    public PlayerData(String rank, int level, long xp, int kills, int mined, int essence, long playtime) {
        this.rank = rank;
        this.level = level;
        this.xp = xp;
        this.kills = kills;
        this.mined = mined;
        this.essence = essence;
        this.playtime = playtime;
    }

    public synchronized String getRank() {
        return this.rank;
    }

    public synchronized int getLevel() {
        return this.level;
    }

    public synchronized long getXp() {
        return this.xp;
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

    public synchronized long getPlaytime() {
        return this.playtime;
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

    // False, and nothing taken, when there is not enough
    public synchronized boolean spendEssence(int price) {
        if (price < 0 || this.essence < price) return false;

        this.essence -= price;
        this.dirty = true;
        return true;
    }

    public synchronized void addPlaytime(long seconds) {
        this.playtime += seconds;
        this.dirty = true;
    }

    public synchronized void setRank(String rank) {
        this.rank = rank;
        this.dirty = true;
    }

    // Level and the XP into it move together, see Levels
    public synchronized void setProgress(int level, long xp) {
        this.level = level;
        this.xp = xp;
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
