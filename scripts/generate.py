#!/usr/bin/env python3
"""
Aurora data generator. Reads CSV files from data/ and writes YAML resources to /resources.

CSV cell syntax for nested data
-------------------------------
  lore            Wither Bones|<gray>Dropped by the tomb guard
  enchants        efficiency:3|fortune:2
  equip           head:iron_helmet|hand:bow
  attributes      MAX_HEALTH:35|ATTACK_DAMAGE:8
  drops           coal:100:1-3|diamond:5        (id : chance : min-max)
  options         coal:50:1-2|iron:30           (id : weight : min-max)
  costs           cobblestone*64+coal*32        (id * amount, + separated)

Everything after the first part is optional and falls back to the default,
so `coal` alone means chance 100, min 1, max 1.
"""

import csv
import re
import os
import sys
from collections import OrderedDict
import yaml
import argparse

parser = argparse.ArgumentParser()
parser.add_argument("--live", action="store_true",
                    help="write to the running server's data folder")
parser.add_argument("--check", action="store_true")
args = parser.parse_args()

CUR = os.path.dirname(os.path.abspath(__file__))
DATA_DIR = os.path.join(CUR, "data")


# The CSVs are sorted into folders under data/: world, items, mobs, shops. A file is
# found by its name alone, whichever folder it is in
def data_path(name):
    import glob
    found = glob.glob(os.path.join(DATA_DIR, "**", name), recursive=True)
    return found[0] if found else os.path.join(DATA_DIR, name)

RESOURCE_PATH = "LocalServer/plugins/Aurora" if args.live else "src/main/resources"
RESOURCE_DIR = os.path.join(CUR, "..", RESOURCE_PATH)

LIST_SEP = "|"
PART_SEP = ":"
RANGE_SEP = "-"
COST_SEP = "+"
AMOUNT_SEP = "*"

# --------------------------------------------------------------------------
# Field types - each field parses a CSV column into a Python value
# --------------------------------------------------------------------------

# Omit is a dummy object if the key is not found
OMIT = object()

# Ensure data is double-quoted
class Quoted(str):
    """A string that should always be emitted double-quoted."""

def _quoted_representer(dumper, data):
    return dumper.represent_scalar("tag:yaml.org,2002:str", str(data), style='"')

yaml.add_representer(Quoted, _quoted_representer)

# Field data
class Field:
    def __init__(self, path=None, default=None, required=False, always=False):
        self.path = path
        self.default = default
        self.required = required
        self.always = always

    # Bind field to the column
    def bind(self, column):
        if self.path is None:
            self.path = column
        self.column = column
        return self

    def parse(self, raw):
        raise NotImplementedError

    # Read the entry/cell, OMIT means empty
    def read(self, raw, ctx):
        raw = (raw or "").strip()
        if not raw:
            if self.required:
                raise ValueError(f"{ctx}: column '{self.column}' is required")
            if self.default is None or not self.always:
                return OMIT
            return self.default
        value = self.parse(raw)
        if not self.always and value == self.default:
            return OMIT
        return value

class Str(Field):
    def __init__(self, quote=False, **kw):
        super().__init__(**kw)
        self.quote = quote

    def parse(self, raw):
        return Quoted(raw) if self.quote else raw

class Int(Field):
    def parse(self, raw):
        return int(raw)

class Float(Field):
    def parse(self, raw):
        return float(raw)

class Bool(Field):
    def parse(self, raw):
        return raw.lower() in ("1", "true", "yes", "y")

class StrList(Field):
    # Lore -> ["line one", "line two"]
    def parse(self, raw):
        return [Quoted(p.strip()) for p in raw.split(LIST_SEP) if p.strip()]

class Layout(Field):
    # Grid rows, verbatim so spaces are there (empty slots)
    def parse(self, raw):
        return [Quoted(p) for p in raw.split(LIST_SEP) if p]

class PairMap(Field):
    # Enchants -> {"efficiency": 3, "fortune": 2}
    def __init__(self, value=int, **kw):
        super().__init__(**kw)
        self.value = value

    def parse(self, raw):
        out = OrderedDict()
        for part in raw.split(LIST_SEP):
            part = part.strip()
            if not part:
                continue
            key, _, val = part.partition(PART_SEP)
            out[key.strip()] = self.value(val.strip()) if val.strip() else self.value(1)
        return out

class PairList(Field):
    # Attributes -> [{"attr": "MAX_HEALTH", "value": 35.0}]
    def __init__(self, key_name, value_name, value=float, **kw):
        super().__init__(**kw)
        self.key_name = key_name
        self.value_name = value_name
        self.value = value

    def parse(self, raw):
        out = []
        for part in raw.split(LIST_SEP):
            part = part.strip()
            if not part:
                continue
            key, _, val = part.partition(PART_SEP)
            out.append(OrderedDict([
                (self.key_name, key.strip()),
                (self.value_name, self.value(val.strip()) if val.strip() else self.value(1)),
            ]))
        return out

class Drops(Field):
    # id : weight : min-max
    # weight_name is chance for independent pools, and weight for choices
    def __init__(self, weight_name="chance", weight_default=100.0, **kw):
        super().__init__(**kw)
        self.weight_name = weight_name
        self.weight_default = weight_default

    def parse(self, raw):
        out = []
        for part in raw.split(LIST_SEP):
            part = part.strip()
            if not part:
                continue
            bits = part.split(PART_SEP)
            entry = OrderedDict(id=bits[0].strip())

            weight = self.weight_default
            if len(bits) > 1 and bits[1].strip():
                weight = float(bits[1])
            if weight != self.weight_default:
                entry[self.weight_name] = _trim(weight)

            if len(bits) > 2 and bits[2].strip():
                lo, _, hi = bits[2].partition(RANGE_SEP)
                lo = int(lo)
                hi = int(hi) if hi.strip() else lo
                if lo != 1:
                    entry["min"] = lo
                if hi != 1:
                    entry["max"] = hi
            out.append(entry)
        return out

class Costs(Field):
    # Cobblestone*64+coal*32 -> [{"id": ..., "amount": ...}]
    def parse(self, raw):
        out = []
        for part in raw.split(COST_SEP):
            part = part.strip()
            if not part:
                continue
            item, _, amount = part.partition(AMOUNT_SEP)
            entry = OrderedDict(id=item.strip())
            if amount.strip() and int(amount) != 1:
                entry["amount"] = int(amount)
            out.append(entry)
        return out

def _trim(value):
    # 100.0 -> 100 so YAML is neater
    return int(value) if float(value).is_integer() else value

# --------------------------------------------------------------------------
# Schemas
# --------------------------------------------------------------------------

RESOURCE_HEADER = """\
# ===========================================================
# AURORA RESOURCE TEMPLATE - generated from data/resources.csv
# ===========================================================
# [resource_id]:
#   display: "<color><b><i>Cobblestone"
#   material: [BUKKIT_MATERIAL]
#   lore: (Optional)
#     - "<color><b><i>Lore Line"
#   world: "overworld" (Options: "overworld")
#   region: "origin_isles" (Defaults to "")
#   collection: 250 (How many the first collection tier needs, the rest are multiples)
# ===========================================================
# DO NOT EDIT BY HAND -- run `python generate.py`
# ==========================================================="""

