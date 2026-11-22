package org.charky.aurora.core.models;

import java.util.ArrayList;
import java.util.List;

// One fishing spot from fishing.yml: its fish and the rod upgrades sold there
public class FishingSpotModel {
    public String name;
    public String realm;
    public int order;
    public List<FishModel> fish = new ArrayList<>();
    public List<RodStepModel> rod = new ArrayList<>();

    public static class FishModel {
        public String id;
        public String tier;
        public int score;
        public int min = 1;
        public int max = 1;
        public int density;
        public double essence;
    }

    public static class RodStepModel {
        public String stat;
        public int level;
        public List<CostModel> costs = new ArrayList<>();
    }

    public static class CostModel {
        public String id;
        public int amount = 1;
    }
}
