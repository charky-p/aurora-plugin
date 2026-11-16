package org.charky.aurora.core.models;

import java.util.ArrayList;
import java.util.List;

public class RealmModel {
    public String name;
    public List<String> worlds = new ArrayList<>();
    // The realm whose tool levels this one builds on, null to stand alone
    public String base;
}
