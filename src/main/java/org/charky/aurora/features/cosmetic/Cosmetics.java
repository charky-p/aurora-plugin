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

    private static final String DONOR_NOTE = "From a donor package";
    // Between the regions of a world cosmetic's target
    public static final String REGION_SEP = "|";

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
    // White chat is earned by time played, every other colour comes with a donor package
    private static final long WHITE_CHAT_HOURS = 10;

    // Armour trims by the pattern's own name. None stands above another, so they
    // cost the same, all but the rarest
    private static final String[] TRIM_PATTERNS = {"sentry", "dune", "coast", "wild", "ward", "eye", "vex", "tide",
            "snout", "rib", "spire", "wayfinder", "shaper", "raiser", "host", "flow", "bolt", "silence"};
    private static final int TRIM_PRICE = 1_500;
    private static final String RARE_TRIM = "silence";
    private static final int RARE_TRIM_PRICE = 5_000;
    // What every trim is drawn on in the menus
    public static final Material TRIM_ICON = Material.IRON_CHESTPLATE;
    // name, colour in the menu, icon. Quartz is what a trim is made of until another is chosen
    private static final String[][] TRIM_MATERIALS = {
            {"quartz", "white", "QUARTZ"}, {"iron", "gray", "IRON_INGOT"}, {"copper", "#E8925A", "COPPER_INGOT"},
            {"gold", "gold", "GOLD_INGOT"}, {"redstone", "red", "REDSTONE"}, {"lapis", "blue", "LAPIS_LAZULI"},
            {"emerald", "green", "EMERALD"}, {"amethyst", "light_purple", "AMETHYST_SHARD"},
            {"diamond", "aqua", "DIAMOND"}, {"netherite", "dark_gray", "NETHERITE_INGOT"},
            {"resin", "#FC7812", "RESIN_BRICK"},
    };
    private static final String DEFAULT_TRIM_MATERIAL = "quartz";
    private static final int TRIM_MATERIAL_PRICE = 750;

    static {
        // Suffixes. Only the ones that cost real money are bold
        suffix("new", "<gray>[<dark_gray>NEW<gray>]", Source.DEFAULT, 0, "Yours from your first join");
        suffix("afk", "<gray>[<dark_gray>AFK<gray>]", Source.DEFAULT, 0, "For when you step away");
        suffix("newcomer", "<gray>[<green>NEWCOMER<gray>]", Source.GRANT, 0, "Finish the tutorial");
        suffix("regular", "<gray>[<dark_gray>REGULAR<gray>]", Source.PLAYTIME, 10 * HOUR, null);
        suffix("devoted", "<gray>[<white>DEVOTED<gray>]", Source.PLAYTIME, 50 * HOUR, null);
        suffix("veteran", "<gray>[<dark_gray>VETERAN<gray>]", Source.PLAYTIME, 100 * HOUR, null);
        suffix("ancient", "<gray>[<gold>ANCIENT<gray>]", Source.PLAYTIME, 500 * HOUR, null);

        // One for every zone, by the id it has in zones.csv, earned by buying every
        // upgrade the zone sells
        zone("cobble", "<gray>", "cobble");
        zone("coal", "<dark_gray>", "coal");
        zone("copper", "<#D94110>", "copper");
        zone("iron", "<white>", "iron");
        zone("gold", "<gold>", "gold");
        zone("city", "<gray>", "city");
        zone("dunes", "<yellow>", "dunes");
        zone("canyons", "<#B4462A>", "canyons");
        zone("temple", "<gold>", "temple");
        zone("tomb", "<dark_purple>", "tomb");
        zone("oak", "<#7BA05B>", "oak");
        zone("birch", "<#D9D3B0>", "birch");
        zone("spruce", "<#4F7A4A>", "spruce");
        zone("dark oak", "<#6B4F35>", "darkoak");
        zone("treehouse", "<#2E8B57>", "treehouse");
        // The regions after the first three, one suffix a zone in the region's colour
        zone("ruins", "<#B4462A>", "ruins");
        zone("catacombs", "<#B4462A>", "catacombs");
        zone("sanctuary", "<#B4462A>", "sanctuary");
        zone("jungle", "<#3A7D2C>", "jungle");
        zone("bamboo", "<#3A7D2C>", "bamboo");
        zone("amber", "<#3A7D2C>", "amber");
        zone("mudflats", "<#4E6B3A>", "mudflats");
        zone("slime", "<#4E6B3A>", "slime_hollow");
        zone("fens", "<#4E6B3A>", "fens");
        zone("sunken ruins", "<#4E6B3A>", "sunken_ruins");
        zone("glowmire", "<#4E6B3A>", "glowmire");
        zone("foothills", "<#2ECC71>", "foothills");
        zone("crags", "<#2ECC71>", "stony_crags");
        zone("slopes", "<#2ECC71>", "frozen_slopes");
        zone("grotto", "<#2ECC71>", "redstone_grotto");
        zone("depths", "<#2ECC71>", "emerald_depths");
        zone("arachne", "<#2ECC71>", "arachne_cave");
        zone("ashen", "<#C9A227>", "ashen");
        zone("judgement", "<#C9A227>", "judgement");
        zone("forge", "<#C9A227>", "forge");
        zone("night", "<#C9A227>", "throne");
        zone("cellar", "<#1E5631>", "root_cellar");
        zone("vines", "<#1E5631>", "vine_halls");
        zone("hive", "<#1E5631>", "hive_chambers");
        zone("canopy", "<#1E5631>", "upper_canopy");
        zone("crown", "<#1E5631>", "the_crown");
        zone("cloudbank", "<#BFE9FF>", "cloudbank");
        zone("spires", "<#BFE9FF>", "skyshard_spires");
        zone("storm", "<#BFE9FF>", "storm_front");
        zone("harbour", "<#BFE9FF>", "sky_harbour");
        zone("walls", "<#FFE680>", "outer_walls");
        zone("gardens", "<#FFE680>", "gilded_gardens");
        zone("treasury", "<#FFE680>", "royal_treasury");
        zone("throne room", "<#FFE680>", "throne_room");
        zone("gravel", "<#8A8D91>", "gravel_pits");
        zone("pale", "<#8A8D91>", "pale_woods");
        zone("maple", "<#8A8D91>", "maple_grove");
        zone("diamond", "<#8A8D91>", "diamond_depths");
        zone("pit", "<#8A8D91>", "quarry_pit");
        zone("golem", "<#8A8D91>", "golems_rest");

        // One for every world, by the regions its zones have in zones.csv, earned by
        // doing that in every zone of every one of them. The Overworld is the Origin
        // Isles and Verdant Groves
        world("overworld", "<gradient:#56AB2F:#A8E063>", "OVERWORLD", String.join(REGION_SEP,
                "origin_isles", "verdant", "marsh", "peaks", "quarry"));
        world("duneworld", "<gradient:#F7971E:#FFD200>", "DUNEWORLD", String.join(REGION_SEP, "gormak", "khorvath", "duat"));
        world("elderwood_roots", "<gradient:#0B8043:#A8FF78>", "ELDERWOOD ROOTS", "elderwood" + REGION_SEP + "elderheart");
        world("skyworld", "<gradient:#BFE9FF:#FFE680>", "SKY WORLD", "skyisles" + REGION_SEP + "kingdom");

        bought("advanced", "<green>", 100);
        bought("elite", "<aqua>", 150);
        bought("hero", "<yellow>", 200);
        bought("miner", "<gray>", 250);
        bought("warrior", "<red>", 300);
        bought("slayer", "<dark_red>", 350);
        bought("champion", "<gold>", 400);
        bought("explorer", "<dark_green>", 450);
        bought("legend", "<light_purple>", 500);
        bought("grinder", "<dark_aqua>", 550);
        bought("titan", "<dark_aqua>", 650);
        bought("tryhard", "<red>", 700);
        bought("colossal", "<dark_green>", 800);
        bought("no life", "<dark_gray>", 900);
        bought("invincible", "<blue>", 1_000);
        bought("mythic", "<gradient:#C471ED:#F64F59>", 1_250);
        bought("phantom", "<gradient:#BDC3C7:#2C3E50>", 1_500);
        bought("void", "<gradient:#8E2DE2:#4A00E0>", 2_000);
        bought("cosmic", "<gradient:#00C6FF:#7F00FF>", 2_500);

        paid("legendary", "<gold><b>");
        paid("immortal", "<dark_red><b>");
        paid("supreme", "<dark_purple><b>");
        paid("ultimate", "<red><b>");
        paid("apex", "<gradient:#FF8008:#E52D27><b>");
        paid("nexus", "<gradient:#56CCF2:#2F80ED><b>");
        paid("radiant", "<gradient:#FFF200:#F7971E><b>");
        paid("paragon", "<gradient:#38EF7D:#11998E><b>");
        paid("divine", "<gradient:#FFFFFF:#FFD700><b>");
        paid("eternal", "<gradient:#B47CFF:#3A1C71><b>");
        paid("ascendant", "<gradient:#7F7FD5:#86A8E7:#91EAE4><b>");

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

        // Light gray chat and a white name are what everyone has
        for (String[] color : COLORS) {
            String name = color[0];
            Material dye = Material.valueOf(color[2]);
            String display = "<" + color[1] + ">" + title(name);

            if (name.equals("light_gray")) {
                add("chat_" + name, Type.CHAT_COLOR, display, dye, Source.DEFAULT, 0, null, color[1], null);
            } else if (name.equals("white")) {
                add("chat_" + name, Type.CHAT_COLOR, display, dye, Source.PLAYTIME, WHITE_CHAT_HOURS * HOUR, null, color[1], null);
            } else {
                add("chat_" + name, Type.CHAT_COLOR, display, dye, Source.GRANT, 0, DONOR_NOTE, color[1], null);
            }

            boolean plain = name.equals("white");
            add("name_" + name, Type.NAME_COLOR, display, dye, plain ? Source.DEFAULT : Source.GRANT, 0,
                    plain ? null : DONOR_NOTE, color[1], null);
        }

        join("default", "<yellow>Classic", Source.DEFAULT, 0, "<yellow><player> joined the game");
        join("back", "<gray>Back Again", Source.ESSENCE, 400, "<gray><player> is back");
        join("arrived", "<green>Arrival", Source.ESSENCE, 500, "<dark_gray>[<green>+<dark_gray>] <white><player> <gray>has arrived");
        join("shift", "<yellow>Another Shift", Source.ESSENCE, 600, "<yellow><player> <gray>clocked in for another shift");
        join("wild", "<green>Wild", Source.ESSENCE, 750, "<green>A wild <white><player> <green>appeared!");
        join("pickaxe", "<aqua>Pickaxe Ready", Source.ESSENCE, 900, "<aqua><player> <gray>picked their pickaxe back up");
        join("skies", "<aqua>From the Skies", Source.ESSENCE, 1_000, "<aqua><player> <gray>descended from the skies");
        join("miner", "<gold>Ore Thief", Source.ESSENCE, 1_000, "<gold><player> <gray>is here to mine. Hide your ore.");
        join("depths", "<dark_aqua>From the Depths", Source.ESSENCE, 1_500, "<dark_aqua><player> <gray>climbed out of the mines");
        join("legend", "<gold><b>Legend", Source.GRANT, 0,
                "<gradient:#FFD700:#FFFFFF><b>The legend <player> has returned</b></gradient>");
        join("royal", "<gold><b>Royal", Source.GRANT, 0, "<gold><b>Make way for <player></b>");
        join("storm", "<aqua><b>Storm", Source.GRANT, 0,
                "<gradient:#4FACFE:#00F2FE>A storm rolls in with <b><player></b></gradient>");
        join("star", "<light_purple><b>Falling Star", Source.GRANT, 0,
                "<gradient:#C471ED:#F64F59><b><player></b> fell from the stars</gradient>");
        join("aurora", "<gradient:#7F7FD5:#86A8E7:#91EAE4><b>Aurora", Source.GRANT, 0,
                "<gradient:#7F7FD5:#86A8E7:#91EAE4>The aurora shimmers as <b><player></b> arrives</gradient>");

        // The value is the particle
        trail("flame", "<gold>Flame", Material.BLAZE_POWDER, Source.ESSENCE, 1_000, "FLAME");
        trail("hearts", "<red>Hearts", Material.POPPY, Source.ESSENCE, 1_000, "HEART");
        trail("clouds", "<white>Clouds", Material.WHITE_WOOL, Source.ESSENCE, 1_000, "CLOUD");
        trail("sparks", "<yellow>Sparks", Material.FLINT_AND_STEEL, Source.ESSENCE, 1_250, "CRIT");
        trail("notes", "<green>Notes", Material.NOTE_BLOCK, Source.ESSENCE, 1_500, "NOTE");
        trail("snow", "<white>Snow", Material.SNOWBALL, Source.ESSENCE, 1_500, "SNOWFLAKE");
        trail("emerald", "<green>Emerald Dust", Material.EMERALD, Source.ESSENCE, 1_750, "HAPPY_VILLAGER");
        trail("cherry", "<light_purple>Cherry Blossom", Material.PINK_PETALS, Source.ESSENCE, 2_000, "CHERRY_LEAVES");
        trail("enchant", "<aqua>Runes", Material.ENCHANTING_TABLE, Source.ESSENCE, 2_000, "ENCHANT");
        trail("wax", "<gold>Honey Glow", Material.HONEYCOMB, Source.ESSENCE, 2_500, "WAX_ON");
        trail("soul", "<dark_aqua><b>Soul Fire", Material.SOUL_TORCH, Source.GRANT, 0, "SOUL_FIRE_FLAME");
        trail("electric", "<yellow><b>Lightning", Material.LIGHTNING_ROD, Source.GRANT, 0, "ELECTRIC_SPARK");
        trail("glow", "<aqua><b>Glow", Material.GLOW_INK_SAC, Source.GRANT, 0, "GLOW");
        trail("totem", "<green><b>Totem", Material.TOTEM_OF_UNDYING, Source.GRANT, 0, "TOTEM_OF_UNDYING");
        trail("end", "<white><b>Starlight", Material.END_ROD, Source.GRANT, 0, "END_ROD");

        // The value is the effect, see KillEffects
        kill("flames", "<gold>Burn", Material.FIRE_CHARGE, 1_500);
        kill("hearts", "<red>Heartbreak", Material.POPPY, 1_500);
        kill("soul", "<dark_aqua>Soul Release", Material.SOUL_LANTERN, 2_000);
        kill("firework", "<light_purple>Firework", Material.FIREWORK_ROCKET, 2_500);
        kill("explosion", "<red><b>Explosion", Material.TNT, 3_000);
        kill("lightning", "<yellow><b>Smite", Material.LIGHTNING_ROD, 4_000);

        death("default", "<gray>Plain", Source.DEFAULT, 0,
                "<gray><b><player></b> was slain by <killer>", "<gray><b><player></b> has died");
        death("fallen", "<red>Fallen", Source.ESSENCE, 500,
                "<red><b><player></b> fell to <killer>", "<red><b><player></b> has fallen");
        death("bested", "<yellow>Bested", Source.ESSENCE, 750,
                "<yellow><b><player></b> was bested by <killer>", "<yellow><b><player></b> ran out of luck");
        death("rest", "<aqua>Laid to Rest", Source.ESSENCE, 1_000,
                "<aqua><b><player></b> was laid to rest by <killer>", "<aqua><b><player></b> was laid to rest");
        death("skull", "<white>Skull", Source.ESSENCE, 1_500,
                "<dark_gray>☠ <white><b><player></b> <dark_gray>« <killer>", "<dark_gray>☠ <white><b><player></b>");
        death("reign", "<gold><b>End of a Reign", Source.GRANT, 0,
                "<gold>The reign of <b><player></b> was ended by <killer>", "<gold>The reign of <b><player></b> is over");
        death("aurora", "<gradient:#7F7FD5:#86A8E7:#91EAE4><b>Aurora", Source.GRANT, 0,
                "<gradient:#7F7FD5:#91EAE4><b><player></b> faded into the aurora, sent by</gradient> <killer>",
                "<gradient:#7F7FD5:#91EAE4><b><player></b> faded into the aurora</gradient>");

        // The value is the pattern or material as the game names it
        add("trim_none", Type.TRIM_PATTERN, "<gray>No Trim", TRIM_ICON, Source.DEFAULT, 0, null, NONE, null);
        for (String name : TRIM_PATTERNS) {
            add("trim_" + name, Type.TRIM_PATTERN, "<yellow>" + title(name), TRIM_ICON, Source.ESSENCE,
                    name.equals(RARE_TRIM) ? RARE_TRIM_PRICE : TRIM_PRICE, null, name, null);
        }
        for (String[] material : TRIM_MATERIALS) {
            boolean plain = material[0].equals(DEFAULT_TRIM_MATERIAL);
            add("trimcolor_" + material[0], Type.TRIM_MATERIAL, "<" + material[1] + ">" + title(material[0]),
                    Material.valueOf(material[2]), plain ? Source.DEFAULT : Source.ESSENCE,
                    plain ? 0 : TRIM_MATERIAL_PRICE, null, material[0], null);
        }
    }

    private Cosmetics() {
    }

    private static void add(String id, Type type, String display, Material icon, Source source,
                            long requirement, String note, String value, String target) {
        ALL.put(id, new Cosmetic(id, type, display, icon, source, requirement, note, value, target));
    }

    private static void suffix(String name, String display, Source source, long requirement, String note) {
        add("suffix_" + name.replace(' ', '_'), Type.SUFFIX, display, Material.NAME_TAG, source, requirement, note, display, null);
    }

    // "<red>" and "titan" make "<red>[TITAN]"
    private static void bought(String name, String style, long essence) {
        suffix(name, style + "[" + name.toUpperCase() + "]", Source.ESSENCE, essence, null);
    }

    private static void paid(String name, String style) {
        suffix(name, style + "[" + name.toUpperCase() + "]", Source.GRANT, 0, DONOR_NOTE);
    }

    private static void donor(String name, String display, String pack) {
        suffix(name, display, Source.GRANT, 0, "From the " + pack + " donor package");
    }

    private static void zone(String name, String style, String zoneId) {
        String display = style + "[" + name.toUpperCase() + "]";
        add("suffix_" + name.replace(' ', '_'), Type.SUFFIX, display, Material.NAME_TAG, Source.ZONE, 0, null, display, zoneId);
    }

    private static void world(String name, String style, String shown, String region) {
        String display = style + "[" + shown + "]";
        add("suffix_" + name, Type.SUFFIX, display, Material.NAME_TAG, Source.WORLD, 0, null, display, region);
    }

    private static void join(String name, String display, Source source, long essence, String message) {
        add("join_" + name, Type.JOIN_MESSAGE, display, Material.OAK_SIGN, source, essence,
                source == Source.GRANT ? DONOR_NOTE : null, message, null);
    }

    private static void death(String name, String display, Source source, long essence, String killed, String alone) {
        add("death_" + name, Type.DEATH_MESSAGE, display, Material.SKELETON_SKULL, source, essence,
                source == Source.GRANT ? DONOR_NOTE : null, killed + DEATH_SEP + alone, null);
    }

    private static void trail(String name, String display, Material icon, Source source, long essence, String particle) {
        add("trail_" + name, Type.TRAIL, display, icon, source, essence,
                source == Source.GRANT ? DONOR_NOTE : null, particle, null);
    }

    private static void kill(String name, String display, Material icon, long essence) {
        add("kill_" + name, Type.KILL_EFFECT, display, icon, Source.ESSENCE, essence, null, name, null);
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

    // Not out yet: hidden from /customise until taken off this list. Someone who has one
    // still sees it, admins see them all
    private static final java.util.Set<String> UNRELEASED = java.util.Set.of(
            "suffix_ancient",
            "suffix_elite", "suffix_slayer", "suffix_explorer", "suffix_grinder", "suffix_titan", "suffix_tryhard",
            "suffix_colossal", "suffix_no_life", "suffix_invincible", "suffix_mythic", "suffix_phantom",
            "suffix_void", "suffix_cosmic",
            "suffix_immortal", "suffix_ultimate", "suffix_nexus", "suffix_paragon", "suffix_divine", "suffix_eternal");

    public static boolean isReleased(Cosmetic cosmetic) {
        return !UNRELEASED.contains(cosmetic.id());
    }

    public static List<Cosmetic> all() {
        return new ArrayList<>(ALL.values());
    }

    public static List<Cosmetic> ofType(Type type) {
        return ALL.values().stream().filter(cosmetic -> cosmetic.type() == type).toList();
    }

    // What maxing out a zone, or the last zone of a world in this region, earns
    public static List<Cosmetic> earnedIn(Source source, String target) {
        return ALL.values().stream()
                .filter(cosmetic -> cosmetic.source() == source && regions(cosmetic).contains(target)).toList();
    }

    // The zone of a zone cosmetic, the regions of a world one
    public static List<String> regions(Cosmetic cosmetic) {
        return cosmetic.target() == null ? List.of() : List.of(cosmetic.target().split(java.util.regex.Pattern.quote(REGION_SEP)));
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
            case TRIM_PATTERN -> get("trim_none");
            case TRIM_MATERIAL -> get("trimcolor_" + DEFAULT_TRIM_MATERIAL);
            default -> null;
        };
    }
}
