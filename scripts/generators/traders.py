from generate import AuroraGenerator

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
        lines = [
            f"  {trader_id}:\n",
            f"    display: \"{display}\"\n",
            f"    entity: {entity}\n",
            f"    gui: {gui_id}\n",
            "\n"
        ]
        self.items.append("".join(lines))

def register_traders():
    gen = TraderGenerator()

    gen.add_trader("starter_trader", "<gray><b>Starter Trader", "VILLAGER", "starter_shop")
    gen.add_trader("cobble_trader", "<gray><b>Cobble Trader", "VILLAGER", "cobble_shop")
    gen.add_trader("coal_trader", "<gray><b>Coal Trader", "VILLAGER", "coal_shop")
    gen.add_trader("copper_trader", "<gray><b>Copper Trader", "VILLAGER", "copper_shop")
    gen.add_trader("iron_trader", "<gray><b>Iron Trader", "VILLAGER", "iron_shop")
    gen.add_trader("gold_trader", "<gray><b>Gold Trader", "VILLAGER", "gold_shop")
    gen.add_trader("gormak_city_trader", "<yellow><b>Abandoned City Trader", "VILLAGER", "gormak_city_shop")
    gen.add_trader("gormak_dunes_trader", "<yellow><b>Dunes Trader", "VILLAGER", "gormak_dunes_shop")
    gen.add_trader("gormak_temple_trader", "<yellow><b>Temple Trader", "VILLAGER", "gormak_temple_shop")
    gen.add_trader("gormak_tomb_trader", "<yellow><b>Gormak Tomb Trader", "VILLAGER", "gormak_tomb_shop")

    gen.save("traders.yml")