SPECIAL_HEADER = """\
# ===========================================================
# AURORA SPECIAL RESOURCE TEMPLATE - generated from data/resources_special.csv
# ===========================================================
# Resources that are used up by right clicking them, in the backpack or the hand
# [resource_id]:
#   display, material, lore, world, region: as in resources.yml
#   unlocks: gormak (Optional, the tag it gives, portals locked to that tag let the player through)
#   summons: gormak_eternal (Optional, the mob it wakes at that mob's spawner, which never spawns it by itself)
#   Each needs unlocks or summons. Special resources are never boosted by Looting
# ===========================================================
# DO NOT EDIT BY HAND -- run `python generate.py`
# ==========================================================="""

BLOCK_HEADER = """\
# ===========================================================
# AURORA BLOCK TEMPLATE - generated from data/blocks.csv
# ===========================================================
# [block_id]:
#   material: [BUKKIT_MATERIAL]
#   density: 0 (Default, subtracts from fortune)
#   independent: (Optional - Every drop roll is independent)
#     - id: [resource_id]
#       chance: 100 (Default)
#       min: 1 (Default)
#       max: 1 (Default)
#
#   choices: (Optional - Picks one drop)
#     chance: 100 (Default)
#     options:
#       - id: [resource_id]
#         weight: 50
#         min: 1 (Default)
#         max: 1 (Default)
# ===========================================================
# DO NOT EDIT BY HAND -- run `python generate.py`
# ==========================================================="""

GEAR_HEADER = """\
# ===========================================================
# AURORA GEAR TEMPLATE - generated from CSV
# ===========================================================
# [gear_id]:
#   display: "<color><b><i>Example Sword"
#   material: [BUKKIT_MATERIAL]
#   tier: 0 (Default, tier 0 will not display tier)
#   type: "armor" (Options: armor, combat, mining)
#   world: "overworld" (Options: "overworld")
#   region: "origin_isles"
#   enchants: (Optional)
#     [enchant_id]: 1 (Default, e.g. sharpness: 5)
#   lore: (Optional)
#     - "<gray>Example lore about gear"
# ===========================================================
# DO NOT EDIT BY HAND -- run `python generate.py`
# ==========================================================="""

MOB_HEADER = """\
# ===========================================================
# AURORA MOB TEMPLATE - generated from CSV
# ===========================================================
# [mob_id]:
#   display: "<color><b><i>Display Name"
#   entity: [BUKKIT_ENTITY_TYPE]
#   equip: (Optional)
#     head: [item_id] (Optional, item_id = gear_id or resource_id, or a vanilla MATERIAL in capitals)
#     chest: [item_id] (Optional)
#     legs: [item_id] (Optional)
#     feet: [item_id] (Optional)
#     hand: [item_id] (Optional)
#     off_hand: [item_id] (Optional)
#   attributes: (Optional)
#     - attr: [BUKKIT_ATTRIBUTE]
#       value: 1
#   glowing: true (Optional, an outline seen through walls)
#   independent: (Optional - Every drop roll is independent)
#     - id: [item_id]
#       chance: 100 (Default)
#       min: 1 (Default)
#       max: 1 (Default)
#   choices: (Optional - Picks one drop)
#     chance: 100 (Default)
#     options:
#       - id: [item_id]
#         weight: 50
# ===========================================================
# DO NOT EDIT BY HAND -- run `python generate.py`
# ==========================================================="""

ICON_HEADER = """\
# ===========================================================
# AURORA ICON TEMPLATE - generated from data/icons.csv
# ===========================================================
# [icon_id]:
#   display: "<color><b><i>Display Name"
#   material: [BUKKIT_MATERIAL]
#   lore: (Optional)
#     - "<color><b><i>Lore Line"
#   glow: true (Enchanted)
# ===========================================================
# DO NOT EDIT BY HAND -- run `python generate.py`
# ==========================================================="""

GUI_HEADER = """\
# ===========================================================
# AURORA GUI TEMPLATE - generated from CSV
# ===========================================================
# [gui_id]:
#   title: "<color><b><i>Gui Title"
#   icon: [icon_id] (Default null)
#   rows: 3 (Default)
#   layout: (Must be same amount as rows)
#     - "BBBBIBBBX" (B: Border, I: Info, X: Close)
#     - "B 1 a b B" (Distinct characters map to Guis or trades, e.g. 0-9, a-z)
#     - "BBBBBBBBB"
#   components:
#     '1':
#       type: GUI
#       id: [gui_id]
#     'a':
#       type: TRADE
#       id: [item_id]
#       amount: 1 (Default)
#       cost: (Optional)
#         - id: [item_id]
#           amount: 1 (Default)
#     'b': ...
# ===========================================================
# DO NOT EDIT BY HAND -- run `python generate.py`
# ==========================================================="""

TRADER_HEADER = """\
# ===========================================================
# AURORA TRADER TEMPLATE - generated from data/traders.csv
# ===========================================================
# [trader_id]:
#   display: "<color><b><i>Display Name"
#   entity: [BUKKIT_ENTITY_TYPE]
#   gui: [gui_id], or upgrades:[zone_id] to open that zone's upgrades directly
# ===========================================================
# DO NOT EDIT BY HAND -- run `python generate.py`
# ==========================================================="""

ENCHANT_HEADER = """\
# ===========================================================
# AURORA ENCHANT TEMPLATE - generated from data/enchants.csv
# ===========================================================
# [enchant_id]:
#   display: "<color><b><i>Example Enchant"
#   enchant: [BUKKIT_ENCHANTMENT] (Optional, null implies custom enchant)
# ===========================================================
# DO NOT EDIT BY HAND -- run `python generate.py`
# ==========================================================="""
def gear_schema(csv_name, out_name, gear_type):
    return {
        "csv": csv_name,
        "out": out_name,
        "header": GEAR_HEADER,
        "fields": OrderedDict([
            ("display",  Str(quote=True, required=True)),
            ("material", Str(required=True)),
            ("tier",     Int(default=0)),
            ("type",     Str(quote=True, default=gear_type, always=True)),
            ("world",    Str(quote=True, default="overworld", always=True)),
            ("region",   Str(quote=True, default="", always=True)),
            ("enchants", PairMap(value=int)),
            ("lore",     StrList()),
        ]),
    }

def loot_fields():
    # Independent and choices, used by blocks and mob for loot drops
    return [
        ("independent", Drops(weight_name="chance", weight_default=100.0)),
        ("choices_chance", Float(path="choices.chance", default=100.0)),
        ("choices", Drops(path="choices.options", weight_name="weight", weight_default=50.0)),
    ]

SCHEMAS = OrderedDict()

SCHEMAS["resources"] = {
    "csv": "resources.csv",
    "out": "resources.yml",
    "header": RESOURCE_HEADER,
    "fields": OrderedDict([
        ("display",  Str(quote=True, required=True)),
        ("material", Str(required=True)),
        ("world",    Str(quote=True, default="overworld", always=True)),
        ("region",   Str(quote=True, default="", always=True)),
        ("lore",     StrList()),
    ]),
}

# Resources that do something when right clicked, in the backpack or the hand
SCHEMAS["resources_special"] = {
    "csv": "resources_special.csv",
    "out": "resources_special.yml",
    "header": SPECIAL_HEADER,
    "fields": OrderedDict(list(SCHEMAS["resources"]["fields"].items()) + [
        ("unlocks",  Str()),
        ("summons",  Str()),
    ]),
}

# After the special schema copied the fields, keys are not collected
SCHEMAS["resources"]["fields"]["collection"] = Int()
# What the Essence Trader pays for one, from scripts/balance/essence_values.py
SCHEMAS["resources"]["fields"]["essence"] = Float(default=0.0)

