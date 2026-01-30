package org.charky.aurora.models;

import java.util.ArrayList;
import java.util.List;

public class GearModel {
    public String display;
    public String material = "BARRIER";
    public int tier = 0;
    public String type;
    public String world;
    public String region;

    public List<EnchantEntry> enchants = new ArrayList<>();
    public List<LoreEntry> lore = new ArrayList<>();

    public static class EnchantEntry {
        public String display;
        public int level = 1;
        public String enchant;
    }

    public static class LoreEntry {
        public String display;
    }
}