package org.charky.aurora.models;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GuiModel {
    public String id;
    public String title;
    public String icon;
    public int rows = 3;
    public List<String> layout = new ArrayList<>();
    public Map<Character, ComponentModel> components = new HashMap<>();

    public static class ComponentModel {
        public String type;
        public String id;
        public int amount = 1;
        public List<CostEntry> cost = new ArrayList<>();
    }

    public static class CostEntry {
        public String id;
        public int amount = 1;
    }
}