SCHEMAS["blocks"] = {
    "csv": "blocks.csv",
    "out": "blocks.yml",
    "header": BLOCK_HEADER,
    "fields": OrderedDict([
        ("material", Str(required=True)),
        ("density",  Int(default=0)),
        ("xp",       Int(default=1)),
    ] + loot_fields()),
}

SCHEMAS["armor"] = gear_schema("gear_armor.csv", "gear/armor.yml", "armor")
SCHEMAS["weapons"] = gear_schema("gear_weapons.csv", "gear/weapons.yml", "combat")
SCHEMAS["other"] = gear_schema("gear_other.csv", "gear/other.yml", "")

# One file per region, every one with the same columns. A column a file leaves out takes its default
MOB_REGIONS = ["origin", "gormak", "verdant", "khorvath", "elderwood", "marsh", "peaks", "duat", "sky", "quarry", "gauntlet"]


def mob_schema(region):
    return {
        "csv": f"mobs_{region}.csv",
        "out": f"mobs/{region}.yml",
        "header": MOB_HEADER,
        "fields": OrderedDict([
            ("display",    Str(quote=True, required=True)),
            ("entity",     Str(required=True)),
            ("health",     Float(default=20.0, always=True)),
            ("defence",    Float(default=0.0)),
            ("attack",     Float(default=0.0)),
            ("attack_cooldown", Float(default=0.0)),
            ("damage",     Float(default=1.0)),
            ("density",    Int(default=0)),
            ("equip",      PairMap(value=str)),
            ("attributes", PairList("attr", "value", value=float)),
            ("glowing",    Bool(default=False)),
        ] + loot_fields()),
    }


for _region in MOB_REGIONS:
    SCHEMAS[f"mobs_{_region}"] = mob_schema(_region)

SCHEMAS["icons"] = {
    "csv": "icons.csv",
    "out": "icons.yml",
    "header": ICON_HEADER,
    "fields": OrderedDict([
        ("display",  Str(quote=True, required=True)),
        ("material", Str(required=True)),
        ("glow",     Bool(default=False, always=True)),
        ("lore",     StrList()),
    ]),
}

SCHEMAS["traders"] = {
    "csv": "traders.csv",
    "out": "traders.yml",
    "header": TRADER_HEADER,
    "fields": OrderedDict([
        ("display", Str(quote=True, required=True)),
        ("entity",  Str(required=True)),
        ("gui",     Str(required=True)),
        ("profession", Str()),
        ("biome",   Str()),
    ]),
}

WARP_HEADER = """\
# ===========================================================
# AURORA WARP TEMPLATE - generated from data/warps.csv
# ===========================================================
# [warp_id]:
#   icon: [icon_id] (Its name and look in the Warps menu)
#   world: [world_name] (Leads to that world's /aurora setspawn)
#   tag: gormak (Optional, locked until the player has used the key giving this tag)
# ===========================================================
"""

SCHEMAS["warps"] = {
    "csv": "warps.csv",
    "out": "warps.yml",
    "header": WARP_HEADER,
    "fields": OrderedDict([
        ("icon",   Str(required=True)),
        ("world",  Str(required=True)),
        ("region", Str()),
        ("tag",    Str()),
    ]),
}

REALM_HEADER = """\
# ===========================================================
# AURORA REALM TEMPLATE - generated from data/realms.csv
# ===========================================================
# A realm is a group of worlds sharing one set of tool levels.
# [realm_id]:
#   name: "<color><b>Realm Name"
#   worlds: (A world in no realm belongs to the first realm)
#     - world_name
#   base: overworld (Optional, tool levels here add on top of that realm's)
#   part_of: overworld (Optional, for a realm with no worlds: its levels count wherever that realm's do)
# ===========================================================
"""

SCHEMAS["realms"] = {
    "csv": "realms.csv",
    "out": "realms.yml",
    "header": REALM_HEADER,
    "fields": OrderedDict([
        ("name",   Str(quote=True, required=True)),
        ("worlds", StrList()),
        ("base",   Str()),
        ("part_of", Str()),
    ]),
}

TOOL_HEADER = """\
# ===========================================================
# AURORA TOOL TEMPLATE - generated from data/tools.csv
# ===========================================================
# A tool lives on the player as levels, the item is drawn from them.
# [tool_id]:
#   name: Pickaxe
#   kind: mining (Options: mining, combat, armor)
#   item: PICKAXE (The vanilla name after the material, IRON_PICKAXE)
#   materials: (In the order they are unlocked)
#     - WOODEN
# ===========================================================
"""

SCHEMAS["tools"] = {
    "csv": "tools.csv",
    "out": "tools.yml",
    "header": TOOL_HEADER,
    "fields": OrderedDict([
        ("name",      Str(required=True)),
        ("kind",      Str(required=True)),
        ("item",      Str(required=True)),
        ("materials", StrList()),
    ]),
}

SCHEMAS["enchants"] = {
    "csv": "enchants.csv",
    "out": "enchants.yml",
    "header": ENCHANT_HEADER,
    "fields": OrderedDict([
        ("display", Str(quote=True, required=True)),
        ("enchant", Str()),
    ]),
}

# GUIs are a menu with a grid, and the grid refers to components.
# We have a separate csv file for the components, rather than the legacy version.
# Child CSV to parent ID relationship.
def gui_schema(csv_name, child_name, out_name):
    return {
        "csv": csv_name,
        "out": out_name,
        "header": GUI_HEADER,
        "fields": OrderedDict([
            ("title",  Str(quote=True, required=True)),
            ("icon",   Str()),
            ("rows",   Int(default=3, always=True)),
            ("layout", Layout(required=True)),
        ]),
        "child": {
            "csv": child_name,
            "parent_column": "gui",
            "key_column": "key",
            "path": "components",
            "fields": OrderedDict([
                ("type",   Str(required=True)),
                ("id",     Str(required=True)),
                ("icon",   Str()),
                ("amount", Int(default=1)),
                ("cost",   Costs()),
            ]),
        },
    }

SCHEMAS["gui_shops"] = gui_schema("guis_shops.csv", "guis_shops_components.csv", "guis/shops.yml")

# --------------------------------------------------------------------------
# Building
# --------------------------------------------------------------------------

def set_path(target, dotted, value):
    parts = dotted.split(".")
    for part in parts[:-1]:
        target = target.setdefault(part, OrderedDict())
    target[parts[-1]] = value

def read_rows(path):
    if not os.path.exists(path):
        return None
    with open(path, newline="", encoding="utf-8") as f:
        rows = []
        for row in csv.DictReader(f):
            # Skip blank lines and #-comment rows so the CSV can be annotated.
            first = (row.get("id") or "").strip()
            if not first or first.startswith("#"):
                continue
            rows.append(row)
        return rows

def build_entry(row, fields, ctx):
    entry = OrderedDict()
    for column, field in fields.items():
        value = field.read(row.get(column), ctx)
        if value is OMIT:
            continue
        set_path(entry, field.path, value)
    return entry

