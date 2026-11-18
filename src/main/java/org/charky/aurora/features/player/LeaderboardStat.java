package org.charky.aurora.features.player;

import org.bukkit.Material;

import java.util.function.ToLongFunction;

// What /leaderboard ranks by. The columns are the playerData table's, fixed here so
// nothing a player types reaches the query
public enum LeaderboardStat {
    LEVEL("Level", Material.EXPERIENCE_BOTTLE, "level", "xp", PlayerData::getLevel, PlayerData::getXp),
    MINED("Blocks Mined", Material.DIAMOND_PICKAXE, "mined", "level", PlayerData::getMined, PlayerData::getLevel),
    KILLS("Mob Kills", Material.IRON_SWORD, "kills", "level", PlayerData::getKills, PlayerData::getLevel),
    ESSENCE("Essence", Material.AMETHYST_SHARD, "essence", "level", PlayerData::getEssence, PlayerData::getLevel),
    PLAYTIME("Playtime", Material.CLOCK, "playtime", "level", PlayerData::getPlaytime, PlayerData::getLevel);

    private final String title;
    private final Material icon;
    private final String column;
    private final String tiebreak;
    private final ToLongFunction<PlayerData> value;
    private final ToLongFunction<PlayerData> second;

    LeaderboardStat(String title, Material icon, String column, String tiebreak,
                    ToLongFunction<PlayerData> value, ToLongFunction<PlayerData> second) {
        this.title = title;
        this.icon = icon;
        this.column = column;
        this.tiebreak = tiebreak;
        this.value = value;
        this.second = second;
    }

    public String title() {
        return title;
    }

    public Material icon() {
        return icon;
    }

    public String column() {
        return column;
    }

    public String tiebreak() {
        return tiebreak;
    }

    public long of(PlayerData data) {
        return value.applyAsLong(data);
    }

    public long tiebreakOf(PlayerData data) {
        return second.applyAsLong(data);
    }

    // How a value reads: playtime in hours and minutes, the rest as a number
    public String format(long amount) {
        if (this == PLAYTIME) return (amount / 3600) + "h " + (amount % 3600 / 60) + "m";
        return String.format("%,d", amount);
    }

    public LeaderboardStat next(int step) {
        LeaderboardStat[] all = values();
        return all[Math.floorMod(ordinal() + step, all.length)];
    }
}
