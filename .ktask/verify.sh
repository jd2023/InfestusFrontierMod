#!/usr/bin/env bash
set -euo pipefail
cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.."
mkdir -p build/verification
python3 scripts/check_design_docs.py --self-test
python3 scripts/check_design_docs.py
./gradlew verifyAll --max-workers=1 --console=plain 2>&1 | tee build/verification/full-gate.log
if ! rg -q 'All [1-9][0-9]* required tests passed' build/verification/full-gate.log; then
    echo 'The gate did not prove that required Minecraft GameTests ran.' >&2
    exit 1
fi
if rg -n '\[ERROR\]|/ERROR\]|Exception in thread|FAILED' build/verification/full-gate.log; then
    echo 'Severe output detected; inspect the full gate log.' >&2
    exit 1
fi
