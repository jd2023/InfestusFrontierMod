#!/usr/bin/env bash
# Builds the mod, creates a fresh demo world with every feature on show, and opens the client in it.
# You spawn facing the "Starter supplies" chest: take a log and the Infestus Frontier tab opens in the Advancements
# screen (L); its steps run from a Spore Culture to a working Bio-Furnace. The tab starts empty, with no advancement
# granted. Play, then close the window. The world is rebuilt on every run; your options.txt in runs/demo is kept.
set -euo pipefail
cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.."
./gradlew runDemo --console=plain "$@"
