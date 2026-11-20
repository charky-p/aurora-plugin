package org.charky.aurora.features.suggestion;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.charky.aurora.common.util.TextUtils;
import org.charky.aurora.core.database.Database;
import org.charky.aurora.core.database.SuggestionStore;
import org.charky.aurora.features.system.CommandFilter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

// What players send with /suggest. Every suggestion is saved for staff to read with
// /aurora suggestions, and staff who are online see it the moment it is sent
public class SuggestionManager {
    // One suggestion a minute each, so nobody can flood the list. Staff can send any time
    private static final long COOLDOWN_MILLIS = 60_000L;

    private final Plugin plugin;
    private final Database db;
    private final SuggestionStore store;
    // Oldest first
    private final List<Suggestion> suggestions;
    private final Map<UUID, Long> lastSent = new HashMap<>();
    private int nextId;

    public SuggestionManager(Plugin plugin, Database db, SuggestionStore store) {
        this.plugin = plugin;
        this.db = db;
        this.store = store;
        this.suggestions = new ArrayList<>(store.loadAll());
        this.nextId = suggestions.stream().mapToInt(Suggestion::id).max().orElse(0) + 1;
    }

    // Null if it was sent, otherwise why not
    public String suggest(Player player, String typed) {
        String text = Suggestion.clean(typed);
        String problem = Suggestion.problem(text);
        if (problem != null) return problem;

        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();
        Long last = lastSent.get(uuid);
        if (last != null && now - last < COOLDOWN_MILLIS && !player.hasPermission(CommandFilter.STAFF_PERMISSION)) {
            return "Wait " + ((COOLDOWN_MILLIS - (now - last)) / 1000 + 1) + " seconds before sending another";
        }
        lastSent.put(uuid, now);

        Suggestion suggestion = new Suggestion(nextId++, uuid, player.getName(), text, now);
        suggestions.add(suggestion);
        db.async(() -> store.add(suggestion));
        plugin.getLogger().info("Suggestion #" + suggestion.id() + " from " + suggestion.name() + ": " + text);
        tellStaff(suggestion);
        return null;
    }

    // What the player wrote is shown as plain text, never read as colours or click events
    private static void tellStaff(Suggestion suggestion) {
        Component message = TextUtils.parse("<gold><b>SUGGESTION</b> <dark_gray>#" + suggestion.id() + " <white>")
                .append(Component.text(suggestion.name(), NamedTextColor.WHITE))
                .append(Component.text(": " + suggestion.text(), NamedTextColor.GRAY));
        for (Player staff : Bukkit.getOnlinePlayers()) {
            if (staff.hasPermission(CommandFilter.STAFF_PERMISSION)) staff.sendMessage(message);
        }
    }

    public List<Suggestion> newestFirst() {
        return List.copyOf(suggestions.reversed());
    }

    // False if there is no suggestion with that id
    public boolean delete(int id) {
        if (!suggestions.removeIf(suggestion -> suggestion.id() == id)) return false;
        db.async(() -> store.delete(id));
        return true;
    }
}
