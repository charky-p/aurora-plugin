package org.charky.aurora.features.cosmetic;

import org.bukkit.Material;
import org.charky.aurora.features.cosmetic.Cosmetic.Slot;
import org.charky.aurora.features.cosmetic.Cosmetic.Source;
import org.charky.aurora.features.cosmetic.Cosmetic.Type;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// Every cosmetic there is, in the order the menus show them
public final class Cosmetics {
    private static final long HOUR = 3600;

    // Not a cosmetic itself: owning this id lets a player wear a second suffix
    public static final String SECOND_SUFFIX_SLOT = "suffix_slot_2";
    // What a slot holds once the player has taken its cosmetic off
    public static final String NONE = "none";

    // What chat and names look like with nothing chosen
    public static final String DEFAULT_CHAT_COLOR = "gray";
    public static final String DEFAULT_NAME_COLOR = "white";
    // <player> is replaced by the name of whoever joined
    public static final String PLAYER_TAG = "<player>";
    // In a death message, whoever did it. A death message is two lines, the first
    // for a death with a killer and the second for one without
    public static final String KILLER_TAG = "<killer>";
    public static final String DEATH_SEP = "||";

    private static final Map<String, Cosmetic> ALL = new LinkedHashMap<>();

    // name, the colour's own name in chat formatting, its dye
    private static final String[][] COLORS = {
            {"light_gray", "gray", "LIGHT_GRAY_DYE"}, {"white", "white", "WHITE_DYE"},
            {"yellow", "yellow", "YELLOW_DYE"}, {"orange", "gold", "ORANGE_DYE"},
            {"red", "red", "RED_DYE"}, {"lime", "green", "LIME_DYE"},
            {"green", "dark_green", "GREEN_DYE"}, {"pink", "light_purple", "PINK_DYE"},
            {"purple", "dark_purple", "PURPLE_DYE"}, {"light_blue", "aqua", "LIGHT_BLUE_DYE"},
            {"cyan", "dark_aqua", "CYAN_DYE"}, {"blue", "blue", "BLUE_DYE"},
    };
    private static final int CHAT_COLOR_PRICE = 300;
    private static final int NAME_COLOR_PRICE = 500;

