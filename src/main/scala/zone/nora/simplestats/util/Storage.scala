package zone.nora.simplestats.util

import scala.language.postfixOps

/**
 * data storage object. if the hypixel api were better this wouldn't need to exist :)
 */
object Storage {
  final val colourNameToCode = Map(
    "black" -> "\u00a70",
    "dark_green" -> "\u00a72",
    "dark_aqua" -> "\u00a73",
    "dark_red" -> "\u00a74",
    "dark_purple" -> "\u00a75",
    "gold" -> "\u00a76",
    "gray" -> "\u00a77",
    "dark_gray" -> "\u00a78",
    "blue" -> "\u00a79",
    "green" -> "\u00a7a",
    "aqua" -> "\u00a7b",
    "red" -> "\u00a7c",
    "light_purple" -> "\u00a7d",
    "yellow" -> "\u00a7e",
    "white" -> "\u00a7f"
  )

  final val skillLevels = Map(
    1 -> 50,
    2 -> 125,
    3 -> 200,
    4 -> 300,
    5 -> 500,
    6 -> 750,
    7 -> 1000,
    8 -> 1500,
    9 -> 2000,
    10 -> 3500,
    11 -> 5000,
    12 -> 7500,
    13 -> 10000,
    14 -> 15000,
    15 -> 20000,
    16 -> 30000,
    17 -> 50000,
    18 -> 75000,
    19 -> 100000,
    20 -> 200000,
    21 -> 300000,
    22 -> 400000,
    23 -> 500000,
    24 -> 600000,
    25 -> 700000,
    26 -> 800000,
    27 -> 900000,
    28 -> 1000000,
    29 -> 1100000,
    30 -> 1200000,
    31 -> 1300000,
    32 -> 1400000,
    33 -> 1500000,
    34 -> 1600000,
    35 -> 1700000,
    36 -> 1800000,
    37 -> 1900000,
    38 -> 2000000,
    39 -> 2100000,
    40 -> 2200000,
    41 -> 2300000,
    42 -> 2400000,
    43 -> 2500000,
    44 -> 2600000,
    45 -> 2750000,
    46 -> 2900000,
    47 -> 3100000,
    48 -> 3400000,
    49 -> 3700000,
    50 -> 4000000
  )

  final val runecraftingLevels = Map(
    1 -> 50,
    2 -> 100,
    3 -> 125,
    4 -> 160,
    5 -> 200,
    6 -> 250,
    7 -> 315,
    8 -> 400,
    9 -> 500,
    10 -> 625,
    11 -> 785,
    12 -> 1000,
    13 -> 1250,
    14 -> 1600,
    15 -> 2000,
    16 -> 2465,
    17 -> 3125,
    18 -> 4000,
    19 -> 5000,
    20 -> 6200,
    21 -> 7800,
    22 -> 9800,
    23 -> 12200,
    24 -> 15300,
    25 -> 19050
  )

  final val weapons = (
    "REVENANT_SWORD"
      :: "FANCY_SWORD"
      :: "HURRICANE_BOW"
      :: "UNDEAD_SWORD"
      :: "INK_WAND"
      :: "SILK_EDGE_SWORD"
      :: "SWEET_AXE"
      :: "SHAMAN_SWORD"
      :: "YETI_SWORD"
      :: "UNDEAD_BOW"
      :: "WOOD_SWORD"
      :: "RUNAANS_BOW"
      :: "END_STONE_BOW"
      :: "BOW"
      :: "SCULPTORS_AXE"
      :: "GOLEM_SWORD"
      :: "PET_ITEM_FORAGING_SKILL_BOOST_COMMON"
      :: "PRISMARINE_BOW"
      :: "FLAMING_SWORD"
      :: "ROGUE_SWORD"
      :: "PET_ITEM_MINING_SKILL_BOOST_RARE"
      :: "IRON_AXE"
      :: "WITHER_BOW"
      :: "EMBER_ROD"
      :: "TREECAPITATOR_AXE"
      :: "ENDER_BOW"
      :: "MOSQUITO_BOW"
      :: "CLEAVER"
      :: "SUPER_CLEAVER"
      :: "DECENT_BOW"
      :: "EMERALD_BLADE"
      :: "PET_ITEM_COMBAT_SKILL_BOOST_UNCOMMON"
      :: "MIDAS_SWORD"
      :: "BONZO_STAFF"
      :: "END_SWORD"
      :: "ZOMBIE_SWORD"
      :: "ROOKIE_AXE"
      :: "JUNGLE_AXE"
      :: "MAGMA_BOW"
      :: "DIAMOND_AXE"
      :: "PET_ITEM_COMBAT_SKILL_BOOST_COMMON"
      :: "PET_ITEM_FORAGING_SKILL_BOOST_EPIC"
      :: "EXPLOSIVE_BOW"
      :: "WOOD_AXE"
      :: "ASPECT_OF_THE_END"
      :: "FROZEN_SCYTHE"
      :: "REAPER_SCYTHE"
      :: "PRISMARINE_BLADE"
      :: "TACTICIAN_SWORD"
      :: "GOLD_SWORD"
      :: "POOCH_SWORD"
      :: "SPIDER_SWORD"
      :: "PET_ITEM_MINING_SKILL_BOOST_COMMON"
      :: "END_STONE_SWORD"
      :: "SLIME_BOW"
      :: "ORNATE_ZOMBIE_SWORD"
      :: "EFFICIENT_AXE"
      :: "SILVER_FANG"
      :: "PET_ITEM_COMBAT_SKILL_BOOST_RARE"
      :: "SCORPION_BOW"
      :: "IRON_SWORD"
      :: "SAVANA_BOW"
      :: "PROMISING_AXE"
      :: "PIGMAN_SWORD"
      :: "PET_ITEM_COMBAT_SKILL_BOOST_EPIC"
      :: "GOLD_AXE"
      :: "STONE_BLADE"
      :: "ASPECT_OF_THE_DRAGON"
      :: "RECLUSE_FANG"
      :: "SCORPION_FOIL"
      :: "LEAPING_SWORD"
      :: "EDIBLE_MACE"
      :: "DIAMOND_SWORD"
      :: "HUNTER_KNIFE"
      :: "REAPER_SWORD"
      :: "STONE_SWORD"
      :: "STONE_AXE"
      :: "RAIDER_AXE"
      :: Nil
    )
}
