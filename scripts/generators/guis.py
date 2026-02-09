from aurora_gen import AuroraGenerator
from typing import Dict, List, Optional

class GuiGenerator(AuroraGenerator):
    def __init__(self) -> None:
        header = """
# ===========================================================
# AURORA GUI TEMPLATE
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
"""
        super().__init__(header)

    def add_gui(
            self,
            gui_id: str,
            title: str,
            layout: List[str],
            components: Dict[str, Dict],
            icon: Optional[str] = None
    ) -> None:
        lines = [
            f"  {gui_id}:\n",
            f"    title: \"{title}\"\n"
        ]

        if icon:
            lines.append(f"    icon: {icon}\n")

        lines.append(f"    rows: {len(layout)}\n")

        lines.append("    layout:\n")
        for line in layout:
            lines.append(f"      - \"{line}\"\n")

        lines.append("    components:\n")
        for key, data in components.items():
            lines.append(f"      '{key}':\n")
            lines.append(f"        type: {data['type']}\n")
            lines.append(f"        id: {data['id']}\n")

            if 'amount' in data:
                lines.append(f"        amount: {data['amount']}\n")

            if 'cost' in data:
                if not data['cost']:
                    lines.append("        cost: []\n")
                else:
                    lines.append("        cost:\n")

                for c in data['cost']:
                    lines.append(f"          - id: {c['id']}\n")
                    if 'amount' in c:
                        lines.append(f"            amount: {c['amount']}\n")

        lines.append("\n")
        self.items.append("".join(lines))

def register_guis():
    register_shop_guis()
    register_tool_guis()

def register_shop_guis():
    gen = GuiGenerator()

    mine_shops = ["cobble", "coal", "copper", "iron", "gold"]
    poi_shops = ["city", "dunes", "temple", "tomb"]

    gen.add_gui("starter_shop", "<gray><b>Starter Shop", [
        "BBBBIBBBX", "BB1BBB2BB", "BBBBBBBBB"
    ], {
            "1": {"type": "TRADE", "id": "starter_pick_1", "cost": []},
            "2": {"type": "TRADE", "id": "starter_sword_1", "cost": []}
    })

    for shop in mine_shops:
        gen.add_gui(
            gui_id=f"{shop}_shop",
            title=f"<gray><b>{shop.capitalize()} Shop",
            layout=["BBBBIBBBX", "BB1BBB2BB", "BBBBBBBBB"],
            components={
                "1": {"type": "GUI", "id": f"{shop}_picks"},
                "2": {"type": "GUI", "id": f"{shop}_gear"}
            }
        )

    for shop in poi_shops:
        gen.add_gui(
            gui_id=f"gormak_{shop}_shop",
            title=f"<gold><b>{shop.capitalize()} Shop",
            layout=["BBBBIBBBX", "BB1BBB2BB", "BBBBBBBBB"],
            components={
                "1": {"type": "GUI", "id": f"gormak_{shop}_picks"},
                "2": {"type": "GUI", "id": f"gormak_{shop}_gear"}
            }
        )

    gen.save("guis/shops.yml")

