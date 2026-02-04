from aurora_gen import *

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

gen = ResourceGenerator("overworld", "origin_isles")
gen.add_resource("cobblestone", "<gray>Cobblestone", "COBBLESTONE")
gen.add_resource("coal", "<gray>Coal", "COAL")
gen.add_resource("copper", "<gray><b>Copper", "COPPER_INGOT")
gen.add_resource("iron", "<gray><b>Iron", "IRON_INGOT")
gen.add_resource("gold", "<gray><b>Gold", "GOLD_INGOT")

gen.add_resource("ancient_flesh", "<yellow><b>Ancient Flesh", "ROTTEN_FLESH")
gen.add_resource("ancient_bone", "<yellow><b>Ancient Bone", "BONE")
gen.add_resource("ancient_nugget", "<yellow><b>Ancient Nugget", "GOLD_NUGGET")

gen.save("resources.yml")

gen = GearGenerator("overworld", "origin_isles", "armor")
gen.add_gear(
    "starter_helmet",
    "<gray><b>Starter Helmet",
    "LEATHER_HELMET",
    tier=1,
    lore=["<gray>A simple helmet crafted for", "<gray>new travellers of Aurora"]
)

gen.save("gear/armor.yml")

# OVERWORLD - ORIGIN_ISLES GEAR
gen = GearGenerator("overworld", "origin_isles", "mining")

# --- STARTER GEAR ---
gen.add_gear("starter_pick", "<gray><b>Starter Pickaxe", "WOODEN_PICKAXE")

# --- COBBLE PICKS (Tiers 1-5) ---
gen.add_gear("cobble_pick_1", "<gray><b>Cobble Pickaxe", "STONE_PICKAXE", tier=1)
gen.add_gear("cobble_pick_2", "<gray><b>Cobble Pickaxe", "STONE_PICKAXE", tier=2, enchants={"efficiency": 1})
gen.add_gear("cobble_pick_3", "<gray><b>Cobble Pickaxe", "STONE_PICKAXE", tier=3, enchants={"efficiency": 2})
gen.add_gear("cobble_pick_4", "<gray><b>Cobble Pickaxe", "STONE_PICKAXE", tier=4, enchants={"efficiency": 2, "fortune": 1})
gen.add_gear("cobble_pick_5", "<gray><b>Cobble Pickaxe", "STONE_PICKAXE", tier=5, enchants={"efficiency": 2, "fortune": 1, "luck": 1})

# --- COAL PICKS (Tiers 1-5) ---
gen.add_gear("coal_pick_1", "<dark_gray><b>Coal Pickaxe", "STONE_PICKAXE", tier=1, enchants={"efficiency": 3, "fortune": 1, "luck": 1})
gen.add_gear("coal_pick_2", "<dark_gray><b>Coal Pickaxe", "STONE_PICKAXE", tier=2, enchants={"efficiency": 3, "fortune": 1, "luck": 2})
gen.add_gear("coal_pick_3", "<dark_gray><b>Coal Pickaxe", "STONE_PICKAXE", tier=3, enchants={"efficiency": 3, "fortune": 2, "luck": 2})
gen.add_gear("coal_pick_4", "<dark_gray><b>Coal Pickaxe", "STONE_PICKAXE", tier=4, enchants={"efficiency": 4, "fortune": 2, "luck": 2})
gen.add_gear("coal_pick_5", "<dark_gray><b>Coal Pickaxe", "STONE_PICKAXE", tier=5, enchants={"efficiency": 4, "fortune": 3, "luck": 2})

# --- COPPER PICKS (Tiers 1-5) ---
gen.add_gear("copper_pick_1", "<#D94110><b>Copper Pickaxe", "COPPER_PICKAXE", tier=1, enchants={"efficiency": 4, "fortune": 3, "luck": 2})
gen.add_gear("copper_pick_2", "<#D94110><b>Copper Pickaxe", "COPPER_PICKAXE", tier=2, enchants={"efficiency": 5, "fortune": 3, "luck": 2})
gen.add_gear("copper_pick_3", "<#D94110><b>Copper Pickaxe", "COPPER_PICKAXE", tier=3, enchants={"efficiency": 5, "fortune": 3, "luck": 3})
gen.add_gear("copper_pick_4", "<#D94110><b>Copper Pickaxe", "COPPER_PICKAXE", tier=4, enchants={"efficiency": 5, "fortune": 4, "luck": 3})
gen.add_gear("copper_pick_5", "<#D94110><b>Copper Pickaxe", "COPPER_PICKAXE", tier=5, enchants={"efficiency": 6, "fortune": 4, "luck": 3})

