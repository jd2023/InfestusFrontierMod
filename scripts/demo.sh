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
# The tab starts empty, with no advancement granted.
# Play, then close the window. The world is rebuilt on every run; your options.txt in runs/demo is kept.
set -euo pipefail
cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.."
./gradlew runDemo --console=plain "$@"
