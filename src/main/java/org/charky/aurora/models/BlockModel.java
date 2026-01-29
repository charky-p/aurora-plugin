package org.charky.aurora.models;

import org.bukkit.Material;

import java.util.ArrayList;
import java.util.List;

public class BlockModel {
    public String id;
    public String material;
    public List<IndependentPoolModel> independent = new ArrayList<>();
    public CumulativeLootPoolModel choices;

    public static class IndependentPoolModel {
        public String id;
        public double chance = 100.0;
        public int min = 1;
        public int max = 1;
    }

    public static class CumulativeLootPoolModel {
        public double chance = 100.0;
        public List<ChoiceModel> options = new ArrayList<>();
    }

    public static class ChoiceModel {
        public String id;
        public double weight;
        public int min = 1;
        public int max = 1;
    }
}