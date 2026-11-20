#!/usr/bin/env bash
# Builds the mod, creates a fresh demo world with every feature on show, and opens the client in it.
# You spawn facing the "Starter supplies" chest: take a log and the Infestus Frontier tab opens in the Advancements
# screen (L); its steps run from a Spore Culture to a working Bio-Furnace and an automatic farm (the Farm exhibit at
# x 14 to 25: load the Proboscis, open the Corolla and the Sac once, bone-meal the wheat) and the Workshop steps row
# (south, x 84 to 112, z -22 to -15: feed the Gizzard, the Kidney and the furnace by hand from their chests, put a stack in
# the Capsule Tree, press Restock on a Palm and take its craft). The Palm supply exhibits (south, x 40 to 80) join a
# Capsule Tree and a chest to a Rote Palm by Item Veins; four Palms (east, x 140 to 152, z -40) show each Mouth symbol.
# Capsule Stems need an Infected Log: turn a tree in the Woodland plot (x 79 to 115), cut its log and craft at the table
# beside the supplies chest; the Timber plot (x 62) and the Capsule Stem exhibit (x 80, z -33) have tables and parts too.
# The DNA exhibit (x 132 to 146, z -24 to -18): sample the penned pig with the needle from its chest and load the
# empty bank; the bank beside it completes the pig genome on its own.
# The construct exhibit (x 148 to 162, z -24 to -18): press Produce at the bank with the complete pig genome, then apply
# the Stairs Construct to the plain Tunnel Root head and the Wider Section Construct to the Rootstock beside it.
# The staircase mine (x 17 to 26, z -14 to 2, south of the Iron Bars Factory): climb the ladder to the minehead on the
# hill, open the Rootstock, and watch the jaw cut the next steps through the glass; spoil fills the chest beside the
# hill and raw iron runs the veins into the factory's Gizzard. Walk down the staircase to the landing.
# The mining galleries (x 37 to 53, z 8 to 23): a minehead whose staircase is done cuts a wide 3x3 gallery with three
# jaws and a 1x2 gallery with one, both on one biomass line and one export line; watch each face through its glass,
# open the Rootstock on the hill to see both faces, and find the cut in the chest on the platform east of the hill.
# The power district (x 197 to 228, z 7 to 29) has the receiver and Vigil Nest. Enter the portal at
# x 223, z 24 for the working Nether workshop: Heat Body, Steam Body and Electric Body, with supplied
# boilers, a steam-driven mining jaw, a nest and spare parts. All power starts in a physical multiblock.
# Carry capsules/cells back through the portal; add chambers to grow the same core.
# Play, then close the window. The world is rebuilt on every run; your options.txt in runs/demo is kept.
set -euo pipefail
cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.."
./gradlew runDemo --console=plain "$@"
