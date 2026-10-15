#!/usr/bin/env bash
# The one gate every story must pass. Keeps the log in build/verification/full-gate.log.
# Headless checks and the graphical capture run are separate Gradle invocations.
set -euo pipefail
cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.."
mkdir -p build/verification
log=build/verification/full-gate.log
./gradlew verifyAll --max-workers=1 --console=plain 2>&1 | tee "$log"
./gradlew verifyCaptures --max-workers=1 --console=plain 2>&1 | tee -a "$log"
