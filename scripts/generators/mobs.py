from aurora_gen import AuroraGenerator
from typing import Dict, List, Union, Optional

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

def register_mobs():
    register_gormak()

def register_gormak():
    gen = MobGenerator()

    # Abandoned City
    gen.add_mob(
        mob_id="hollowed_serf",
        display="<gray>Hollowed Serf",
        entity="HUSK",
        attributes=[{"attr": "MAX_HEALTH", "value": 30.0}],
        equip={"head": "serf_helmet_1", "hand": "serf_sword_1"},
        independent=[
            {"id": "tattered_cloth", "min": 2, "max": 5},
            {"id": "ancient_scrap", "chance": 30.0, "max": 2}
        ]
    )

    gen.add_mob(
        mob_id="scavenger_husk",
        display="<gray><b>Scavenger Husk",
        entity="HUSK",
        attributes=[{"attr": "MAX_HEALTH", "value": 45.0}, {"attr": "MOVEMENT_SPEED", "value": 0.3}],
        equip={"head": "scavenger_helmet_1", "chest": "scavenger_chestplate_1", "hand": "scavenger_sword_1"},
        independent=[{"id": "ancient_scrap", "min": 3, "max": 6}]
    )

    # --- THE DUNES ---
    gen.add_mob(
        mob_id="dune_scout",
        display="<white><b>Dune Scout",
        entity="HUSK",
        attributes=[{"attr": "MAX_HEALTH", "value": 60.0}],
        equip={"head": "dune_scout_helmet_1", "hand": "dune_scout_sword_1"},
        independent=[{"id": "ancient_flesh", "max": 3}]
    )

    gen.add_mob(
        mob_id="sand_stalker",
        display="<gold><b>Sand Stalker",
        entity="SKELETON",
        attributes=[{"attr": "MAX_HEALTH", "value": 80.0}],
        equip={"head": "stalker_helmet_1", "hand": "stalker_sword_1"},
        independent=[{"id": "ancient_bone", "min": 5, "max": 11}]
    )

    # --- THE TEMPLE ---
    gen.add_mob(
        mob_id="tomb_guard",
        display="<gold><b>Tomb Guard",
        entity="HUSK",
        attributes=[{"attr": "MAX_HEALTH", "value": 120.0}, {"attr": "KNOCKBACK_RESISTANCE", "value": 1.0}],
        equip={
            "head": "tomb_guard_helmet_1", "chest": "tomb_guard_chestplate_1",
            "legs": "tomb_guard_leggings_1", "feet": "tomb_guard_boots_1", "hand": "tomb_guard_sword_1"
        },
        independent=[{"id": "ancient_nugget", "min": 3, "max": 7}]
    )

    gen.add_mob(
        mob_id="vizier_mage",
        display="<gold><b>Vizier Mage",
        entity="HUSK",
        attributes=[{"attr": "MAX_HEALTH", "value": 100.0}],
        equip={"head": "vizier_helmet_1", "hand": "vizier_sword_1"},
        independent=[{"id": "vizier_scroll", "chance": 25.0}]
    )

    # --- ROYAL TOMB ---
    gen.add_mob(
        mob_id="royal_myrmidon",
        display="<gold><b>Royal Myrmidon",
        entity="HUSK",
        attributes=[{"attr": "MAX_HEALTH", "value": 200.0}, {"attr": "ATTACK_DAMAGE", "value": 15.0}],
        equip={
            "head": "myrmidon_helmet_1", "chest": "myrmidon_chestplate_1",
            "legs": "myrmidon_leggings_1", "feet": "myrmidon_boots_1", "hand": "myrmidon_sword_1"
        },
        independent=[{"id": "eternal_soul", "chance": 35.0}]
    )

    gen.add_mob(
        mob_id="gormak_eternal",
        display="<gold><b>Gormak the Eternal",
        entity="HUSK",
        attributes=[
            {"attr": "MAX_HEALTH", "value": 1000.0},
            {"attr": "KNOCKBACK_RESISTANCE", "value": 1.0},
            {"attr": "FOLLOW_RANGE", "value": 100.0}
        ],
        equip={
            "head": "gormak_helmet_1", "chest": "gormak_chestplate_1",
            "legs": "gormak_leggings_1", "feet": "gormak_boots_1", "hand": "gormak_sword_1"
        },
        independent=[{"id": "ancient_heart", "chance": 100.0, "max": 2}]
    )

    gen.save("mobs/gormak.yml")

register_mobs()
