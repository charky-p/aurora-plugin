import os
from typing import List, Optional, Dict, Union

class AuroraGenerator:
    def __init__(self, header: str) -> None:
        self.header = header
        self.items: List[str] = []

    def get_output(self) -> str:
        body = "".join(["".join(inner) for inner in self.items])
        return f"{self.header}\nitems:\n{body}"

    def save(self, filename: str):
        target_path = os.path.join("..", "src", "main", "resources", filename)
        os.makedirs(os.path.dirname(target_path), exist_ok=True)

        with open(target_path, "w") as f:
            f.write(self.get_output())
        print(f"Successfully saved to: {target_path}")

class BlockGenerator(AuroraGenerator):
    def __init__(self) -> None:
        header = """
# ===========================================================
# AURORA BLOCK TEMPLATE
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
"""
        super().__init__(header)

    def add_block(
            self,
            block_id: str,
            material: str,
            density: int = 0,
            independent: Optional[List[Dict[str, Union[str, int, float]]]] = None,
            choices: Optional[Dict[str, Union[float, List[Dict]]]] = None
    ) -> None:
        lines = [
            f"  {block_id}:\n",
            f"    material: {material}\n"
        ]

        if density > 0:
            lines.append(f"    density: {density}\n")

        # Independent Drops
        if independent:
            lines.append(f"    independent:\n")
            for drop in independent:
                drop_id = drop.get("id")
                independent_chance = drop.get("chance", 100.0)
                min_v = drop.get("min", 1)
                max_v = drop.get("max", 1)

                lines.append(f"      - id: {drop_id}\n")
                if independent_chance != 100.0:
                    lines.append(f"        chance: {independent_chance}\n")
                if min_v != 1 or max_v != 1:
                    lines.append(f"        min: {min_v}\n")
                    lines.append(f"        max: {max_v}\n")

        # Choice Drops
        if choices:
            lines.append(f"    choices:\n")

            chance = choices.get("chance", 100.0)
            lines.append(f"      chance: {chance}\n")

            lines.append(f"      options:\n")
            options: List[Dict] = choices.get("options", [])
            for opt in options:
                lines.append(f"        - id: {opt['id']}\n")
                lines.append(f"          weight: {opt['weight']}\n")
                if "min" in opt: lines.append(f"          min: {opt['min']}\n")
                if "max" in opt: lines.append(f"          max: {opt['max']}\n")

        lines.append(f"\n")
        self.items.append(lines)

class ResourceGenerator(AuroraGenerator):
    def __init__(self, world: str, region: str) -> None:
        self.world = world
        self.region = region
        header = """
# ===========================================================
# AURORA RESOURCE TEMPLATE
# ===========================================================
# [resource_id]:
#   display: "<color><b><i>Cobblestone"
#   material: [BUKKIT_MATERIAL]
#   lore: (Optional)
#     - "<color><b><i>Lore Line"
#   world: "overworld" (Options: "overworld")
#   region: "origin_isles" (Defaults to "")
# ===========================================================
# NOTE: A compressed version is automatically created with
# compressed_ prefix, e.g. compressed_cobblestone
# ===========================================================
"""
        super().__init__(header)

    def add_resource(
            self,
            resource_id: str,
            display: str,
            material: str,
            lore: Optional[List[str]] = None
    ) -> None:
        lines = [
            f"  {resource_id}:\n",
            f"    display: \"{display}\"\n",
            f"    material: {material}\n",
            f"    world: \"{self.world}\"\n",
            f"    region: \"{self.region}\"\n"
        ]
        if lore:
            lines.append(f"    lore:\n")
            for line in lore:
                lines.append(f"      - \"{line}\"\n")

        lines.append(f"\n")
        self.items.append("".join(lines))