def build(schema):
    # Returns an OrderedDict of id -> entry in CSV row order.
    rows = read_rows(data_path(schema["csv"]))
    if rows is None:
        return None

    for column, field in schema["fields"].items():
        field.bind(column)

    items = OrderedDict()
    for row in rows:
        item_id = row["id"].strip()
        if item_id in items:
            raise ValueError(f"{schema['csv']}: duplicate id '{item_id}'")
        items[item_id] = build_entry(row, schema["fields"], f"{schema['csv']}:{item_id}")

    child = schema.get("child")
    if child:
        for column, field in child["fields"].items():
            field.bind(column)
        child_rows = read_rows(data_path(child["csv"])) or []
        for row in child_rows:
            parent = row[child["parent_column"]].strip()
            if parent not in items:
                raise ValueError(
                    f"{child['csv']}: '{parent}' is not a row in {schema['csv']}")
            key = row[child["key_column"]].strip()
            ctx = f"{child['csv']}:{parent}/{key}"
            bucket = items[parent].setdefault(child["path"], OrderedDict())
            bucket[key] = build_entry(row, child["fields"], ctx)

    return items

def dump(schema, items):
    # Generate the yaml data
    body = yaml.dump(
        {"items": items},
        Dumper=_Dumper,
        sort_keys=False,
        default_flow_style=False,
        allow_unicode=True,
        width=10 ** 6,
        indent=2,
    )

    out, first = [], True
    for line in body.splitlines():
        if line.startswith("  ") and not line.startswith("   ") and line.rstrip().endswith(":"):
            if not first:
                out.append("")
            first = False
        out.append(line)
    return schema["header"] + "\n\n" + "\n".join(out) + "\n"

class _Dumper(yaml.Dumper):
    def increase_indent(self, flow=False, indentless=False):
        return super().increase_indent(flow, False)

def represent_ordered(dumper, data):
    return dumper.represent_mapping("tag:yaml.org,2002:map", data.items())

_Dumper.add_representer(OrderedDict, represent_ordered)
_Dumper.add_representer(Quoted, _quoted_representer)


# ------------------------------------------------------------------ cross checks
# Each CSV is checked on its own while it builds. These check that the files agree
# with each other, the way the plugin reads them: every id points at something real,
# every cost can be earned, every menu cell has a component. Errors fail the run,
# warnings are data that is never used.

GUI_SPECIAL = set("BIX ")
UPGRADES_ACTION = "upgrades:"
FISHING_ACTION = "fishing:"
ACTIONS = {"backpack", "armory", "spawn", "collections", "ranks", "shop", "warps", "customise", "tools", "howto", "core", "exchange", "leaderboard", "fish_merchant", "gauntlet", "gauntlet_forge"}       # MenuActions.java
EQUIP_SLOTS = {"head", "chest", "legs", "feet", "hand", "off_hand"}

def _drop_ids(entry):
    ids = [d["id"] for d in entry.get("independent") or []]
    choices = entry.get("choices") or {}
    ids += [d["id"] for d in choices.get("options") or []]
    return ids

def validate(built):
    errors, warnings = [], []
    get = lambda name: built.get(name) or {}

    resources = dict(get("resources"))
    for rid, entry in resources.items():
        if not entry.get("collection") or entry["collection"] < 1:
            errors.append(f"resource '{rid}' needs a collection amount of 1 or more")
    for rid in get("resources_special"):
        if rid in resources:
            errors.append(f"resource '{rid}' is in both resources.csv and resources_special.csv")
    resources.update(get("resources_special"))
    gear = {}
    for name in ("armor", "weapons", "other"):
        gear.update(get(name))
    items = set(resources) | set(gear)
    enchants = set(get("enchants"))
    icons = set(get("icons"))
    guis = get("gui_shops")
    mobs = {}
    for name in built:
        if name.startswith("mobs_"):
            mobs.update(get(name))

    for rid, entry in get("resources_special").items():
        if not entry.get("unlocks") and not entry.get("summons"):
            errors.append(f"special resource '{rid}' needs unlocks or summons")
        if entry.get("summons") and entry["summons"] not in mobs:
            errors.append(f"special resource '{rid}' summons unknown mob '{entry['summons']}'")

    for gid, entry in gear.items():
        for enchant in (entry.get("enchants") or {}):
            if enchant not in enchants:
                errors.append(f"gear '{gid}': unknown enchant '{enchant}'")

    mined = set()
    for bid, entry in get("blocks").items():
        for drop in _drop_ids(entry):
            mined.add(drop)
            if drop not in items:
                errors.append(f"block '{bid}': drops unknown item '{drop}'")

    dropped, worn = set(), set()
    for mid, entry in mobs.items():
        for slot, item in (entry.get("equip") or {}).items():
            worn.add(item)
            if slot not in EQUIP_SLOTS:
                errors.append(f"mob '{mid}': unknown equip slot '{slot}'")
            # UPPER_CASE is a plain vanilla item (hand:BOW), anything else an Aurora item
            if item not in items and not item.isupper():
                errors.append(f"mob '{mid}': wears unknown item '{item}'")
        for drop in _drop_ids(entry):
            dropped.add(drop)
            if drop not in items:
                errors.append(f"mob '{mid}': drops unknown item '{drop}'")

    # upgrades:<zone> opens that zone's upgrade menu
    upgrade_zones = {row["id"] for row in read_rows(data_path("zones.csv")) or []}
    # fishing:<spot> opens that spot's rod shop
    fishing_spots = {row["id"] for row in read_rows(os.path.join(DATA_DIR, "fishing", "spots.csv")) or []}

    sold, costs_used = set(), {}
    for gui_id, gui in guis.items():
        if gui.get("icon") and gui["icon"] not in icons:
            errors.append(f"gui '{gui_id}': unknown icon '{gui['icon']}'")
        layout = gui.get("layout") or []
        if len(layout) != gui.get("rows"):
            errors.append(f"gui '{gui_id}': {len(layout)} layout rows for rows {gui.get('rows')}")
        components = gui.get("components") or {}
        used = set()
        # Every menu is framed and can be closed: no empty slot on the outer ring,
        # and the Close button sits bottom right where players look for it
        if layout and all(len(row) == 9 for row in layout):
            ring = layout[0] + layout[-1] + "".join(row[0] + row[-1] for row in layout[1:-1])
            if " " in ring:
                errors.append(f"gui '{gui_id}': the border has an empty slot")
            if layout[-1][-1] != "X":
                errors.append(f"gui '{gui_id}': no Close button (X) in the bottom right")
        for row in layout:
            if len(row) != 9:
                errors.append(f"gui '{gui_id}': layout row '{row}' is not 9 wide")
            for key in row:
                if key in GUI_SPECIAL:
                    continue
                used.add(key)
                if key not in components:
                    errors.append(f"gui '{gui_id}': '{key}' in the layout has no component")
        for key, comp in components.items():
            if key not in used:
                warnings.append(f"gui '{gui_id}': component '{key}' is not in the layout")
            ctype, cid = comp.get("type"), comp.get("id")
            if comp.get("icon") and comp["icon"] not in icons:
                errors.append(f"gui '{gui_id}/{key}': unknown icon '{comp['icon']}'")
            if ctype == "GUI" and cid not in guis:
                errors.append(f"gui '{gui_id}/{key}': unknown menu '{cid}'")
            elif ctype == "ICON" and cid not in icons:
                errors.append(f"gui '{gui_id}/{key}': unknown icon '{cid}'")
            elif ctype == "ACTION" and cid.startswith(UPGRADES_ACTION):
                if cid[len(UPGRADES_ACTION):] not in upgrade_zones:
                    errors.append(f"gui '{gui_id}/{key}': no zone '{cid[len(UPGRADES_ACTION):]}' in zones.csv")
            elif ctype == "ACTION" and cid not in ACTIONS:
                errors.append(f"gui '{gui_id}/{key}': unknown action '{cid}'")
            elif ctype == "TRADE":
                if cid not in items:
                    errors.append(f"gui '{gui_id}/{key}': sells unknown item '{cid}'")
                sold.add(cid)
                for cost in comp.get("cost") or []:
                    costs_used.setdefault(cost["id"], []).append(f"{gui_id}/{key}")

    realms = get("realms")
    for rid, entry in realms.items():
        if entry.get("part_of") and entry["part_of"] not in realms:
            errors.append(f"realm '{rid}': is part of unknown realm '{entry['part_of']}'")
        if entry.get("base") and entry["base"] not in realms:
            errors.append(f"realm '{rid}': builds on unknown realm '{entry['base']}'")
    for tid, entry in get("tools").items():
        if not entry.get("materials"):
            errors.append(f"tool '{tid}' needs at least one material")

    tags = {entry.get("unlocks") for entry in get("resources_special").values()}
    for wid, entry in get("warps").items():
        if entry.get("icon") not in icons:
            errors.append(f"warp '{wid}': unknown icon '{entry.get('icon')}'")
        if entry.get("tag") and entry["tag"] not in tags:
            errors.append(f"warp '{wid}': no key unlocks the tag '{entry['tag']}'")

    for tid, entry in get("traders").items():
        opens = entry.get("gui") or ""
        if opens.startswith(UPGRADES_ACTION):
            if opens[len(UPGRADES_ACTION):] not in upgrade_zones:
                errors.append(f"trader '{tid}': no zone '{opens[len(UPGRADES_ACTION):]}' in zones.csv")
        elif opens.startswith(FISHING_ACTION):
            if opens[len(FISHING_ACTION):] not in fishing_spots:
                errors.append(f"trader '{tid}': no fishing spot '{opens[len(FISHING_ACTION):]}' in fishing/spots.csv")
        elif opens not in guis and opens not in ACTIONS:
            errors.append(f"trader '{tid}': unknown gui '{opens}'")

    # Shop gear is earned by buying it, mob gear by killing for it, starter gear is given
    earned = mined | dropped | sold
    for cost_id, where in costs_used.items():
        if cost_id not in items:
            errors.append(f"shop {where[0]}: costs unknown item '{cost_id}'")
        elif cost_id not in earned and not cost_id.startswith("starter_"):
            errors.append(f"'{cost_id}' is a cost ({where[0]}) but no block, mob or shop gives it")

    for gid in gear:
        if gid not in sold and gid not in dropped and gid not in worn and not gid.startswith("starter_"):
            warnings.append(f"gear '{gid}' is never sold, dropped or worn")
    for rid in resources:
        if rid not in mined and rid not in dropped and rid not in sold:
            warnings.append(f"resource '{rid}' is never mined, dropped or sold")
        # A key is used up by right clicking it, not spent in a shop
        elif rid not in costs_used and rid not in get("resources_special"):
            warnings.append(f"resource '{rid}' is never used as a cost")

    return errors, warnings

