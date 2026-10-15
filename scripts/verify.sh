#!/usr/bin/env bash
# The one gate every task must pass. Keeps the log in build/verification/full-gate.log.
set -euo pipefail
cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.."
mkdir -p build/verification
./gradlew verifyAll --max-workers=1 --console=plain 2>&1 | tee build/verification/full-gate.log
