package org.charky.aurora.core.models;

import java.util.ArrayList;
import java.util.List;

public class MobModel {
    public String display = "Unknown Mob";
    public String entity = "ZOMBIE";
    public double health = 20.0;
    public double defence = 0.0;
    public double attack = 1.0;
    public EquipmentModel equip = new EquipmentModel();
    public List<AttributeEntry> attributes = new ArrayList<>();
    public List<BlockModel.IndependentPoolModel> independent = new ArrayList<>();
    public BlockModel.CumulativeLootPoolModel choices;

    public static class EquipmentModel {
        public String head;
        public String chest;
        public String legs;
        public String feet;
        public String hand;
        public String off_hand;
    }

    public static class AttributeEntry {
        public String attr;
        public double value;
    }
}