UPGRADE_HEADER = """\
# ===========================================================
# AURORA UPGRADES - generated from data/zones.csv and data/upgrades.csv
# ===========================================================
# Every zone's upgrade steps in purchase order. The prices are not written by
# hand, scripts/balance/prices.py works them out from the zone's time target.
# [zone_id]:
#   name: "Zone Name"
#   realm: overworld (The realm the levels bought here belong to)
#   region: origin_isles (The world it is part of, maxing all of a world's zones earns its rank)
#   released: true (Its region is marked released in regions.csv: hidden from players when false)
#   before: [cobble, coal] (The zones played before it, a tool's upgrades here wait on all of that tool's there)
#   steps:
#     - tool: pick
#       stat: fortune (An enchant, or material)
#       level: "2" (The level it reaches, a material by name)
#       cost:
#         - id: [item_id]
#           amount: 96
# ===========================================================
# DO NOT EDIT BY HAND -- run `python generate.py`
# ===========================================================
"""


def earlier_zones(zones, regions):
    """zone id -> the zones played before it, in zones.csv order: the ones it comes
    after in its own region, and every zone of the regions that lead to it. A region
    follows the one in its after column, or in portal when a gate leads in. Regions
    side by side, like Gormak and Verdant Groves after Origin Isles, never lead to
    each other, so neither waits on the other."""
    by_region = {r["id"]: r for r in regions}
    by_zone = {z["id"]: z for z in zones}

    def leading(region):
        found = []
        row = by_region.get(region)
        while row:
            parent = (row.get("after") or "").strip() or (row.get("portal") or "").strip()
            if not parent or parent == region or parent in found:
                break
            found.append(parent)
            row = by_region.get(parent)
        return set(found)

    result = {}
    for zone in zones:
        before = set()
        current = by_zone.get((zone.get("after") or "").strip())
        while current and current["id"] not in before and current["region"] == zone["region"]:
            before.add(current["id"])
            current = by_zone.get((current.get("after") or "").strip())
        regions_before = leading(zone["region"])
        result[zone["id"]] = [z["id"] for z in zones if z["id"] in before or z["region"] in regions_before]
    return result


def upgrades_text():
    """upgrades.yml as text, None without zones.csv. Raises on bad data."""
    if not os.path.exists(data_path("zones.csv")):
        return None, 0
    sys.path.insert(0, os.path.join(CUR, "balance"))
    import price_zones

    lines = [UPGRADE_HEADER, "items:"]
    regions = read_rows(data_path("regions.csv")) or []
    # Regions marked released in regions.csv are shown to players, the rest are hidden
    released = {r["id"] for r in regions if (r.get("released") or "").strip().lower() in ("yes", "true", "1")}
    zones = price_zones.price_all()
    before = earlier_zones([zone for zone, _ in zones], regions)
    for zone, priced in zones:
        lines.append(f"  {zone['id']}:")
        lines.append(f"    name: \"{zone['name']}\"")
        lines.append(f"    realm: {zone['realm']}")
        lines.append(f"    region: {zone['region']}")
        lines.append(f"    released: {'true' if zone['region'] in released else 'false'}")
        lines.append(f"    before: [{', '.join(before[zone['id']])}]")
        lines.append("    steps:")
        for row in priced:
            lines.append(f"      - tool: {row['tool']}")
            lines.append(f"        stat: {row['stat']}")
            lines.append(f"        level: \"{row['level']}\"")
            lines.append("        cost:")
            for resource, amount in row["cost"].items():
                lines.append(f"          - id: {resource}")
                lines.append(f"            amount: {amount}")
        lines.append("")
    return "\n".join(lines), len(zones)


def write_generated(name, text, count, check_only, changed):
    """Writes one file built outside the schemas, reporting it like the rest."""
    target = os.path.join(RESOURCE_DIR, name)
    existing = None
    if os.path.exists(target):
        with open(target, encoding="utf-8") as f:
            existing = f.read()
    if existing == text:
        print(f"  = {name:<24} {count} entries (unchanged)")
        return
    changed.append(name)
    if check_only:
        print(f"  ! {name:<24} {count} entries (would change)")
        return
    with open(target, "w", encoding="utf-8") as f:
        f.write(text)
    print(f"  > {name:<24} {count} entries")