def register_tool_guis():
    # TODO: ERRORS BELOW
    gen = GuiGenerator()

    mine_configs = {
        "cobble": ["Cobblestone", "cobblestone", [16, 32, 48, 96, 192], "starter_pick_1"],
        "coal":   ["Coal",        "coal",        [32, 64, 192, 320, 512], "cobble_pick_5"],
        "copper": ["Copper",      "copper",      [48, 128, 320, 512, 768], "coal_pick_5"],
        "iron":   ["Iron",        "iron",        [64, 192, 320, 640, 1024], "copper_pick_5"],
        "gold":   ["Gold",        "gold",        [96, 192, 384, 768, 1152], "iron_pick_5"]
    }

    for mid, data in mine_configs.items():
        name, resource, costs, prev_pick = data
        components = {}

        for i in range(1, 6):
            current_pick = f"{mid}_pick_{i}"
            req_item = f"{mid}_pick_{i-1}" if i > 1 else prev_pick

            components[str(i)] = {
                "type": "TRADE",
                "id": current_pick,
                "cost": [{"id": req_item}, {"id": resource, "amount": costs[i-1]}]
            }

        gen.add_gui(
            gui_id=f"{mid}_picks",
            title=f"<gray><b>{name} Picks",
            icon=f"{mid}_picks",
            layout=[
                "BBBBBBBBB",
                "B12345  B",
                "B       B",
                "B       B",
                "BBBBBBBBB"
            ],
            components=components
        )

    mine_gear_configs = ["cobble", "coal", "copper", "iron", "gold"]
    prev_map = {"cobble": "starter", "coal": "cobble", "copper": "coal", "iron": "copper", "gold": "iron"}
    costs_map = {"cobble": 32, "coal": 64, "copper": 128, "iron": 256, "gold": 512}

    for mid in mine_gear_configs:
        prev_mid = prev_map[mid]
        res = "cobblestone" if mid == "cobble" else mid
        amt = costs_map[mid]

        prev_sword = f"{prev_mid}_sword_1" if prev_mid == "starter" else f"{prev_mid}_sword_2"

        gen.add_gui(
            gui_id=f"{mid}_gear",
            title=f"<gray><b>{mid.capitalize()} Equipment",
            icon=f"{mid}_gear",
            layout=[
                "BBBBBBBBB",
                "B sS    B",
                "B hclb  B",
                "BBBBBBBBB"
            ],
            components={
                "s": {"type": "TRADE", "id": f"{mid}_sword_1", "cost": [{"id": prev_sword}, {"id": res, "amount": amt}]},
                "S": {"type": "TRADE", "id": f"{mid}_sword_2", "cost": [{"id": f"{mid}_sword_1"}, {"id": res, "amount": amt*2}]},
                "h": {"type": "TRADE", "id": f"{mid}_helmet_1", "cost": [{"id": res, "amount": amt}]},
                "c": {"type": "TRADE", "id": f"{mid}_chestplate_1", "cost": [{"id": res, "amount": amt}]},
                "l": {"type": "TRADE", "id": f"{mid}_leggings_1", "cost": [{"id": res, "amount": amt}]},
                "b": {"type": "TRADE", "id": f"{mid}_boots_1", "cost": [{"id": res, "amount": amt}]}
            }
        )

    # --- GORMAK Layout ---
    gormak_mapping = {
        "city":   ["Abandoned City", "serf", "scavenger", "tattered_cloth", "ancient_scrap"],
        "dunes":  ["The Shifting Dunes", "dune_scout", "stalker", "ancient_bone", "ancient_flesh"],
        "temple": ["Ancient Temple", "tomb_guard", "vizier", "ancient_nugget", "vizier_scroll"],
        "tomb":   ["Royal Tomb", "myrmidon", "gormak", "eternal_soul", "ancient_heart"]
    }

    for poi, data in gormak_mapping.items():
        name, m1, m2, res1, res2 = data

        gen.add_gui(
            gui_id=f"gormak_{poi}_picks",
            title=f"<gold><b>{name} Picks",
            icon=f"gormak_{poi}_picks",
            layout=["BBBBIBBBX", "BBBB1B2BB", "BBBBBBBBB"],
            components={
                "1": {"type": "TRADE", "id": f"gormak_{poi}_pick_1", "cost": [{"id": "gold_pick_5"}, {"id": res1, "amount": 64}]},
                "2": {"type": "TRADE", "id": f"gormak_{poi}_pick_2", "cost": [{"id": f"gormak_{poi}_pick_1"}, {"id": res2, "amount": 32}]}
            }
        )

        gen.add_gui(
            gui_id=f"gormak_{poi}_gear",
            title=f"<gold><b>{name} Gear",
            icon=f"gormak_{poi}_gear",
            layout=[
                "BBBBIBBBX",
                "B12 hclbB",
                "B34 HCLTB",
                "BBBBBBBBB"
            ],
            components={
                "1": {"type": "TRADE", "id": f"{m1}_sword_1", "cost": [{"id": "gold_sword_2"}, {"id": res1, "amount": 32}]},
                "2": {"type": "TRADE", "id": f"{m1}_sword_2", "cost": [{"id": f"{m1}_sword_1"}, {"id": res1, "amount": 64}]},
                "h": {"type": "TRADE", "id": f"{m1}_helmet_1", "cost": [{"id": "gold_helmet_1"}, {"id": res1, "amount": 32}]},
                "c": {"type": "TRADE", "id": f"{m1}_chestplate_1", "cost": [{"id": "gold_chestplate_1"}, {"id": res1, "amount": 32}]},
                "l": {"type": "TRADE", "id": f"{m1}_leggings_1", "cost": [{"id": "gold_leggings_1"}, {"id": res1, "amount": 32}]},
                "b": {"type": "TRADE", "id": f"{m1}_boots_1", "cost": [{"id": "gold_boots_1"}, {"id": res1, "amount": 32}]},
                "3": {"type": "TRADE", "id": f"{m2}_sword_1", "cost": [{"id": f"{m1}_sword_2"}, {"id": res2, "amount": 16}]},
                "4": {"type": "TRADE", "id": f"{m2}_sword_2", "cost": [{"id": f"{m2}_sword_1"}, {"id": res2, "amount": 32}]},
                "H": {"type": "TRADE", "id": f"{m2}_helmet_1", "cost": [{"id": f"{m1}_helmet_1"}, {"id": res2, "amount": 16}]},
                "C": {"type": "TRADE", "id": f"{m2}_chestplate_1", "cost": [{"id": f"{m1}_chestplate_1"}, {"id": res2, "amount": 16}]},
                "L": {"type": "TRADE", "id": f"{m2}_leggings_1", "cost": [{"id": f"{m1}_leggings_1"}, {"id": res2, "amount": 16}]},
                "T": {"type": "TRADE", "id": f"{m2}_boots_1", "cost": [{"id": f"{m1}_boots_1"}, {"id": res2, "amount": 16}]}
            }
        )

    gen.save("guis/tools.yml")