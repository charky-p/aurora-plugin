package org.charky.aurora.core.models;

import org.bukkit.Material;

import java.util.ArrayList;
import java.util.List;

public class ResourceModel {
    public String display;
    public String material;
    public List<String> lore = new ArrayList<>();
    public String world;
    public String region;
    // Tag given when the item is right clicked and used up, null for none
    public String unlocks;
}
