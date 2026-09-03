from generate import AuroraGenerator
from typing import Dict, List, Optional

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

def register_gear():
    register_picks()
    register_armor()
    register_weapons()

def register_picks():
    # OVERWORLD - ORIGIN_ISLES
    gen = GearGenerator("overworld", "origin_isles", "mining")

    picks_config = [
        {
            "id": "starter",
            "name": "<gray><b>Starter Pickaxe",
            "material": "WOODEN_PICKAXE",
            "tiers": [
                {}
            ]
        },
        {
            "id": "cobble",
            "name": "<gray><b>Cobble Pickaxe",
            "material": "STONE_PICKAXE",
            "tiers": [
                {},
                {"efficiency": 1},
                {"efficiency": 2},
                {"efficiency": 2, "fortune": 1},
                {"efficiency": 2, "fortune": 1, "luck": 1}
            ]
        },
        {
            "id": "coal",
            "name": "<dark_gray><b>Coal Pickaxe",
            "material": "STONE_PICKAXE",
            "tiers": [
                {"efficiency": 3, "fortune": 1, "luck": 1},
                {"efficiency": 3, "fortune": 1, "luck": 2},
                {"efficiency": 3, "fortune": 2, "luck": 2},
                {"efficiency": 4, "fortune": 2, "luck": 2},
                {"efficiency": 4, "fortune": 3, "luck": 2}
            ]
        },
        {
            "id": "copper",
            "name": "<#D94110><b>Copper Pickaxe",
            "material": "COPPER_PICKAXE",
            "tiers": [
                {"efficiency": 4, "fortune": 3, "luck": 2},
                {"efficiency": 5, "fortune": 3, "luck": 2},
                {"efficiency": 5, "fortune": 3, "luck": 3},
                {"efficiency": 5, "fortune": 4, "luck": 3},
                {"efficiency": 6, "fortune": 4, "luck": 3}
            ]
        },
        {
            "id": "iron",
            "name": "<white><b>Iron Pickaxe",
            "material": "IRON_PICKAXE",
            "tiers": [
                {"efficiency": 6, "fortune": 4, "luck": 3},
                {"efficiency": 6, "fortune": 4, "luck": 4},
                {"efficiency": 7, "fortune": 4, "luck": 4},
                {"efficiency": 7, "fortune": 5, "luck": 4},
                {"efficiency": 8, "fortune": 5, "luck": 4}
            ]
        },
        {
            "id": "gold",
            "name": "<gold><b>Gold Pickaxe",
            "material": "GOLDEN_PICKAXE",
            "tiers": [
                {"efficiency": 8, "fortune": 5, "luck": 4},
                {"efficiency": 8, "fortune": 5, "luck": 5},
                {"efficiency": 9, "fortune": 5, "luck": 5},
                {"efficiency": 10, "fortune": 5, "luck": 5},
                {"efficiency": 10, "fortune": 5, "luck": 5, "blast": 1}
            ]
        }
    ]

    for config in picks_config:
        for index, enchant_data in enumerate(config["tiers"]):
            tier_num = index + 1
            gen.add_gear(
                gear_id=f"{config['id']}_pick_{tier_num}",
                display=config["name"],
                material=config["material"],
                tier=tier_num,
                enchants=enchant_data if enchant_data else None
            )

    gen.save("gear/picks.yml")

def register_armor():
    armor_config = [
        {"id": "starter", "name": "<gray><b>Starter", "base": "LEATHER", "tiers": [{}]},
        {"id": "cobble", "name": "<gray><b>Cobble", "base": "LEATHER", "tiers": [{"protection": 1}, {"protection": 2}]},
        {"id": "coal", "name": "<dark_gray><b>Coal", "base": "LEATHER", "tiers": [{"protection": 3}, {"protection": 4}]},
        {"id": "copper", "name": "<#D94110><b>Copper", "base": "COPPER", "tiers": [{"protection": 4}, {"protection": 5}]},
        {"id": "iron", "name": "<white><b>Iron", "base": "IRON", "tiers": [{"protection": 2}, {"protection": 3}]},
        {"id": "gold", "name": "<gold><b>Gold", "base": "GOLDEN", "tiers": [{"protection": 4}, {"protection": 5}]},
        {"id": "serf", "name": "<gray><b>Serf", "base": "LEATHER", "tiers": [{"protection": 5, "health": 1}]},
        {"id": "scavenger", "name": "<gray><b>Scavenger", "base": "CHAINMAIL", "tiers": [{"protection": 6, "health": 1}]},
        {"id": "dune_scout", "name": "<white><b>Dune Scout", "base": "IRON", "tiers": [{"protection": 7, "health": 1}]},
        {"id": "stalker", "name": "<gold><b>Stalker", "base": "GOLDEN", "tiers": [{"protection": 7, "health": 2}]},
        {"id": "tomb_guard", "name": "<gold><b>Tomb Guard", "base": "GOLDEN", "tiers": [{"protection": 8, "health": 2}]},
        {"id": "vizier", "name": "<gold><b>Vizier", "base": "GOLDEN", "tiers": [{"protection": 9, "health": 2}]},
        {"id": "myrmidon", "name": "<gold><b>Myrmidon", "base": "GOLDEN", "tiers": [{"protection": 10, "health": 2}]},
        {"id": "gormak", "name": "<gold><b>Gormak the Eternal", "base": "GOLDEN", "tiers": [{"protection": 10, "health": 3}]}
    ]

    gen = GearGenerator("overworld", "origin_isles", "armor")

    for entry in armor_config:
        for index, enchants in enumerate(entry["tiers"]):
            tier_num = index + 1

            for piece in ["HELMET", "CHESTPLATE", "LEGGINGS", "BOOTS"]:
                gen.add_gear(
                    gear_id=f"{entry['id']}_{piece.lower()}_{tier_num}",
                    display=f"{entry['name']} {piece.capitalize()}",
                    material=f"{entry['base']}_{piece}",
                    tier=tier_num,
                    enchants=enchants if enchants else None
                )

    gen.save("gear/armor.yml")

