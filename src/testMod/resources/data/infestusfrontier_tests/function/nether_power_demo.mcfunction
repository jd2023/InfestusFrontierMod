# Shared working Nether workshop for demo.sh, capture and the engine regression.
fill 16 78 0 47 89 31 polished_blackstone_bricks
fill 17 80 1 46 88 30 air
fill 18 79 10 44 79 27 infestusfrontier:living_substrate[stage=mature]
setblock 20 88 5 shroomlight
setblock 20 88 13 shroomlight
setblock 20 88 21 shroomlight
setblock 20 88 28 shroomlight
setblock 28 88 5 shroomlight
setblock 28 88 13 shroomlight
setblock 28 88 21 shroomlight
setblock 28 88 28 shroomlight
setblock 36 88 5 shroomlight
setblock 36 88 13 shroomlight
setblock 36 88 21 shroomlight
setblock 36 88 28 shroomlight
setblock 44 88 5 shroomlight
setblock 44 88 13 shroomlight
setblock 44 88 21 shroomlight
setblock 44 88 28 shroomlight
setblock 20 79 12 shroomlight
setblock 20 79 20 shroomlight
setblock 20 79 27 shroomlight
setblock 28 79 12 shroomlight
setblock 28 79 20 shroomlight
setblock 28 79 27 shroomlight
setblock 36 79 12 shroomlight
setblock 36 79 20 shroomlight
setblock 36 79 27 shroomlight
setblock 44 79 12 shroomlight
setblock 44 79 20 shroomlight
setblock 44 79 27 shroomlight
setblock 22 80 16 infestusfrontier:thermal_heart{power:{heat:0,water:0,steam:0,charge:0,cycles:0},items:[{id:"infestusfrontier:heat_capsule",count:1},{},{},{}],discharge:0b}
setblock 23 80 16 infestusfrontier:heat_collector[facing=west]
setblock 23 79 16 lava
setblock 22 80 13 oak_sign[rotation=8]{is_waxed:1b,front_text:{messages:['"1. HEAT BODY"','"Heart + Collector"','"Lava below fin"','"Open heart to see"']}}
setblock 31 80 16 infestusfrontier:thermal_heart{power:{heat:0,water:16000,steam:0,charge:0,cycles:0},items:[{},{},{},{}],discharge:0b}
setblock 32 80 16 infestusfrontier:heat_collector[facing=west]
setblock 32 79 16 lava
setblock 30 80 16 infestusfrontier:boiler_chamber[facing=east]
setblock 31 80 17 infestusfrontier:pressure_outlet[facing=north]
setblock 31 80 18 infestusfrontier:vascular_junction[north=intake,down=outlet]
fill 31 79 18 31 79 21 infestusfrontier:fluid_vein[sealed=true]
setblock 31 80 21 infestusfrontier:steam_driver
setblock 30 80 21 infestusfrontier:fracture_jaw[facing=north]{tank:{stored:1000,capacity:2000},cuts:0,ticks:0}
setblock 30 80 22 infestusfrontier:work_bed
setblock 30 80 20 diamond_ore
setblock 31 80 13 oak_sign[rotation=8]{is_waxed:1b,front_text:{messages:['"2. STEAM BODY"','"Add Boiler + Water"','"Outlet pumps steam"','"to jaw behind it"']}}
setblock 29 80 24 oak_sign[rotation=8]{is_waxed:1b,front_text:{messages:['"STEAM MINING"','"Diamond in Work Bed"','"Replace ore ahead"','"to watch next cut"']}}
setblock 40 80 16 infestusfrontier:thermal_heart{power:{heat:0,water:16000,steam:0,charge:0,cycles:0},items:[{},{id:"infestusfrontier:charge_cell",count:1},{},{}],discharge:0b}
setblock 41 80 16 infestusfrontier:heat_collector[facing=west]
setblock 41 79 16 lava
setblock 39 80 16 infestusfrontier:boiler_chamber[facing=east]
setblock 40 80 15 infestusfrontier:generating_chamber[facing=south]
setblock 40 80 13 oak_sign[rotation=8]{is_waxed:1b,front_text:{messages:['"3. ELECTRIC BODY"','"Add Generator"','"Open heart: Charge"','"Cells carry power"']}}
setblock 35 80 9 oak_sign[rotation=8]{is_waxed:1b,front_text:{messages:['"ONE LIVING BODY"','"Parts face the core"','"Remove a chamber"','"to stop that stage"']}}
setblock 22 80 23 infestusfrontier:vigil_nest{tank:{stored:16000,capacity:16000},mask:16,paid:0,enabled:0b,egg:1b}
setblock 21 80 23 infestusfrontier:vigil_feeder
setblock 23 80 23 infestusfrontier:vigil_feeder
setblock 22 80 22 infestusfrontier:vigil_feeder
setblock 22 80 24 infestusfrontier:vigil_feeder
setblock 22 80 26 oak_sign[rotation=8]{is_waxed:1b,front_text:{messages:['"VIGIL NEST"','"Egg and biomass in"','"Open: select chunks"','"then Wake"']}}
setblock 36 80 25 chest{Items:[{Slot:0b,id:"infestusfrontier:thermal_heart",count:1},{Slot:1b,id:"infestusfrontier:heat_collector",count:1},{Slot:2b,id:"infestusfrontier:boiler_chamber",count:1},{Slot:3b,id:"infestusfrontier:generating_chamber",count:1},{Slot:4b,id:"infestusfrontier:pressure_outlet",count:1},{Slot:5b,id:"infestusfrontier:heat_capsule",count:1},{Slot:6b,id:"infestusfrontier:charge_cell",count:1},{Slot:7b,id:"infestusfrontier:vigil_egg",count:1},{Slot:8b,id:"minecraft:water_bucket",count:1},{Slot:9b,id:"infestusfrontier:biomass_bucket",count:1},{Slot:10b,id:"minecraft:diamond_ore",count:16}]}
setblock 36 80 26 oak_sign[rotation=8]{is_waxed:1b,front_text:{messages:['"SUPPLIES"','"Boilers start full"','"Refill outer faces"','"with water buckets"']}}
fill 26 79 4 29 83 4 obsidian
fill 27 80 4 28 82 4 nether_portal[axis=x]
setblock 25 80 6 oak_sign[rotation=8]{is_waxed:1b,front_text:{messages:['"OVERWORLD"','"Step into portal"','"Carry hot capsules"','"and charged cells"']}}
execute in minecraft:overworld run fill 220 -61 22 228 -61 29 polished_blackstone_bricks
execute in minecraft:overworld run fill 222 -61 24 225 -57 24 obsidian
execute in minecraft:overworld run fill 223 -60 24 224 -58 24 nether_portal[axis=x]
execute in minecraft:overworld run setblock 221 -60 23 oak_sign[rotation=8]{is_waxed:1b,front_text:{messages:['"NETHER WORKSHOP"','"Working multiblocks"','"Heat > Steam > FE"','"Step into portal"']}}