FISHING_HEADER = """# ===========================================================
# AURORA FISHING - generated from data/fishing/*.csv
# ===========================================================
# The side quest: one fishing spot a realm, each with its own fish and its own rod
# upgrades, apart from the zones. Prices come from scripts/balance/fishing_prices.py
# [spot_id]:
#   name: "<color><b>Spot Name"
#   realm: overworld (The realm whose worlds the spot's areas are in)
#   order: 1 (Spots in the order they are played, the rod carries over)
#   fish: (A cast rolls 1-100 plus the rod's Luck, the fish with the highest score not above it bites)
#     - id: perch (A resource)
#       tier: common (junk, common, uncommon, rare)
#       score: 20
#       min: 1, max: 2 (How many, before Fortune)
#       density: 0 (The rod needs this much Fortune, past it the Fortune curve multiplies)
#       essence: 0.28 (What the Fish Merchant pays for one)
#   rod: (The rod's upgrades sold at this spot, in order)
#     - stat: fortune (fortune, luck or lure)
#       level: 1 (The total the rod has after it)
#       costs: [{id: perch, amount: 4}]
# ===========================================================
# DO NOT EDIT BY HAND -- run `python generate.py`
# ==========================================================="""

def fishing_text(resources):
    """fishing.yml as text, its spot count and any errors; None without the fishing data."""
    if not os.path.exists(os.path.join(DATA_DIR, "fishing", "spots.csv")):
        return None, 0, []
    sys.path.insert(0, os.path.join(CUR, "balance"))
    import fishing_prices

    errors = []
    realms = {row["id"] for row in read_rows(data_path("realms.csv")) or []}
    items = OrderedDict()
    for spot, fish, priced, essence in fishing_prices.build():
        if spot["realm"] not in realms:
            errors.append(f"fishing spot '{spot['id']}': unknown realm '{spot['realm']}'")
        entries = []
        for f in fish:
            if f["id"] not in resources:
                errors.append(f"fishing spot '{spot['id']}': fish '{f['id']}' is not in resources.csv")
            low, _, high = f["amount"].partition("-")
            entries.append(OrderedDict(id=f["id"], tier=f["tier"], score=int(f["score"]), min=int(low),
                                       max=int(high or low), density=int(f["density"]), essence=essence[f["id"]]))
        steps = []
        for step in priced:
            for resource in step["cost"]:
                if resource not in {f["id"] for f in fish}:
                    errors.append(f"fishing rod at '{spot['id']}': costs '{resource}', not a fish of that spot")
            steps.append(OrderedDict(stat=step["stat"], level=step["level"],
                                     costs=[OrderedDict(id=r, amount=a) for r, a in step["cost"].items()]))
        items[spot["id"]] = OrderedDict(name=Quoted(spot["name"]), realm=spot["realm"], order=int(spot["order"]),
                                        fish=entries, rod=steps)
    return dump({"header": FISHING_HEADER}, items), len(items), errors

GAUNTLET_HEADER = """# ===========================================================
# AURORA GAUNTLET - generated from data/gauntlet/*.csv
# ===========================================================
# The tower mode, apart from the zones. Floor and gear numbers come from
# scripts/balance/gauntlet_balance.py; mobs are in mobs/gauntlet.yml
# floors: one a floor, in order. Past the last the plugin repeats it, harder
#   - mobs: {g_rotter: 3} (mob id: how many)
#     health: 1.05 (every mob's health times this)
#     attack: 1 (attack levels added to every mob)
#     kill: 1 (Shards each mob killed pays, a boss five times it; a boss is a boss floor's one mob)
#     shards: 4, cores: 0 (the bonus for clearing it)
# gear: what can be upgraded, in the forge's order
#   sword: {name, item, materials: [WOODEN, ...] or [NONE], kind: melee|ranged|thrown|armor|shield,
#           damage: 100 (% of a melee hit), unlock_shards, unlock_floor (best floor needed)}
# levels: [{level, shards, cores, material (index into materials), melee, ranged,
#           defence, protection, deflection}] (what each gear level costs and gives)
# boons: the run's upgrades, offered every 5 floors {name, icon, description, stat, amount, max,
#        effects: {stat: amount} (stat and amount in the csv can list several, joined by +), requires}
# care: care package levels [{level, shards, cores, loot: [{item, min, max, chance}]}]
#   item is a material, or POTION@TYPE / SPLASH_POTION@TYPE
# milestones: [{floor, essence, cosmetic}] (best floor reached, paid once)
# boons also have a rarity: common, rare or epic, weighted by the rarity_ settings, or legendary: a
# legendary requires two or more boons (joined by +) and is offered once each has legendary_stacks stacks
# talents: permanent, run-only upgrades {name, icon, description, stat, amount (a level), max,
#          shards (the first level), growth (each level costs this many times the last), cores (a level)}
# mutators: what can change a floor {name, description, health, damage, speed, count, elites (times),
#           shards (percent more for the floor), affix (every mob but an elite carries it, from affix_from)}
# challenges: the daily challenge pool {description ({goal} is filled in), stat, goal, shards, cores,
#             scale (the goal rises to the player's best floor, rounded down to 5)}
# bosses: a boss's abilities and their cooldown in seconds {g_rot_king: [{type: summon, cooldown: 14}, ...]}
#         (slam, volley, charge, leap, summon, fireball, blink or sonic, see GauntletBossAbilities)
# perks: what gear unlocks at a level, in runs only [{gear, level, name, description, stat, amount}]
#        (gear armor_set: every armour piece at the level)
# affixes: what an elite can roll from affix_from on {name, colour, description} (see GauntletRuns)
# achievements: long goals [{id, name, description ({goal} filled in), stat, goal, essence, shards, cosmetic}]
# shop: cosmetics sold for Shards in the Shard Shop [{cosmetic, shards}]
# settings: the gauntlet's tuning numbers, by key (settings.csv says what each one does)
# setup: the starting kit, bought for good, handed out at the start of every run
#   quiver: {name, item (as care loot), amount (a level), max, shards, cores (level n costs n times), description}
# ===========================================================
# DO NOT EDIT BY HAND -- run `python generate.py`
# ==========================================================="""

GAUNTLET_KINDS = {"melee", "ranged", "thrown", "armor", "shield"}
GAUNTLET_STATS = {"damage", "crit", "killheal", "health", "speed", "regen", "guard", "dodge", "absorb",
                  "firststrike", "shards", "execute", "slayer", "cleave", "revive"}
GAUNTLET_RARITIES = {"common", "rare", "epic", "legendary"}
GAUNTLET_TALENT_STATS = {"health", "shards", "care", "reroll", "insight", "revive", "headstart"}
GAUNTLET_CHALLENGE_STATS = {"floor", "kills", "elite_kills", "fast_clears", "boss_kills", "pvp_kills", "runs"}
GAUNTLET_BOSS_ABILITIES = {"slam", "volley", "charge", "leap", "summon", "fireball", "blink", "sonic"}
GAUNTLET_WEAPON_PERK_STATS = {"damage", "crit", "firststrike", "execute", "slayer", "cleave", "killheal"}
GAUNTLET_SET_PERK_STATS = {"health", "guard", "absorb"}
GAUNTLET_AFFIXES = {"vampiric", "explosive", "shielded", "swift", "frenzied", "summoner"}
GAUNTLET_ACHIEVEMENT_STATS = {"kills", "elite_kills", "boss_kills", "hoarders", "fast_clears", "best_combo", "runs",
                              "best", "pvp_kills", "duel_wins", "revives", "party_runs", "discovered",
                              "tower_climbs", "tower_wins"}


