from generate import AuroraGenerator
from typing import Optional

class EnchantGenerator(AuroraGenerator):
    def __init__(self) -> None:
        header = """
# ===========================================================
# AURORA ENCHANT TEMPLATE
# ===========================================================
# [enchant_id]:
#   display: "<color><b><i>Example Enchant"
#   enchant: [BUKKIT_ENCHANTMENT] (Optional, null implies custom enchant)
# ===========================================================
"""
        super().__init__(header)

    def add_enchant(
            self,
            enchant_id: str,
            display: str,
            enchant: Optional[str] = None
    ) -> None:
        lines = [f"  {enchant_id}:\n", f"    display: \"{display}\"\n"]

        if enchant is not None:
            lines.append(f"    enchant: {enchant}\n")

        lines.append("\n")
        self.items.append("".join(lines))

def register_enchants():
    gen = EnchantGenerator()

    gen.add_enchant("efficiency", "<gray>Efficiency", "EFFICIENCY")
    gen.add_enchant("fortune", "<gray>Fortune", "FORTUNE")
    gen.add_enchant("luck", "<gray>Luck")
    gen.add_enchant("blast", "<gray>Blast")
    gen.add_enchant("sharpness", "<gray>Sharpness", "SHARPNESS")
    gen.add_enchant("looting", "<gray>Looting", "LOOTING")
    gen.add_enchant("pierce", "<gray>Pierce")
    gen.add_enchant("protection", "<gray>Protection")
    gen.add_enchant("health", "<gray>Health")
    gen.add_enchant("adrenaline", "<red>Adrenaline")

    gen.save("enchants.yml")