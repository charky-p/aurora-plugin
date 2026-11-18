package org.charky.aurora.core.models;

import java.util.ArrayList;
import java.util.List;

// One zone of upgrades.yml, generated from upgrades.csv with the prices worked out
public class UpgradeModel {
    public String name;
    public String realm;
    public String region;
    // Marked released in regions.csv, players are not shown the rest
    public boolean released;
    public List<StepModel> steps = new ArrayList<>();

    public static class StepModel {
        public String tool;
        public String stat;
        // A number, or the name of a material
        public String level;
        public List<GuiModel.CostEntry> cost = new ArrayList<>();
    }
}
