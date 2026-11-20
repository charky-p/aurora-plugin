package org.charky.aurora.core.database;

import org.charky.aurora.features.suggestion.Suggestion;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

// The suggestions table, what players sent with /suggest
public class SuggestionStore {
    private final Database db;

    public SuggestionStore(Database db) {
        this.db = db;
    }

    // Oldest first
    public List<Suggestion> loadAll() {
        List<Suggestion> suggestions = new ArrayList<>();
        db.query("SELECT * FROM suggestions ORDER BY id", pst -> {}, rs -> suggestions.add(new Suggestion(rs.getInt("id"),
                UUID.fromString(rs.getString("uuid")), rs.getString("name"), rs.getString("text"), rs.getLong("time"))));
        return suggestions;
    }

    // The id is given, not made by the database, so the suggestion has it before the save runs
    public void add(Suggestion suggestion) {
        db.update("INSERT INTO suggestions (id, uuid, name, text, time) VALUES (?, ?, ?, ?, ?)", pst -> {
            pst.setInt(1, suggestion.id());
            pst.setString(2, suggestion.uuid().toString());
            pst.setString(3, suggestion.name());
            pst.setString(4, suggestion.text());
            pst.setLong(5, suggestion.time());
        });
    }

    public void delete(int id) {
        db.update("DELETE FROM suggestions WHERE id = ?", pst -> pst.setInt(1, id));
    }
}
