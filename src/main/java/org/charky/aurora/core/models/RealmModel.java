package org.charky.aurora.core.models;

import java.util.ArrayList;
import java.util.List;

public class RealmModel {
    public String name;
    public List<String> worlds = new ArrayList<>();
    // The realm whose tool levels this one builds on, null to stand alone
    public String base;
    // The realm this one is a part of, null for a realm with worlds of its own
    public String part_of;
}