# --- IRON PICKS (Tiers 1-5) ---
gen.add_gear("iron_pick_1", "<white><b>Iron Pickaxe", "IRON_PICKAXE", tier=1, enchants={"efficiency": 6, "fortune": 4, "luck": 3})
gen.add_gear("iron_pick_2", "<white><b>Iron Pickaxe", "IRON_PICKAXE", tier=2, enchants={"efficiency": 6, "fortune": 4, "luck": 4})
gen.add_gear("iron_pick_3", "<white><b>Iron Pickaxe", "IRON_PICKAXE", tier=3, enchants={"efficiency": 7, "fortune": 4, "luck": 4})
gen.add_gear("iron_pick_4", "<white><b>Iron Pickaxe", "IRON_PICKAXE", tier=4, enchants={"efficiency": 7, "fortune": 5, "luck": 4})
gen.add_gear("iron_pick_5", "<white><b>Iron Pickaxe", "IRON_PICKAXE", tier=5, enchants={"efficiency": 8, "fortune": 5, "luck": 4})

# --- GOLD PICKS (Tiers 1-5) ---
gen.add_gear("gold_pick_1", "<gold><b>Gold Pickaxe", "GOLDEN_PICKAXE", tier=1, enchants={"efficiency": 8, "fortune": 5, "luck": 4})
gen.add_gear("gold_pick_2", "<gold><b>Gold Pickaxe", "GOLDEN_PICKAXE", tier=2, enchants={"efficiency": 8, "fortune": 5, "luck": 5})
gen.add_gear("gold_pick_3", "<gold><b>Gold Pickaxe", "GOLDEN_PICKAXE", tier=3, enchants={"efficiency": 9, "fortune": 5, "luck": 5})
gen.add_gear("gold_pick_4", "<gold><b>Gold Pickaxe", "GOLDEN_PICKAXE", tier=4, enchants={"efficiency": 10, "fortune": 5, "luck": 5})
gen.add_gear("gold_pick_5", "<gold><b>Gold Pickaxe", "GOLDEN_PICKAXE", tier=5, enchants={"efficiency": 10, "fortune": 5, "luck": 5, "blast": 1})

gen.save("gear/picks.yml")

gen = GearGenerator("overworld", "origin_isles", "combat")
gen.add_gear(gear_id="starter_sword", display="<gray><b>Starter Sword", material="WOODEN_SWORD")

# Gormak Swords - T1-6
gen.add_gear("gormak_sword_1", "<yellow><b>Gormak Sword", "IRON_SWORD", tier=1)
gen.add_gear("gormak_sword_2", "<yellow><b>Gormak Sword", "IRON_SWORD", tier=2, enchants={"sharpness": 1})
gen.add_gear("gormak_sword_3", "<yellow><b>Gormak Sword", "IRON_SWORD", tier=3, enchants={"sharpness": 2})
gen.add_gear("gormak_sword_4", "<yellow><b>Gormak Sword", "IRON_SWORD", tier=4, enchants={"sharpness": 3})
gen.add_gear("gormak_sword_5", "<yellow><b>Gormak Sword", "IRON_SWORD", tier=5, enchants={"sharpness": 4})
gen.add_gear("gormak_sword_6", "<yellow><b>Gormak Sword", "IRON_SWORD", tier=6, enchants={"sharpness": 5})

gen.save("gear/weapons.yml")

gen = GearGenerator(world="overworld", region="origin_isles", gear_type="armor")

# --- ARMOR ITEMS ---
gen.add_gear("leather_helmet", "<white><b>Leather Helmet", "LEATHER_HELMET")
gen.add_gear("iron_helmet", "<white><b>Iron Helmet", "IRON_HELMET")
gen.add_gear("iron_chestplate", "<white><b>Iron Chestplate", "IRON_CHESTPLATE")
gen.add_gear("gold_chestplate", "<white><b>Gold Chestplate", "GOLDEN_CHESTPLATE")
gen.add_gear("iron_leggings", "<white><b>Iron Leggings", "IRON_LEGGINGS")
gen.add_gear("leather_leggings", "<white><b>Leather Leggings", "LEATHER_LEGGINGS")
gen.add_gear("leather_boots", "<white><b>Leather Boots", "LEATHER_BOOTS")

# --- COMBAT ITEMS ---
gen.gear_type = "combat"
gen.add_gear("wooden_sword", "<white><b>Wooden Sword", "WOODEN_SWORD")
gen.add_gear("iron_sword", "<white><b>Iron Sword", "IRON_SWORD")
gen.add_gear("bow", "<white><b>Bow", "BOW")
gen.add_gear("shield", "<white><b>Shield", "SHIELD")

# --- GORMAK ITEMS ---
gen.add_gear(
    gear_id="gormak_sword",
    display="<aqua><b>Gormak Sword",
    material="DIAMOND_SWORD",
    tier=1,
    enchants={"sharpness": 30, "pierce": 5},
    lore=[
        "<yellow>Forged by Gormak's descendants",
        "<yellow>Aura emits from the sword n dat"
    ]
)

gen.gear_type = "armor"
gen.add_gear("gormak_chest", "<brown><b>Gormak Chestplate", "DIAMOND_CHESTPLATE")
gen.add_gear("totem_of_undying", "<yellow><b>Totem of Undying", "TOTEM_OF_UNDYING")

gen.save("gear/other.yml")

