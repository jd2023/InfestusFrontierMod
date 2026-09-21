#!/usr/bin/env bash
set -euo pipefail
cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.."
bash .ktask/verify.sh
mkdir -p .ktask/logs
review=$(mktemp .ktask/logs/review.XXXXXX)
trap 'rm -f -- "$review"' EXIT
codex exec --ephemeral --ignore-user-config --sandbox read-only \
    -c 'approval_policy="never"' --model gpt-6-astra \
    -c 'model_reasoning_effort="high"' -o "$review" - < .ktask/review.md
cat "$review"
cp "$review" .ktask/logs/review.md
if [[ $(head -n 1 "$review") != APPROVED ]]; then
    echo 'Independent review did not approve; see .ktask/logs/review.md.' >&2
    exit 1
fi