    static {
        // Suffixes: time played, then bought with Essence cheapest first, then donor
        suffix("new", "<gray>[<dark_gray>NEW<gray>]", Source.DEFAULT, 0, "Yours from your first join");
        suffix("regular", "<gray>[<dark_gray>REGULAR<gray>]", Source.PLAYTIME, 10 * HOUR, null);
        suffix("veteran", "<gray>[<dark_gray>VETERAN<gray>]", Source.PLAYTIME, 100 * HOUR, null);

        bought("advanced", "<green>", 100);
        bought("elite", "<aqua>", 150);
        bought("hero", "<yellow>", 200);
        bought("warrior", "<red>", 300);
        bought("champion", "<gold>", 400);
        bought("legend", "<light_purple>", 500);
        bought("titan", "<dark_aqua>", 650);
        bought("colossal", "<dark_green>", 800);
        bought("invincible", "<blue>", 1_000);
        bought("legendary", "<gold><b>", 1_250);
        bought("immortal", "<dark_red><b>", 1_500);
        bought("supreme", "<dark_purple><b>", 1_750);
        bought("ultimate", "<red><b>", 2_000);
        bought("apex", "<gradient:#FF8008:#E52D27><b>", 2_500);
        bought("nexus", "<gradient:#56CCF2:#2F80ED><b>", 3_000);
        bought("radiant", "<gradient:#FFF200:#F7971E><b>", 3_500);
        bought("paragon", "<gradient:#38EF7D:#11998E><b>", 4_000);
        bought("divine", "<gradient:#FFFFFF:#FFD700><b>", 5_000);
        bought("eternal", "<gradient:#B47CFF:#3A1C71><b>", 6_000);
        bought("ascendant", "<gradient:#7F7FD5:#86A8E7:#91EAE4><b>", 7_500);

        donor("supporter", "<green>[SUPPORTER]", "Luminary");
        donor("donor", "<dark_purple>[DONOR]", "Luminary");
        donor("investor", "<green>[INVESTOR]", "Luminary");
        donor("vip", "<dark_green>[VIP]", "Luminary");
        donor("rich", "<green>[$RICH$]", "Celestial");
        donor("patron", "<light_purple>[PATRON]", "Celestial");
        donor("vip_plus", "<dark_green>[VIP+]", "Celestial");
        donor("mvp", "<aqua>[MVP]", "Celestial");
        donor("mvp_plus", "<aqua>[MVP+]", "Aurora");
        donor("maestro", "<gold>[MAESTRO]", "Aurora");

        for (String[] color : COLORS) {
            String name = color[0];
            String display = "<" + color[1] + ">" + title(name);
            Material dye = Material.valueOf(color[2]);

            if (name.equals("light_gray")) {
                add("chat_" + name, Type.CHAT_COLOR, display, dye, Source.DEFAULT, 0, null, color[1]);
            } else if (name.equals("white")) {
                add("chat_" + name, Type.CHAT_COLOR, display, dye, Source.PLAYTIME, 100 * HOUR, null, color[1]);
            } else {
                add("chat_" + name, Type.CHAT_COLOR, display, dye, Source.ESSENCE, CHAT_COLOR_PRICE,
                        "Free with the AURORA rank", color[1]);
            }
        }
        for (String[] color : COLORS) {
            String name = color[0];
            boolean plain = name.equals("white");
            add("name_" + name, Type.NAME_COLOR, "<" + color[1] + ">" + title(name), Material.valueOf(color[2]),
                    plain ? Source.DEFAULT : Source.ESSENCE, plain ? 0 : NAME_COLOR_PRICE, null, color[1]);
        }

        join("default", "<yellow>Classic", Source.DEFAULT, 0, "<yellow><player> joined the game");
        join("arrived", "<green>Arrival", Source.ESSENCE, 500, "<dark_gray>[<green>+<dark_gray>] <white><player> <gray>has arrived");
        join("wild", "<green>Wild", Source.ESSENCE, 750, "<green>A wild <white><player> <green>appeared!");
        join("skies", "<aqua>From the Skies", Source.ESSENCE, 1_000, "<aqua><player> <gray>descended from the skies");
        join("miner", "<gold>Ore Thief", Source.ESSENCE, 1_000, "<gold><player> <gray>is here to mine. Hide your ore.");
        join("legend", "<gold><b>Legend", Source.ESSENCE, 2_500,
                "<gradient:#FFD700:#FFFFFF><b>The legend <player> has returned</b></gradient>");
        join("aurora", "<gradient:#7F7FD5:#86A8E7:#91EAE4><b>Aurora", Source.ESSENCE, 5_000,
                "<gradient:#7F7FD5:#86A8E7:#91EAE4>The aurora shimmers as <b><player></b> arrives</gradient>");

        // The value is the particle
        trail("flame", "<gold>Flame", Material.BLAZE_POWDER, 1_000, "FLAME");
        trail("hearts", "<red>Hearts", Material.POPPY, 1_000, "HEART");
        trail("notes", "<green>Notes", Material.NOTE_BLOCK, 1_500, "NOTE");
        trail("snow", "<white>Snow", Material.SNOWBALL, 1_500, "SNOWFLAKE");
        trail("cherry", "<light_purple>Cherry Blossom", Material.PINK_PETALS, 2_000, "CHERRY_LEAVES");
        trail("enchant", "<aqua>Runes", Material.ENCHANTING_TABLE, 2_000, "ENCHANT");
        trail("soul", "<dark_aqua>Soul Fire", Material.SOUL_TORCH, 2_500, "SOUL_FIRE_FLAME");
        trail("end", "<white><b>Starlight", Material.END_ROD, 3_000, "END_ROD");

        // The value is the effect, see KillEffects
        kill("flames", "<gold>Burn", Material.FIRE_CHARGE, 1_500);
        kill("hearts", "<red>Heartbreak", Material.POPPY, 1_500);
        kill("soul", "<dark_aqua>Soul Release", Material.SOUL_LANTERN, 2_000);
        kill("firework", "<light_purple>Firework", Material.FIREWORK_ROCKET, 2_500);
        kill("explosion", "<red><b>Explosion", Material.TNT, 3_000);
        kill("lightning", "<yellow><b>Smite", Material.LIGHTNING_ROD, 4_000);

        death("default", "<gray>Plain", Source.DEFAULT, 0,
                "<gray><b><player></b> was slain by <killer>", "<gray><b><player></b> has died");
        death("dramatic", "<red>Dramatic", Source.ESSENCE, 500,
                "<red><b><player></b> met their end at the hands of <killer>", "<red><b><player></b> met their end");
        death("clumsy", "<yellow>Clumsy", Source.ESSENCE, 750,
                "<yellow><b><player></b> forgot how to fight <killer>", "<yellow><b><player></b> tripped over nothing");
        death("ghost", "<aqua>Ghost", Source.ESSENCE, 1_000,
                "<aqua><b><player></b> became a ghost, thanks to <killer>", "<aqua><b><player></b> became a ghost");
        death("revenge", "<dark_red>Vengeful", Source.ESSENCE, 1_500,
                "<dark_red><b><player></b> will be back for <killer>", "<dark_red><b><player></b> will be back");
        death("legend", "<gold><b>Legend", Source.ESSENCE, 2_500,
                "<gradient:#FFD700:#FFFFFF>The legend of <b><player></b> was ended by</gradient> <killer>",
                "<gradient:#FFD700:#FFFFFF>The legend of <b><player></b> has ended</gradient>");
    }

