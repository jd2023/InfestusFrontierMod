#!/usr/bin/env bash
# The one gate every story must pass. Keeps the log in build/verification/full-gate.log.
# Headless checks and the graphical capture run are separate Gradle invocations.
set -euo pipefail
cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.."
mkdir -p build/verification
log=build/verification/full-gate.log
filter_gametest_progress() {
    # Drop only INFO bars containing pending/running/passed tests. Keep failure
    # bars and every diagnostic/summary. Stream output so failures appear promptly.
    sed -u '/^\[[^]]*\] \[Server thread\/INFO\] \[minecraft\/GameTestServer\]: \[[_+ ]*\]$/d'
}
./gradlew verifyAll --max-workers=1 --console=plain 2>&1 | filter_gametest_progress | tee "$log"
./gradlew verifyCaptures --max-workers=1 --console=plain 2>&1 | tee -a "$log"
./gradlew verifyCaptureFailure --max-workers=1 --console=plain 2>&1 | tee -a "$log"
