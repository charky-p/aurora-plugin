package org.charky.aurora.core.database;

import org.charky.aurora.features.moderation.Punishment;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

// The punishments table: every mute, ban and kick, kept after it ends as the history
public class PunishmentStore {
    private final Database db;

    public PunishmentStore(Database db) {
        this.db = db;
    }

    public List<Punishment> loadAll() {
        List<Punishment> all = new ArrayList<>();
        db.query("SELECT * FROM punishments ORDER BY id", pst -> {}, rs -> all.add(new Punishment(
                rs.getInt("id"), UUID.fromString(rs.getString("uuid")), rs.getString("name"),
                Punishment.Type.valueOf(rs.getString("type")), rs.getString("reason"), rs.getString("by_name"),
                rs.getLong("created"), rs.getLong("until"), rs.getInt("lifted") == 1)));
        return all;
    }

    public void add(Punishment punishment) {
        db.update("INSERT INTO punishments (uuid, name, type, reason, by_name, created, until, lifted) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, 0)", pst -> {
            pst.setString(1, punishment.uuid().toString());
            pst.setString(2, punishment.name());
            pst.setString(3, punishment.type().name());
            pst.setString(4, punishment.reason());
            pst.setString(5, punishment.by());
            pst.setLong(6, punishment.created());
            pst.setLong(7, punishment.until());
        });
    }

    public void lift(UUID uuid, Punishment.Type type) {
        db.update("UPDATE punishments SET lifted = 1 WHERE uuid = ? AND type = ? AND lifted = 0", pst -> {
            pst.setString(1, uuid.toString());
            pst.setString(2, type.name());
        });
    }
}
