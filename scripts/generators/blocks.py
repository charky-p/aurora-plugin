from aurora_gen import AuroraGenerator
from typing import List, Optional, Dict, Union

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
        self.items.append("".join(lines))

def register_blocks():
    gen = BlockGenerator()
    gen.add_block("cobblestone", "COBBLESTONE", independent=[{"id": "cobblestone"}])
    gen.add_block("stone", "STONE", independent=[{"id": "cobblestone", "min": 2, "max": 4}])
    gen.add_block("coal_ore", "COAL_ORE", independent=[{"id": "coal"}])
    gen.add_block("coal_block", "COAL_BLOCK", independent=[{"id": "coal", "min": 5, "max": 9}])
    gen.add_block("copper_ore", "COPPER_ORE", density=1, independent=[{"id": "copper"}])
    gen.add_block("copper_block", "COPPER_BLOCK", density=1, independent=[{"id": "copper", "min": 5, "max": 9}])
    gen.add_block("iron_ore", "IRON_ORE", density=2, independent=[{"id": "iron"}])
    gen.add_block("iron_block", "IRON_BLOCK", density=2, independent=[{"id": "iron", "min": 5, "max": 9}])
    gen.add_block("gold_ore", "GOLD_ORE", density=3, independent=[{"id": "gold"}])
    gen.add_block("gold_block", "GOLD_BLOCK", density=3, independent=[{"id": "gold", "min": 5, "max": 9}])

    gen.save("blocks.yml")