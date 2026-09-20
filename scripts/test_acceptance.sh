#!/usr/bin/env bash
set -euo pipefail
repo=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)
fixture=$(mktemp -d -t infestus-acceptance-XXXXXX)
trap 'rm -rf -- "$fixture"' EXIT
mkdir -p "$fixture/.ktask/logs" "$fixture/bin"
cp "$repo/.ktask/accept.sh" "$fixture/.ktask/accept.sh"
cp "$repo/.ktask/review.md" "$fixture/.ktask/review.md"
cat > "$fixture/.ktask/verify.sh" <<'GATE'
exit "${TEST_GATE_EXIT:-0}"
GATE
cat > "$fixture/bin/codex" <<'MODEL'
#!/usr/bin/env bash
set -euo pipefail
[[ " $* " == *' --sandbox read-only '* ]]
while (( $# )); do
    if [[ $1 == -o ]]; then output=$2; shift; fi
    shift
done
cat >/dev/null
if [[ ${TEST_MODEL_MODE:-approve} == empty ]]; then exit 0; fi
printf '%s\n' "${TEST_VERDICT:-APPROVED}" > "$output"
if [[ ${TEST_MODEL_MODE:-approve} == fail ]]; then exit 7; fi
MODEL
chmod +x "$fixture/bin/codex"
export PATH="$fixture/bin:$PATH"
run_hook() { bash "$fixture/.ktask/accept.sh" > "$fixture/output" 2>&1; }
reject() {
    if run_hook; then
        printf 'Expected rejection: %s\n' "$1" >&2
        exit 1
    fi
}
run_hook
grep -qx APPROVED "$fixture/.ktask/logs/review.md"
export TEST_VERDICT=REJECTED
reject 'review finding'
export TEST_VERDICT=APPROVED TEST_GATE_EXIT=1
reject 'failed mod tests'
export TEST_GATE_EXIT=0
run_hook
export TEST_MODEL_MODE=empty
reject 'missing fresh review, despite an earlier approval'
export TEST_MODEL_MODE=fail
reject 'reviewer execution failure'
export TEST_MODEL_MODE=approve
run_hook
printf 'Acceptance hook tests passed (stub reviewer; no task execution).\n'
