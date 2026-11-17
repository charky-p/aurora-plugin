package org.charky.aurora.features.tutorial;

// The steps of the first-join tutorial at the Cobblestone Quarry, in order. Each one
// ends by itself when the player does it. Goal is how many times, 1 for a single action
public enum TutorialStep {
    MINE("Mine cobblestone", "Break blocks in the quarry with your pickaxe", 10),
    BACKPACK("Open your backpack", "Type /bp, everything you mine goes in there", 1),
    TRADER("Visit the Cobble Trader", "Click the trader beside the quarry", 1),
    UPGRADE("Buy an upgrade", "Click your pickaxe in the trader's menu", 1);

    private final String title;
    private final String hint;
    private final int goal;

    TutorialStep(String title, String hint, int goal) {
        this.title = title;
        this.hint = hint;
        this.goal = goal;
    }

    public String title() {
        return title;
    }

    public String hint() {
        return hint;
    }

    public int goal() {
        return goal;
    }

    public int number() {
        return ordinal() + 1;
    }

    // Null after the last step
    public TutorialStep next() {
        TutorialStep[] steps = values();
        return ordinal() + 1 < steps.length ? steps[ordinal() + 1] : null;
    }

    // Null for anything that is not a step, so a renamed step in the database starts over
    public static TutorialStep of(String name) {
        for (TutorialStep step : values()) {
            if (step.name().equalsIgnoreCase(name)) return step;
        }
        return null;
    }
}
