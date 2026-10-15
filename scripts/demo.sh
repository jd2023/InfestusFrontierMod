#!/usr/bin/env bash
# Builds the mod, creates a fresh demo world with every feature on show, and opens the client in it.
# Play, then close the window. The world is rebuilt on every run; your options.txt in runs/demo is kept.
set -euo pipefail
cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.."
./gradlew runDemo --console=plain "$@"
