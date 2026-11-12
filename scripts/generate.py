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
#   unlocks: gormak (the tag it gives, portals locked to that tag let the player through)
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
#     head: [item_id] (Optional, item_id = gear_id or resource_id)
#     chest: [item_id] (Optional)
#     legs: [item_id] (Optional)
#     feet: [item_id] (Optional)
#     hand: [item_id] (Optional)
#     off_hand: [item_id] (Optional)
#   attributes: (Optional)
#     - attr: [BUKKIT_ATTRIBUTE]
#       value: 1
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
#   gui: [gui_id]
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
        ("unlocks",  Str(required=True)),
    ]),
}

SCHEMAS["blocks"] = {
    "csv": "blocks.csv",
    "out": "blocks.yml",
    "header": BLOCK_HEADER,
    "fields": OrderedDict([
        ("material", Str(required=True)),
        ("density",  Int(default=0)),
    ] + loot_fields()),
}

SCHEMAS["picks"] = gear_schema("gear_picks.csv", "gear/picks.yml", "mining")
SCHEMAS["armor"] = gear_schema("gear_armor.csv", "gear/armor.yml", "armor")
SCHEMAS["weapons"] = gear_schema("gear_weapons.csv", "gear/weapons.yml", "combat")
SCHEMAS["other"] = gear_schema("gear_other.csv", "gear/other.yml", "")

SCHEMAS["mobs_origin"] = {
    "csv": "mobs_origin.csv",
    "out": "mobs/origin.yml",
    "header": MOB_HEADER,
    "fields": OrderedDict([
        ("display",    Str(quote=True, required=True)),
        ("entity",     Str(required=True)),
        ("health",     Float(default=20.0, always=True)),
        ("defence",    Float(default=0.0)),
        ("attack",     Float(default=0.0)),
        ("equip",      PairMap(value=str)),
        ("attributes", PairList("attr", "value", value=float)),
    ] + loot_fields()),
}

SCHEMAS["mobs_gormak"] = {
    "csv": "mobs_gormak.csv",
    "out": "mobs/gormak.yml",
    "header": MOB_HEADER,
    "fields": OrderedDict([
        ("display",    Str(quote=True, required=True)),
        ("entity",     Str(required=True)),
        ("health",     Float(default=20.0, always=True)),
        ("defence",    Float(default=0.0)),
        ("attack",     Float(default=0.0)),
        ("attack_cooldown", Float(default=0.0)),
        ("equip",      PairMap(value=str)),
        ("attributes", PairList("attr", "value", value=float)),
    ] + loot_fields()),
}

# Same columns as Gormak
SCHEMAS["mobs_verdant"] = dict(SCHEMAS["mobs_gormak"], csv="mobs_verdant.csv", out="mobs/verdant.yml")

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
    rows = read_rows(os.path.join(DATA_DIR, schema["csv"]))
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
        child_rows = read_rows(os.path.join(DATA_DIR, child["csv"])) or []
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
ACTIONS = {"backpack", "armory", "spawn"}       # MenuActions.java
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
    for rid in get("resources_special"):
        if rid in resources:
            errors.append(f"resource '{rid}' is in both resources.csv and resources_special.csv")
    resources.update(get("resources_special"))
    gear = {}
    for name in ("picks", "armor", "weapons", "other"):
        gear.update(get(name))
    items = set(resources) | set(gear)
    enchants = set(get("enchants"))
    icons = set(get("icons"))
    guis = get("gui_shops")
    mobs = {}
    for name in built:
        if name.startswith("mobs_"):
            mobs.update(get(name))

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
            if item not in items:
                errors.append(f"mob '{mid}': wears unknown item '{item}'")
        for drop in _drop_ids(entry):
            dropped.add(drop)
            if drop not in items:
                errors.append(f"mob '{mid}': drops unknown item '{drop}'")

    sold, costs_used = set(), {}
    for gui_id, gui in guis.items():
        if gui.get("icon") and gui["icon"] not in icons:
            errors.append(f"gui '{gui_id}': unknown icon '{gui['icon']}'")
        layout = gui.get("layout") or []
        if len(layout) != gui.get("rows"):
            errors.append(f"gui '{gui_id}': {len(layout)} layout rows for rows {gui.get('rows')}")
        components = gui.get("components") or {}
        used = set()
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
            elif ctype == "ACTION" and cid not in ACTIONS:
                errors.append(f"gui '{gui_id}/{key}': unknown action '{cid}'")
            elif ctype == "TRADE":
                if cid not in items:
                    errors.append(f"gui '{gui_id}/{key}': sells unknown item '{cid}'")
                sold.add(cid)
                for cost in comp.get("cost") or []:
                    costs_used.setdefault(cost["id"], []).append(f"{gui_id}/{key}")

    for tid, entry in get("traders").items():
        if entry.get("gui") not in guis:
            errors.append(f"trader '{tid}': unknown gui '{entry.get('gui')}'")

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
        elif rid not in costs_used and not resources[rid].get("unlocks"):
            warnings.append(f"resource '{rid}' is never used as a cost")

    return errors, warnings

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