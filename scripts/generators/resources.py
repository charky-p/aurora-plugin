from generate import AuroraGenerator
from typing import List, Optional

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

def register_resources():
    gen = ResourceGenerator("overworld", "origin_isles")
    gen.add_resource("cobblestone", "<gray><b>Cobblestone", "COBBLESTONE")
    gen.add_resource("coal", "<gray><b>Coal", "COAL")
    gen.add_resource("copper", "<gray><b>Copper", "COPPER_INGOT")
    gen.add_resource("iron", "<gray><b>Iron", "IRON_INGOT")
    gen.add_resource("gold", "<gray><b>Gold", "GOLD_INGOT")

    # --- ABANDONED CITY (Serf / Scavenger) ---
    gen.add_resource("tattered_cloth", "<gray><b>Tattered Cloth", "PAPER",
                     lore=["<gray>Old bandages from a fallen serf."])
    gen.add_resource("ancient_scrap", "<white><b>Ancient Scrap", "IRON_NUGGET",
                     lore=["<gray>Brittle metal used for basic shivs."])

    # --- THE DUNES (Scout / Stalker) ---
    gen.add_resource("ancient_flesh", "<#964B00><b>Ancient Flesh", "ROTTEN_FLESH",
                     lore=["<gray>A pulsating heart of desert magic."])
    gen.add_resource("ancient_bone", "<white><b>Ancient Bone", "BONE",
                     lore=["<gray>Hardened by centuries in the heat."])

    # --- THE TEMPLE (Guard / Vizier) ---
    gen.add_resource("ancient_nugget", "<gold><b>Ancient Nugget", "GOLD_NUGGET",
                     lore=["<gray>Dense stone from the Vizier's temple."])
    gen.add_resource("vizier_scroll", "<gold><b>Vizier Scroll", "FLOW_BANNER_PATTERN",
                     lore=["<gray>The mark of the high priests."])

    # --- ROYAL TOMB (Myrmidon / Gormak) ---
    gen.add_resource("eternal_soul", "<gold><b>Eternal Soul", "SOUL_SAND",
                     lore=["<gray>A lost soul from the inner tomb."])
    gen.add_resource("ancient_heart", "<gold><b>Sand-Tainted Heart", "YELLOW_DYE",
                     lore=["<gray>A fragment of Gormak."])

    gen.save("resources.yml")
