package org.charky.aurora.core.models;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GearModel {
    public String display;
    public String material = "BARRIER";
    public int tier = 0;
    public String type;
    public String world;
    public String region;

    public Map<String, Integer> enchants = new HashMap<>();
    public List<String> lore = new ArrayList<>();
}