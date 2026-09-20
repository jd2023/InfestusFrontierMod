#!/usr/bin/env bash
set -euo pipefail
cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.."
mkdir -p build/verification
python3 scripts/check_design_docs.py --self-test
python3 scripts/check_design_docs.py
python3 scripts/check_armor_balance.py --self-test
python3 scripts/check_armor_balance.py
./gradlew verifyAll --max-workers=1 --console=plain 2>&1 | tee build/verification/full-gate.log
