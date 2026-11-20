package org.charky.aurora.features.suggestion;

import java.util.UUID;

// One thing a player sent with /suggest. The name is the one they had when they sent
// it, time is when, in milliseconds
public record Suggestion(int id, UUID uuid, String name, String text, long time) {
    // Most a suggestion may be, about two lines of chat
    public static final int MAX_LENGTH = 256;

    // What they typed with the spaces tidied, every run of them one space
    public static String clean(String text) {
        return text == null ? "" : text.strip().replaceAll("\\s+", " ");
    }

    // Null when the cleaned text can be sent, otherwise why not
    public static String problem(String text) {
        if (text.isEmpty()) return "Write your suggestion after /suggest";
        if (text.length() > MAX_LENGTH) return "That is " + text.length() + " characters, keep it under " + MAX_LENGTH;
        return null;
    }
}