    private Cosmetics() {
    }

    private static void add(String id, Type type, String display, Material icon, Source source,
                            long requirement, String note, String value) {
        ALL.put(id, new Cosmetic(id, type, display, icon, source, requirement, note, value));
    }

    private static void suffix(String name, String display, Source source, long requirement, String note) {
        add("suffix_" + name, Type.SUFFIX, display, Material.NAME_TAG, source, requirement, note, display);
    }

    // "<red>" and "titan" make "<red>[TITAN]"
    private static void bought(String name, String style, long essence) {
        suffix(name, style + "[" + name.toUpperCase() + "]", Source.ESSENCE, essence, null);
    }

    private static void donor(String name, String display, String pack) {
        suffix(name, display, Source.GRANT, 0, "From the " + pack + " donor package");
    }

    private static void join(String name, String display, Source source, long essence, String message) {
        add("join_" + name, Type.JOIN_MESSAGE, display, Material.OAK_SIGN, source, essence, null, message);
    }

    private static void death(String name, String display, Source source, long essence, String killed, String alone) {
        add("death_" + name, Type.DEATH_MESSAGE, display, Material.SKELETON_SKULL, source, essence, null,
                killed + DEATH_SEP + alone);
    }

    private static void trail(String name, String display, Material icon, long essence, String particle) {
        add("trail_" + name, Type.TRAIL, display, icon, Source.ESSENCE, essence, null, particle);
    }

    private static void kill(String name, String display, Material icon, long essence) {
        add("kill_" + name, Type.KILL_EFFECT, display, icon, Source.ESSENCE, essence, null, name);
    }

    // "light_gray" to "Light Gray"
    private static String title(String name) {
        StringBuilder title = new StringBuilder();
        for (String word : name.split("_")) {
            if (!title.isEmpty()) title.append(' ');
            title.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
        }
        return title.toString();
    }

    public static List<Cosmetic> all() {
        return new ArrayList<>(ALL.values());
    }

    public static List<Cosmetic> ofType(Type type) {
        return ALL.values().stream().filter(cosmetic -> cosmetic.type() == type).toList();
    }

    // Null for an id that is not a cosmetic, or not one any more
    public static Cosmetic get(String id) {
        return id == null ? null : ALL.get(id.toLowerCase());
    }

    // What a slot holds for a player who has never touched it, null for nothing
    public static Cosmetic defaultFor(Slot slot) {
        return switch (slot) {
            case SUFFIX_1 -> get("suffix_new");
            case CHAT_COLOR -> get("chat_light_gray");
            case NAME_COLOR -> get("name_white");
            case JOIN_MESSAGE -> get("join_default");
            case DEATH_MESSAGE -> get("death_default");
            default -> null;
        };
    }
}