def _gauntlet_rows(name):
    path = os.path.join(DATA_DIR, "gauntlet", name)
    if not os.path.exists(path):
        return None
    with open(path, newline="", encoding="utf-8") as f:
        return [row for row in csv.DictReader(f) if any((v or "").strip() for v in row.values())]


def _gauntlet_loot(text, where, errors):
    loot = []
    for part in [p for p in (text or "").split(LIST_SEP) if p.strip()]:
        bits = part.strip().split(PART_SEP)
        if len(bits) != 3:
            errors.append(f"gauntlet {where}: loot '{part}' is not item:min-max:chance")
            continue
        low, _, high = bits[1].partition(RANGE_SEP)
        loot.append(OrderedDict(item=bits[0].upper(), min=int(low), max=int(high or low), chance=float(bits[2])))
    return loot


def gauntlet_text(mobs, cosmetics):
    """gauntlet.yml as text, its floor count and any errors; None without the gauntlet data."""
    floors_rows = _gauntlet_rows("floors.csv")
    if floors_rows is None:
        return None, 0, []
    errors = []
    floors = []
    for row in floors_rows:
        spawns = OrderedDict()
        for part in [p for p in row["mobs"].split(LIST_SEP) if p.strip()]:
            mob, _, count = part.partition(PART_SEP)
            if mob not in mobs:
                errors.append(f"gauntlet floor {row['floor']}: no mob '{mob}' in mobs_gauntlet.csv")
            spawns[mob] = int(count or 1)
        if not row.get("health"):
            errors.append(f"gauntlet floor {row['floor']}: no numbers, run scripts/balance/gauntlet_balance.py --write")
            continue
        # On a boss floor the boss is the one mob of a kind; a second single mob would be a second boss
        if int(row["cores"] or 0) > 0 and sum(1 for count in spawns.values() if count == 1) != 1:
            errors.append(f"gauntlet floor {row['floor']}: a boss floor needs exactly one mob with a count of 1, its boss")
        floors.append(OrderedDict(mobs=spawns, health=float(row["health"]), attack=int(row["attack"]),
                                  kill=int(row["kill"]), shards=int(row["shards"]), cores=int(row["cores"])))

    gear = OrderedDict()
    for row in _gauntlet_rows("gear.csv") or []:
        if row["kind"] not in GAUNTLET_KINDS:
            errors.append(f"gauntlet gear '{row['id']}': unknown kind '{row['kind']}'")
        gear[row["id"]] = OrderedDict(name=row["name"], item=row["item"], materials=row["materials"].split(LIST_SEP),
                                      kind=row["kind"], damage=int(row["damage"] or 100),
                                      unlock_shards=int(row["unlock_shards"] or 0), unlock_floor=int(row["unlock_floor"] or 0))

    levels = [OrderedDict((k, int(v)) for k, v in row.items()) for row in _gauntlet_rows("gear_levels.csv") or []]
    if not levels:
        errors.append("gauntlet: no gear_levels.csv, run scripts/balance/gauntlet_balance.py --write")

    boons = OrderedDict()
    for row in _gauntlet_rows("boons.csv") or []:
        stats = [stat.strip() for stat in row["stat"].split("+")]
        amounts = [float(amount) for amount in str(row["amount"]).split("+")]
        if len(stats) != len(amounts):
            errors.append(f"gauntlet boon '{row['id']}': {len(stats)} stats but {len(amounts)} amounts")
        for stat in stats:
            if stat not in GAUNTLET_STATS:
                errors.append(f"gauntlet boon '{row['id']}': unknown stat '{stat}'")
        rarity = (row.get("rarity") or "common").strip()
        if rarity not in GAUNTLET_RARITIES:
            errors.append(f"gauntlet boon '{row['id']}': unknown rarity '{rarity}'")
        requires = [boon.strip() for boon in (row.get("requires") or "").split("+") if boon.strip()]
        if (rarity == "legendary") != (len(requires) > 0):
            errors.append(f"gauntlet boon '{row['id']}': a legendary boon, and only a legendary, requires other boons")
        # stat and amount are the first effect, for a plugin that reads only those
        boons[row["id"]] = OrderedDict(name=row["name"], icon=row["icon"], description=Quoted(row["description"]),
                                       stat=stats[0], amount=amounts[0], effects=OrderedDict(zip(stats, amounts)),
                                       max=int(row["max"]), rarity=rarity, requires=requires)
    for boon_id, boon in boons.items():
        for required in boon["requires"]:
            if required not in boons or boons[required]["rarity"] == "legendary":
                errors.append(f"gauntlet boon '{boon_id}': requires '{required}', which is not a boon below legendary")

    talents = OrderedDict()
    for row in _gauntlet_rows("talents.csv") or []:
        if row["stat"] not in GAUNTLET_TALENT_STATS:
            errors.append(f"gauntlet talent '{row['id']}': unknown stat '{row['stat']}'")
        talents[row["id"]] = OrderedDict(name=row["name"], icon=row["icon"], description=Quoted(row["description"]),
                                         stat=row["stat"], amount=float(row["amount"]), max=int(row["max"]),
                                         shards=int(row["shards"]), growth=float(row["growth"]), cores=int(row["cores"] or 0))

    mutators = OrderedDict()
    for row in _gauntlet_rows("mutators.csv") or []:
        affix = (row.get("affix") or "").strip()
        if affix and affix not in GAUNTLET_AFFIXES:
            errors.append(f"gauntlet mutator '{row['id']}': unknown affix '{affix}'")
        mutators[row["id"]] = OrderedDict(name=row["name"], description=Quoted(row["description"]),
                                          health=float(row["health"]), damage=float(row["damage"]), speed=float(row["speed"]),
                                          count=float(row["count"]), elites=float(row["elites"]), shards=int(row["shards"]),
                                          affix=affix)

    challenges = OrderedDict()
    for row in _gauntlet_rows("challenges.csv") or []:
        if row["stat"] not in GAUNTLET_CHALLENGE_STATS:
            errors.append(f"gauntlet challenge '{row['id']}': unknown stat '{row['stat']}'")
        challenges[row["id"]] = OrderedDict(description=Quoted(row["description"]), stat=row["stat"], goal=int(row["goal"]),
                                            shards=int(row["shards"]), cores=int(row["cores"] or 0),
                                            scale=(row.get("scale") or "").strip().lower() == "yes")

    bosses = OrderedDict()
    for row in _gauntlet_rows("bosses.csv") or []:
        if row["id"] not in mobs:
            errors.append(f"gauntlet boss '{row['id']}': no such mob in mobs_gauntlet.csv")
        abilities = []
        for part in [p for p in (row["abilities"] or "").split(LIST_SEP) if p.strip()]:
            kind, _, cooldown = part.strip().partition(PART_SEP)
            if kind not in GAUNTLET_BOSS_ABILITIES:
                errors.append(f"gauntlet boss '{row['id']}': unknown ability '{kind}'")
            abilities.append(OrderedDict(type=kind, cooldown=int(cooldown or 10)))
        bosses[row["id"]] = abilities

    perks = []
    for row in _gauntlet_rows("perks.csv") or []:
        armour = row["gear"] == "armor_set"
        if not armour and row["gear"] not in gear:
            errors.append(f"gauntlet perk '{row['name']}': no gear '{row['gear']}' in gear.csv")
        allowed = GAUNTLET_SET_PERK_STATS if armour else GAUNTLET_WEAPON_PERK_STATS
        if row["stat"] not in allowed:
            errors.append(f"gauntlet perk '{row['name']}': stat '{row['stat']}' does not work on {row['gear']}")
        perks.append(OrderedDict(gear=row["gear"], level=int(row["level"]), name=row["name"],
                                 description=Quoted(row["description"]), stat=row["stat"], amount=float(row["amount"])))

    affixes = OrderedDict()
    for row in _gauntlet_rows("affixes.csv") or []:
        if row["id"] not in GAUNTLET_AFFIXES:
            errors.append(f"gauntlet affix '{row['id']}': no such affix in the plugin")
        affixes[row["id"]] = OrderedDict(name=row["name"], colour=row["colour"], description=Quoted(row["description"]))

    achievements = []
    for row in _gauntlet_rows("achievements.csv") or []:
        if row["stat"] not in GAUNTLET_ACHIEVEMENT_STATS:
            errors.append(f"gauntlet achievement '{row['id']}': unknown stat '{row['stat']}'")
        cosmetic = (row.get("cosmetic") or "").strip()
        if cosmetic and cosmetics is not None and cosmetic not in cosmetics:
            errors.append(f"gauntlet achievement '{row['id']}': no cosmetic '{cosmetic}' in Cosmetics.java")
        achievements.append(OrderedDict(id=row["id"], name=row["name"], description=Quoted(row["description"]), stat=row["stat"],
                                        goal=int(row["goal"]), essence=int(row["essence"] or 0), shards=int(row["shards"] or 0),
                                        cosmetic=cosmetic))

    shop = []
    for row in _gauntlet_rows("shardshop.csv") or []:
        if cosmetics is not None and row["cosmetic"] not in cosmetics:
            errors.append(f"gauntlet shard shop: no cosmetic '{row['cosmetic']}' in Cosmetics.java")
        shop.append(OrderedDict(cosmetic=row["cosmetic"], shards=int(row["shards"])))

    settings = OrderedDict()
    for row in _gauntlet_rows("settings.csv") or []:
        try:
            settings[row["key"]] = float(row["value"])
        except ValueError:
            errors.append(f"gauntlet setting '{row['key']}': '{row['value']}' is not a number")

    care = [OrderedDict(level=int(row["level"]), shards=int(row["shards"]), cores=int(row["cores"]),
                        loot=_gauntlet_loot(row["loot"], f"care package {row['level']}", errors))
            for row in _gauntlet_rows("care_package.csv") or []]

    milestones = []
    for row in _gauntlet_rows("milestones.csv") or []:
        cosmetic = (row.get("cosmetic") or "").strip()
        if cosmetic and cosmetics is not None and cosmetic not in cosmetics:
            errors.append(f"gauntlet milestone {row['floor']}: no cosmetic '{cosmetic}' in Cosmetics.java")
        milestones.append(OrderedDict(floor=int(row["floor"]), essence=int(row["essence"] or 0), cosmetic=cosmetic))

    setup = OrderedDict()
    for row in _gauntlet_rows("setup.csv") or []:
        setup[row["id"]] = OrderedDict(name=row["name"], item=row["item"].upper(), amount=int(row["amount"]),
                                       max=int(row["max"]), shards=int(row["shards"]), cores=int(row["cores"] or 0),
                                       description=Quoted(row["description"]))

    # Each band of floors' theme, from its first floor
    themes = []
    for row in _gauntlet_rows("themes.csv") or []:
        themes.append(OrderedDict(floor=int(row["from"]), name=row["name"], colour=row["colour"],
                                  description=Quoted(row["description"])))
    if themes and themes[0]["floor"] != 1:
        errors.append("gauntlet themes: the first theme has to start on floor 1")
    data = OrderedDict(floors=floors, gear=gear, levels=levels, boons=boons, care=care, milestones=milestones, setup=setup,
                       talents=talents, mutators=mutators, challenges=challenges, bosses=bosses, perks=perks,
                       affixes=affixes, achievements=achievements, shop=shop, settings=settings, themes=themes)
    body = yaml.dump(data, Dumper=_Dumper, sort_keys=False, default_flow_style=False, allow_unicode=True,
                     width=10 ** 6, indent=2)
    return GAUNTLET_HEADER + "\n" + body, len(floors), errors


