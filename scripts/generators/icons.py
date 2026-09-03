from generate import AuroraGenerator
from typing import List, Optional

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

def register_icons():
    gen = IconGenerator()

    # --- Cobble to Gold ---
    mine_sets = [
        ("cobble", "<gray><b>Cobblestone", "STONE", "LEATHER"),
        ("coal", "<dark_gray><b>Coal", "STONE", "LEATHER"),
        ("copper", "<#D94110><b>Copper", "COPPER", "COPPER"),
        ("iron", "<white><b>Iron", "IRON", "IRON"),
        ("gold", "<gold><b>Gold", "GOLDEN", "GOLDEN")
    ]

    for mid, display, material1, material2 in mine_sets:
        gen.add_icon(f"{mid}_picks", f"{display} Pickaxe", f"{material1}_PICKAXE", glow=True)
        gen.add_icon(f"{mid}_gear", f"{display} Gear", f"{material2}_CHESTPLATE", glow=True)

    # --- GORMAK City to Tomb ---
    poi_sets = [
        ("city", "<gray><b>Abandoned City", "CHAINMAIL"),
        ("dunes", "<white><b>The Shifting Dunes", "IRON"),
        ("temple", "<gold><b>Ancient Temple", "GOLDEN"),
        ("tomb", "<#FFD700><b>Royal Tomb", "GOLDEN")
    ]

    for pid, display, material in poi_sets:
        gen.add_icon(f"gormak_{pid}_picks", f"{display} Picks", "GOLDEN_PICKAXE", glow=True)
        gen.add_icon(f"gormak_{pid}_gear", f"{display} Gear", f"{material}_CHESTPLATE", glow=True)

    gen.save("icons.yml")