gen = MobGenerator()
gen.add_mob(
    "gormak_minion", "<dark_gray><b>Gormak Minion", "HUSK",
    equip={"head": "leather_helmet", "chest": "iron_chestplate", "legs": "leather_leggings", "feet": "leather_boots", "hand": "wooden_sword"},
    attributes=[{"attr": "MAX_HEALTH", "value": 30.0}],
    independent=[{"id": "ancient_flesh", "min": 1, "max": 6}]
)

gen.add_mob(
    "gormak_bowman", "<gray><b>Gormak Bowman", "SKELETON",
    equip={"head": "iron_helmet", "chest": "gold_chestplate", "hand": "bow"},
    attributes=[{"attr": "MAX_HEALTH", "value": 35.0}],
    independent=[{"id": "ancient_bone", "min": 1, "max": 4}]
)

gen.add_mob(
    "gormak_guard", "<yellow><b>Gormak Guard", "HUSK",
    equip={"head": "iron_helmet", "chest": "gold_chestplate", "legs": "iron_leggings", "feet": "iron_boots", "hand": "iron_sword", "off_hand": "shield"},
    attributes=[{"attr": "MAX_HEALTH", "value": 60.0}],
    independent=[{"id": "gormak_sword", "chance": 1}, {"id": "ancient_nugget", "min": 5, "max": 13}]
)

gen.add_mob(
    "void_gormak_shaman", "<black><b>Void-Infused Shaman", "WITHER_SKELETON",
    equip={"chest": "gormak_chest"},
    attributes=[
        {"attr": "MAX_HEALTH", "value": 100.0},
        {"attr": "MOVEMENT_SPEED", "value": 0.25},
        {"attr": "KNOCKBACK_RESISTANCE", "value": 1.0},
        {"attr": "FOLLOW_RANGE", "value": 48.0},
        {"attr": "ATTACK_DAMAGE", "value": 8.0}
    ],
    independent=[{"id": "coal"}, {"id": "gold", "min": 5, "max": 12}]
)

gen.save("mobs.yml")

gen = IconGenerator()
# --- COBBLESTONE ICONS ---
gen.add_icon("cobble_tools", "<gray><b>Cobblestone Tools", "STONE_PICKAXE", glow=True)
gen.add_icon("cobble_armor", "<gray><b>Cobblestone Armor", "LEATHER_CHESTPLATE", glow=True)
gen.add_icon("cobble_compress", "<gray><b>Cobblestone Compressor", "COBBLESTONE", glow=True)

# --- COAL ICONS ---
gen.add_icon("coal_tools", "<gray><b>Coal Tools", "STONE_PICKAXE", glow=True)
gen.add_icon("coal_compress", "<gray><b>Coal Compressor", "COAL", glow=True)

# --- COPPER ICONS ---
gen.add_icon("copper_tools", "<#D94110><b>Copper Tools", "COPPER_PICKAXE", glow=True)
gen.add_icon("copper_compress", "<gray><b>Copper Compressor", "COPPER_INGOT", glow=True)

# --- IRON ICONS ---
gen.add_icon("iron_tools", "<white><b>Iron Tools", "IRON_PICKAXE", glow=True)
gen.add_icon("iron_compress", "<gray><b>Iron Compressor", "IRON_INGOT", glow=True)

# --- GOLD ICONS ---
gen.add_icon("gold_tools", "<gold><b>Gold Tools", "GOLDEN_PICKAXE", glow=True)
gen.add_icon("gold_compress", "<gray><b>Gold Compressor", "GOLD_INGOT", glow=True)

gen.save("icons.yml")

gen = GuiGenerator()
# --- SHOPS ---
materials = ["coal", "copper", "iron", "gold", "gormak"]

for material in materials:
    display_name = f"{material.capitalize()} Shop"
    tools_id = f"{material}_tools"
    compress_id = f"{material}_compress" if material != "gormak" else "gold_compress"

    gen.add_gui(
        f"{material}_shop",
        f"<gray><b>{display_name}",
        ["BBBBIBBBX", "BB1BBB3BB", "BBBBBBBBB"],
        {
            "1": {"type": "GUI", "id": tools_id},
            "3": {"type": "GUI", "id": compress_id}
        }
    )

gen.save("guis/shops.yml")

# TODO: gui for armor, tools, compressors after backpack

gen = TraderGenerator()

gen.add_trader("cobble_trader", "<gray><b>Cobble Trader", "VILLAGER", "cobble_shop")
gen.add_trader("coal_trader", "<gray><b>Coal Trader", "VILLAGER", "coal_shop")
gen.add_trader("copper_trader", "<gray><b>Copper Trader", "VILLAGER", "copper_shop")
gen.add_trader("iron_trader", "<gray><b>Iron Trader", "VILLAGER", "iron_shop")
gen.add_trader("gold_trader", "<gray><b>Gold Trader", "VILLAGER", "gold_shop")
gen.add_trader("gormak_trader", "<yellow><b>Gormak Trader", "VILLAGER", "gormak_shop")

gen.save("traders.yml")