def java_cosmetics():
    """The suffix and trail ids Cosmetics.java defines, None if it cannot be read."""
    path = os.path.join(CUR, "..", "src", "main", "java", "org", "charky", "aurora", "features", "cosmetic", "Cosmetics.java")
    if not os.path.exists(path):
        return None
    with open(path, encoding="utf-8") as f:
        return {m.group(1) + "_" + m.group(2) for m in re.finditer(r'^\s*(suffix|trail)\("([a-z0-9_]+)"', f.read(), re.M)}


def main(check_only=False):
    print("Generating CSV to YAML Conversion")
    changed, skipped, errors = [], [], []
    built = {}
    for name, schema in SCHEMAS.items():
        try:
            items = build(schema)
        except (ValueError, KeyError) as exc:
            errors.append(f"{name}: {exc}")
            continue
        built[name] = items

        if items is None:
            skipped.append(f"{name} (no {schema['csv']})")
            continue

        text = dump(schema, items)
        target = os.path.join(RESOURCE_DIR, schema["out"])

        existing = None
        if os.path.exists(target):
            with open(target, encoding="utf-8") as f:
                existing = f.read()

        if existing == text:
            print(f"  = {schema['out']:<24} {len(items)} entries (unchanged)")
            continue

        changed.append(schema["out"])
        if check_only:
            print(f"  ! {schema['out']:<24} {len(items)} entries (would change)")
        else:
            os.makedirs(os.path.dirname(target), exist_ok=True)
            with open(target, "w", encoding="utf-8") as f:
                f.write(text)
            print(f"  > {schema['out']:<24} {len(items)} entries")

    # The upgrade prices come from the formula, not from a schema
    try:
        text, count = upgrades_text()
    except Exception as exc:
        text, count = None, 0
        errors.append(f"upgrades: {exc}")
    if text is not None:
        write_generated("upgrades.yml", text, count, check_only, changed)

    # Fishing's spots, fish and rod, priced by scripts/balance/fishing_prices.py
    try:
        text, count, fishing_errors = fishing_text(built.get("resources") or {})
        errors += fishing_errors
    except Exception as exc:
        text, count = None, 0
        errors.append(f"fishing: {exc}")
    if text is not None:
        write_generated("fishing.yml", text, count, check_only, changed)

    # The gauntlet's floors, gear, boons, care packages and milestones
    try:
        text, count, gauntlet_errors = gauntlet_text(built.get("mobs_gauntlet") or {}, java_cosmetics())
        errors += gauntlet_errors
    except Exception as exc:
        text, count = None, 0
        errors.append(f"gauntlet: {exc}")
    if text is not None:
        write_generated("gauntlet.yml", text, count, check_only, changed)

    # Only when every file built, a missing one would report everything it holds
    if not errors and not skipped:
        cross_errors, warnings = validate(built)
        for warning in warnings:
            print(f"  ? {warning}")
        errors += cross_errors

    for item in skipped:
        print(f"  - skipped {item}")
    for err in errors:
        print(f"  x {err}")

    if errors:
        return 1
    if check_only and changed:
        return 1
    return 0

if __name__ == "__main__":
    # Error code is set correctly
    sys.exit(main())