class GearGenerator(AuroraGenerator):
    def __init__(self, world: str, region: str, gear_type: str) -> None:
        self.world = world
        self.region = region
        self.gear_type = gear_type
        header = """
# ===========================================================
# AURORA GEAR TEMPLATE
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
"""
        super().__init__(header)

    def add_gear(
            self,
            gear_id: str,
            display: str,
            material: str,
            tier: int = 0,
            enchants: Optional[Dict[str, int]] = None,
            lore: Optional[List[str]] = None
    ) -> None:
        lines = [
            f"  {gear_id}:\n",
            f"    display: \"{display}\"\n",
            f"    material: {material}\n"
        ]
        if tier > 0:
            lines.append(f"    tier: {tier}\n")
        lines.append(f"    type: \"{self.gear_type}\"\n")
        lines.append(f"    world: \"{self.world}\"\n")
        lines.append(f"    region: \"{self.region}\"\n")
        if enchants:
            lines.append(f"    enchants:\n")
            for enchant_id, level in enchants.items():
                lines.append(f"      {enchant_id}: {level}\n")

        if lore:
            lines.append(f"    lore:\n")
            for line in lore:
                lines.append(f"      - \"{line}\"\n")

        lines.append(f"\n")
        self.items.append("".join(lines))

class MobGenerator(AuroraGenerator):
    def __init__(self) -> None:
        header = """
# ===========================================================
# AURORA MOB TEMPLATE
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
"""
        super().__init__(header)

    def add_mob(
            self,
            mob_id: str,
            display: str,
            entity: str,
            equip: Optional[Dict[str, str]] = None,
            attributes: Optional[List[Dict[str, Union[str, float]]]] = None,
            independent: Optional[List[Dict]] = None,
            choices: Optional[Dict] = None
    ) -> None:
        lines = [
            f"  {mob_id}:\n",
            f"    display: \"{display}\"\n",
            f"    entity: {entity}\n"
        ]

        if equip:
            lines.append("    equip:\n")
            for slot, item in equip.items():
                lines.append(f"      {slot}: {item}\n")

        if attributes:
            lines.append("    attributes:\n")
            for attr in attributes:
                lines.append(f"      - attr: {attr['attr']}\n")
                lines.append(f"        value: {attr['value']}\n")

        if independent:
            lines.append("    independent:\n")
            for d in independent:
                lines.append(f"      - id: {d['id']}\n")
                if d.get('chance', 100) != 100: lines.append(f"        chance: {d['chance']}\n")
                if d.get('min', 1) != 1: lines.append(f"        min: {d['min']}\n")
                if d.get('max', 1) != 1: lines.append(f"        max: {d['max']}\n")

        if choices:
            lines.append(f"    choices:\n      chance: {choices.get('chance', 100)}\n      options:\n")
            for o in choices.get('options', []):
                lines.append(f"        - id: {o['id']}\n          weight: {o['weight']}\n")

        lines.append("\n")
        self.items.append("".join(lines))

class IconGenerator(AuroraGenerator):
    def __init__(self) -> None:
        header = """
# ===========================================================
# AURORA ICON TEMPLATE
# ===========================================================
# [icon_id]:
#   display: "<color><b><i>Display Name"
#   material: [BUKKIT_MATERIAL]
#   lore: (Optional)
#     - "<color><b><i>Lore Line"
#   glow: true (Enchanted)
# ===========================================================
"""
        super().__init__(header)

    def add_icon(
            self,
            icon_id: str,
            display: str,
            material: str,
            glow: bool = False,
            lore: Optional[List[str]] = None
    ) -> None:
        lines = [
            f"  {icon_id}:\n",
            f"    display: \"{display}\"\n",
            f"    material: {material}\n"
        ]

        lines.append(f"    glow: {str(glow).lower()}\n")

        if lore:
            lines.append("    lore:\n")
            for line in lore:
                lines.append(f"      - \"{line}\"\n")

        lines.append("\n")
        self.items.append("".join(lines))

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
                lines.append("        cost:\n")
                for c in data['cost']:
                    lines.append(f"          - id: {c['id']}\n")
                    if 'amount' in c:
                        lines.append(f"            amount: {c['amount']}\n")

        lines.append("\n")
        self.items.append("".join(lines))

class TraderGenerator(AuroraGenerator):
    def __init__(self) -> None:
        header = """
# ===========================================================
# AURORA TRADER TEMPLATE
# ===========================================================
# [trader_id]:
#   display: "<color><b><i>Display Name"
#   entity: [BUKKIT_ENTITY_TYPE]
#   gui: [gui_id]
# ===========================================================
"""
        super().__init__(header)

    def add_trader(self, trader_id: str, display: str, entity: str, gui_id: str) -> None:
        out = [
            f"  {trader_id}:\n",
            f"    display: \"{display}\"\n",
            f"    entity: {entity}\n",
            f"    gui: {gui_id}\n\n"
        ]
        self.items.append("".join(out))