def register_weapons():
    gen = GearGenerator("overworld", "origin_isles", "combat")

    mine_weapons = [
        {"id": "starter", "name": "<gray><b>Starter Sword", "material": "WOODEN_SWORD", "tiers": [{}]},
        {"id": "cobble", "name": "<gray><b>Cobble Sword", "material": "STONE_SWORD", "tiers": [{"sharpness": 1}, {"sharpness": 2}]},
        {"id": "coal", "name": "<dark_gray><b>Coal Sword", "material": "STONE_SWORD", "tiers": [{"sharpness": 2}, {"sharpness": 3}]},
        {"id": "copper", "name": "<#D94110><b>Copper Sword", "material": "COPPER_SWORD", "tiers": [{"sharpness": 3}, {"sharpness": 4}]},
        {"id": "iron", "name": "<white><b>Iron Sword", "material": "IRON_SWORD", "tiers": [{"sharpness": 4}, {"sharpness": 5}]},
        {"id": "gold", "name": "<gold><b>Gold Sword", "material": "GOLDEN_SWORD", "tiers": [{"sharpness": 5}, {"sharpness": 6}]},
        {"id": "crude_gormak", "name": "<gold><b>Crude Gormak Sword", "material": "GOLDEN_SWORD", "tiers": [{"sharpness": 6, "looting": 1}, {"sharpness": 7, "looting": 1}, {"sharpness": 8, "looting": 1}, {"sharpness": 9, "looting": 1}, {"sharpness": 10, "looting": 1}]},
        {"id": "refined_gormak", "name": "<gold><b>Refined Gormak Sword", "material": "GOLDEN_SWORD", "tiers": [{"sharpness": 10, "looting": 2}, {"sharpness": 11, "looting": 2}, {"sharpness": 12, "looting": 2}, {"sharpness": 13, "looting": 2}, {"sharpness": 13, "looting": 3}]},
        {"id": "gilded_gormak", "name": "<gold><b>Gilded Gormak Sword", "material": "GOLDEN_SWORD", "tiers": [{"sharpness": 14, "looting": 3}, {"sharpness": 15, "looting": 3}, {"sharpness": 16, "looting": 3}, {"sharpness": 17, "looting": 3}, {"sharpness": 18, "looting": 3}]},
        {"id": "eternal_gormak", "name": "<gold><b>Eternal Gormak Sword", "material": "GOLDEN_SWORD", "tiers": [{"sharpness": 18, "looting": 4}, {"sharpness": 19, "looting": 4}, {"sharpness": 20, "looting": 4}, {"sharpness": 20, "looting": 5}, {"sharpness": 20, "looting": 5, "adrenaline": 1}]},
        {"id": "serf", "name": "<gray><b>Serf Dagger", "material": "STONE_SWORD", "tiers": [{"sharpness": 6, "looting": 1}, {"sharpness": 7, "looting": 1}]},
        {"id": "scavenger", "name": "<gray><b>Scavenger Shiv", "material": "IRON_SWORD", "tiers": [{"sharpness": 8, "looting": 1}, {"sharpness": 9, "looting": 1}]},
        {"id": "dune_scout", "name": "<white><b>Dune Scout Machete", "material": "IRON_SWORD", "tiers": [{"sharpness": 10, "looting": 1}, {"sharpness": 11, "looting": 1}]},
        {"id": "stalker", "name": "<gold><b>Stalker Dagger", "material": "GOLDEN_SWORD", "tiers": [{"sharpness": 11, "looting": 2}, {"sharpness": 12, "looting": 2}]},
        {"id": "tomb_guard", "name": "<gold><b>Tomb Guard Halberd", "material": "GOLDEN_SWORD", "tiers": [{"sharpness": 13, "looting": 2}, {"sharpness": 14, "looting": 2}]},
        {"id": "vizier", "name": "<gold><b>Vizier Dagger", "material": "GOLDEN_SWORD", "tiers": [{"sharpness": 15, "looting": 2}, {"sharpness": 16, "looting": 2}]},
        {"id": "myrmidon", "name": "<gold><b>Myrmidon Blade", "material": "GOLDEN_SWORD", "tiers": [{"sharpness": 16, "looting": 3}, {"sharpness": 17, "looting": 3}]},
        {"id": "pharaoh", "name": "<gold><b>Pharaoh Khopesh", "material": "GOLDEN_SWORD", "tiers": [{"sharpness": 20, "looting": 1}]},
        {"id": "gormak", "name": "<gold><b>Gormak's Sun Blade", "material": "GOLDEN_SWORD", "tiers": [{"sharpness": 18, "looting": 3}, {"sharpness": 18, "looting": 4}]}
    ]

    for config in mine_weapons:
        for index, enchants in enumerate(config["tiers"]):
            tier_num = index + 1
            gen.add_gear(
                gear_id=f"{config['id']}_sword_{tier_num}",
                display=config['name'],
                material=config['material'],
                tier=tier_num,
                enchants=enchants
            )

    gen.save("gear/weapons.yml")

def register_mob_gear():
    return
