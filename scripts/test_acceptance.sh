#!/usr/bin/env bash
set -euo pipefail
repo=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)
fixture=$(mktemp -d -t infestus-acceptance-XXXXXX)
trap 'rm -rf -- "$fixture"' EXIT
mkdir -p "$fixture/.ktask/logs" "$fixture/.ktask/queue" "$fixture/bin"
cp "$repo/.ktask/accept.sh" "$fixture/.ktask/accept.sh"
cp "$repo/.ktask/review.md" "$fixture/.ktask/review.md"
cat > "$fixture/.ktask/verify.sh" <<'GATE'
exit "${TEST_GATE_EXIT:-0}"
GATE
printf '[Orchestrator context]\nIF-000 decoy\n\n# Implement the task\n\nIF-900 Fixture packet\nOwner: fixture\nEvidence: rules\n' \
    > "$fixture/.ktask/queue/current-task.md"
cat > "$fixture/bin/codex" <<'MODEL'
#!/usr/bin/env bash
set -euo pipefail
[[ " $* " == *' --sandbox read-only '* ]]
while (( $# )); do
    if [[ $1 == -o ]]; then output=$2; shift; fi
    shift
done
cat > "$TEST_PROMPT_COPY"
if [[ ${TEST_MODEL_MODE:-approve} == empty ]]; then exit 0; fi
printf '%s\n' "${TEST_VERDICT:-APPROVED}" > "$output"
if [[ ${TEST_MODEL_MODE:-approve} == fail ]]; then exit 7; fi
MODEL
chmod +x "$fixture/bin/codex"
export PATH="$fixture/bin:$PATH" TEST_PROMPT_COPY="$fixture/prompt-copy"
git -C "$fixture" init -q
git -C "$fixture" -c user.name=t -c user.email=t@t commit -q --allow-empty -m base
base=$(git -C "$fixture" rev-parse HEAD)
bash "$fixture/.ktask/accept.sh" --checkpoint > /dev/null
git -C "$fixture" -c user.name=t -c user.email=t@t commit -q --allow-empty -m work
state="$fixture/.ktask/logs/review-state"
run_hook() { bash "$fixture/.ktask/accept.sh" > "$fixture/output" 2>&1; }
reject() {
    if run_hook; then
        printf 'Expected rejection: %s\n' "$1" >&2
        exit 1
    fi
}
export TEST_VERDICT=$'REJECTED\n1. first-round defect\nFOLLOW-UP:\n- later polish'
reject 'review finding'
grep -qx "$base" "$state/IF-900/baseline"
grep -q "Task: IF-900 (owner fixture). Round: 1." "$TEST_PROMPT_COPY"
grep -q 'later polish' "$fixture/.ktask/logs/follow-ups.md"
export TEST_VERDICT=APPROVED TEST_GATE_EXIT=1
reject 'failed mod tests'
export TEST_GATE_EXIT=0 TEST_MODEL_MODE=empty
reject 'missing fresh review, despite an earlier verdict'
export TEST_MODEL_MODE=fail
reject 'reviewer execution failure'
export TEST_MODEL_MODE=approve
run_hook
grep -q 'Round: 2.' "$TEST_PROMPT_COPY"
grep -q 'first-round defect' "$TEST_PROMPT_COPY"
grep -qx APPROVED "$fixture/.ktask/logs/review.md"
[[ $(cat "$state/last-accepted") == "$(git -C "$fixture" rev-parse HEAD)" ]]
printf 'Acceptance hook tests passed (stub reviewer; no task execution).\n'
