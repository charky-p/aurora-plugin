#!/usr/bin/env python3
# Aurora Data Generator

# import argparse
# import csv
import yaml

# --------------------------------------------------------------------------
# Schemas - TODO: implement csv strategy
# --------------------------------------------------------------------------

RESOURCE_HEADER = """\
# ===========================================================
# AURORA RESOURCE TEMPLATE  --  generated from data/resources.csv
# ===========================================================
# [resource_id]:
#   display:  "<color><b>Cobblestone"
#   material: [BUKKIT_MATERIAL]
#   category: ores          (backpack section, see backpack.yml)
#   world:    "overworld"
#   region:   "origin_isles"
#   lore:     (Optional)
#     - "<gray>Lore line"
# ===========================================================
# DO NOT EDIT BY HAND -- run `python generate.py`
# ==========================================================="""

BLOCK_HEADER = """\
# ===========================================================
# AURORA BLOCK TEMPLATE  --  generated from data/blocks.csv
# ===========================================================
# [block_id]:
#   material: [BUKKIT_MATERIAL]
#   density:  0   (subtracts from fortune)
#   independent:  (every drop rolls separately)
#     - id: [resource_id]
#       chance: 100 / min: 1 / max: 1
#   choices:      (exactly one drop is picked)
#     chance: 100
#     options:
#       - id: [resource_id]
#         weight: 50
# ===========================================================
# DO NOT EDIT BY HAND -- run `python generate.py`
# ==========================================================="""

GEAR_HEADER = """\
# ===========================================================
# AURORA GEAR TEMPLATE  --  generated from CSV
# ===========================================================
# [gear_id]:
#   display:  "<color><b>Example Sword"
#   material: [BUKKIT_MATERIAL]
#   tier:     0   (0 hides the tier tag)
#   type:     "armor" | "combat" | "mining"
#   world:    "overworld"
#   region:   "origin_isles"
#   enchants: (Optional)  [enchant_id]: level
#   lore:     (Optional)
# ===========================================================
# DO NOT EDIT BY HAND -- run `python generate.py`
# ==========================================================="""

MOB_HEADER = """\
# ===========================================================
# AURORA MOB TEMPLATE  --  generated from CSV
# ===========================================================
# [mob_id]:
#   display: "<color><b>Display Name"
#   entity:  [BUKKIT_ENTITY_TYPE]
#   equip:   head/chest/legs/feet/hand/off_hand -> [item_id]
#   attributes:
#     - attr: [BUKKIT_ATTRIBUTE]
#       value: 1
#   independent / choices: see blocks.yml
# ===========================================================
# DO NOT EDIT BY HAND -- run `python generate.py`
# ==========================================================="""

ICON_HEADER = """\
# ===========================================================
# AURORA ICON TEMPLATE  --  generated from data/icons.csv
# ===========================================================
# [icon_id]:
#   display:  "<color><b>Display Name"
#   material: [BUKKIT_MATERIAL]
#   glow:     false
#   lore:     (Optional)
# ===========================================================
# DO NOT EDIT BY HAND -- run `python generate.py`
# ==========================================================="""

GUI_HEADER = """\
# ===========================================================
# AURORA GUI TEMPLATE  --  generated from CSV
# ===========================================================
# [gui_id]:
#   title:  "<color><b>Gui Title"
#   icon:   [icon_id]
#   rows:   3
#   layout: (one string per row, 9 chars each)
#     - "BBBBIBBBX"   B border, I info, X close, space empty
#   components:       (every other distinct char)
#     'a':
#       type: TRADE | GUI
#       id: [item_id] / [gui_id]
#       amount: 1
#       cost:
#         - id: [item_id]
#           amount: 1
# ===========================================================
# DO NOT EDIT BY HAND -- run `python generate.py`
# ==========================================================="""

TRADER_HEADER = """\
# ===========================================================
# AURORA TRADER TEMPLATE  --  generated from data/traders.csv
# ===========================================================
# [trader_id]:
#   display: "<color><b>Display Name"
#   entity:  [BUKKIT_ENTITY_TYPE]
#   gui:     [gui_id]
# ===========================================================
# DO NOT EDIT BY HAND -- run `python generate.py`
# ==========================================================="""
