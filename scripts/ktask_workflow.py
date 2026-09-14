"""Project delivery adapter. ktask owns task selection, retries and provider waits."""

import hashlib
import json
import os
from pathlib import Path
import re
import subprocess
import sys
import tomllib
import uuid

from ktask_contracts import parse_tasks, check_scope, check_review, review_schema
from ktask_process import run, codex_args
from ktask_evidence import record, validate_runs
from ktask_guardian import invoke as run_model

ROOT = Path(__file__).resolve().parents[1]


class ReviewUnavailable(ValueError):
    """Review execution failed without producing a semantic verdict."""


def git(root, *args):
    return subprocess.run(["git", *args], cwd=root, check=True, capture_output=True,
                          text=True).stdout.strip()


def save(path, value):
    path.parent.mkdir(parents=True, exist_ok=True)
    temporary = path.with_suffix(".tmp")
    temporary.write_text(json.dumps(value, indent=2) + "\n")
    temporary.replace(path)


def tracked_clean(root):
    return not git(root, "status", "--porcelain", "--untracked-files=no")


def untracked(root):
    return set(git(root, "ls-files", "--others", "--exclude-standard", "-z").split("\0")) - {""}


def file_digest(root, paths):
    result = {}
    for name in sorted(paths):
        path = root / name
        if path.is_symlink():
            result[name] = "link:" + os.readlink(path)
        elif path.is_file():
            result[name] = hashlib.sha256(path.read_bytes()).hexdigest()
        else:
            result[name] = "missing"
    return result


def begin(root, task, policy):
    if git(root, "branch", "--show-current") != policy["branch"] or not tracked_clean(root):
        raise ValueError("A clean checkpoint on the authorized feature branch is required")
    return dict(task=task["id"], packet=task["digest"], baseline=git(root, "rev-parse", "HEAD"),
                branch=policy["branch"], protected=file_digest(root, untracked(root)), token=uuid.uuid4().hex)


def candidate_state(root, task, state, policy, committed=False, allowed_controls=()):
    if (git(root, "branch", "--show-current") != state["branch"]
            or state["branch"] != policy["branch"] or task["digest"] != state["packet"]
            or git(root, "rev-parse", "HEAD") != state["commit" if committed else "baseline"]):
        raise ValueError("Task, branch or checkpoint changed outside delivery")
    if file_digest(root, state["protected"]) != state["protected"]:
        raise ValueError("Pre-existing untracked user files changed")
    paths = set(git(root, "diff", "--no-renames", "--name-only", "-z", state["baseline"]).split("\0")) - {""}
    paths |= untracked(root) - state["protected"].keys()
    check_scope(task, paths, allowed_controls)
    if not paths:
        raise ValueError("No implementation change to accept")
    contents = file_digest(root, paths)
    modes = {name: bool((root / name).lstat().st_mode & 0o100)
             for name in paths if (root / name).exists() and not (root / name).is_symlink()}
    fingerprint = hashlib.sha256(json.dumps([contents, modes], sort_keys=True).encode())
    return sorted(paths), fingerprint.hexdigest()


def evidence_binding(state):
    return {key: state[key] for key in ('task', 'baseline', 'packet', 'token')}


def evidence(root, task, state, candidate):
    folder = root / ".ktask/session/evidence" / task["id"]
    files = ["evidence.json"]
    try:
        value = json.loads((folder / "evidence.json").read_text())
        if value["task"] != task["id"] or value["baseline"] != state["baseline"]:
            raise ValueError("Stale test evidence")
        recorded = validate_runs(folder, evidence_binding(state), candidate, task['Evidence'].split(', '))
        files += recorded
        for kind in task["Evidence"].split(", "):
            if not value["artifacts"][kind]:
                raise ValueError(f"Missing {kind} evidence")
            for name in value["artifacts"][kind]:
                safe_artifact(folder, name)
                if name not in recorded:
                    raise ValueError(f'Artifact lacks a producing run: {name}')
                files.append(name)
    except (OSError, KeyError, TypeError, json.JSONDecodeError) as error:
        raise ValueError(f"Incomplete evidence at {folder}") from error
    return file_digest(folder, files)


def safe_artifact(folder, name):
    path = (folder / name).resolve()
    if not path.is_relative_to(folder.resolve()) or not path.is_file() or path.stat().st_size == 0:
        raise ValueError(f"Missing or invalid evidence artifact: {name}")


def run_gate(root):
    run(["bash", ".ktask/verify.sh"], root, 1800)


def review_candidate(root, task, state, candidate, policy):
    folder = root / ".ktask/session/reviews" / task["id"] / candidate
    folder.mkdir(parents=True, exist_ok=True)
    schema = folder / "schema.json"
    output = folder / "verdict.json"
    schema.write_text(json.dumps(review_schema()))
    if output.exists():
        output.rename(folder / f"prior-{output.stat().st_mtime_ns}.json")
    prompt = (root / ".ktask/review.md").read_text()
    prompt += f"\nTask: {task['id']}\nBaseline: {state['baseline']}\nCandidate: {candidate}\n"
    prompt += f"Evidence: .ktask/session/evidence/{task['id']}\n\n{task['body']}\n"
    try:
        run_model([*codex_args(policy['reviewer_model'], policy['reviewer_effort']),
             "--output-schema", str(schema), "-o", str(output), "-C", str(root), "-"],
            root, policy["review_timeout"], prompt, log=folder / 'transcript.log', check=True)
    except (ValueError, OSError, subprocess.SubprocessError) as error:
        raise ReviewUnavailable(str(error)) from error
    try:
        return json.loads(output.read_text())
    except (OSError, json.JSONDecodeError) as error:
        raise ValueError("Reviewer produced no valid structured verdict") from error


def push_receipt(root, task, state, policy):
    commit = state["commit"]
    if (git(root, "rev-parse", "HEAD") != commit or not tracked_clean(root)
            or git(root, "branch", "--show-current") != policy["branch"]
            or state["packet"] != task["digest"]
            or untracked(root) != state["protected"].keys()
            or file_digest(root, state["protected"]) != state["protected"]):
        raise ValueError("Pending delivery changed; coordinator recovery required")
    if candidate_state(root, task, state, policy, committed=True)[1] != state["candidate"]:
        raise ValueError("Committed content differs from the reviewed candidate")
    publish(root, policy, commit)
    save(root / ".ktask/session/accepted" / (task["id"] + ".json"), state)
    return commit


def publish(root, policy, commit):
    if git(root, 'rev-parse', 'HEAD') != commit or git(root, 'branch', '--show-current') != policy['branch']:
        raise ValueError('Delivery checkpoint or branch changed')
    credentials = (["-c", "credential.helper=", "-c", "credential.helper=!gh auth git-credential"]
                   if policy.get("github_cli_credentials", False) else [])
    git(root, *credentials, "push", policy["remote"], f"HEAD:refs/heads/{policy['branch']}")
    remote = git(root, "ls-remote", policy["remote"], f"refs/heads/{policy['branch']}")
    if not remote or remote.split()[0] != commit:
        raise ValueError("Remote branch does not contain the accepted commit")


def accept(root, task, state, policy):
    if "commit" in state:
        return push_receipt(root, task, state, policy)
    paths, candidate = candidate_state(root, task, state, policy)
    evidence_digest = evidence(root, task, state, candidate)
    run_gate(root)
    if candidate_state(root, task, state, policy)[1] != candidate:
        raise ValueError("Candidate changed during tests")
    if evidence(root, task, state, candidate) != evidence_digest:
        raise ValueError("Test evidence changed during acceptance")
    verdict = review_candidate(root, task, state, candidate, policy)
    save(root / ".ktask/session/last-review.json", verdict)
    check_review(verdict, task["id"], candidate)
    if candidate_state(root, task, state, policy)[1] != candidate:
        raise ValueError("Candidate changed during review")
    if evidence(root, task, state, candidate) != evidence_digest:
        raise ValueError("Test evidence changed during review")
    indexed = set(git(root, "ls-files", "-z").split("\0"))
    to_stage = [name for name in paths if name in indexed or os.path.lexists(root / name)]
    if to_stage:
        git(root, "add", "--", *to_stage)
    if candidate_state(root, task, state, policy)[1] != candidate:
        raise ValueError("Candidate changed while staging")
    git(root, "commit", "-m", f"{task['id']}: {task['title']}\n\n"
        f"Task-packet: {task['digest']}\nReviewed-candidate: {candidate}\n"
        "Validation: red/green evidence, authoritative gate, independent review.")
    state.update(commit=git(root, "rev-parse", "HEAD"), candidate=candidate)
    save(root / ".ktask/session/active.json", state)
    return push_receipt(root, task, state, policy)


def load_project(root):
    tasks = parse_tasks((root / ".ktask/tasks.md").read_text())
    policy = tomllib.loads((root / ".ktask/policy.toml").read_text())
    if policy["branch"] in ("main", "master", "V3_1.21.1"):
        raise ValueError("Delivery must target the explicit feature branch")
    return tasks, policy


def plan_digest(root):
    paths = [".ktask/" + name for name in
             ("config.toml", "policy.toml", "context.md", "prompt.md", "tasks.md",
              "review.md", "autoresolve.md", "readiness.md", "coordinator.md")]
    paths += [str(path.relative_to(root)) for path in sorted((root / 'scripts').glob('ktask_*.py'))]
    paths += ["AGENTS.md", "VISION.md", ".ktask/verify.sh"]
    paths += [str(path.relative_to(root)) for path in sorted((root / "docs").glob("*.md"))]
    return hashlib.sha256(json.dumps(file_digest(root, paths), sort_keys=True).encode()).hexdigest()


def require_session(root):
    expected = root / ".ktask/session/plan.json"
    if not expected.exists() or json.loads(expected.read_text())["digest"] != plan_digest(root):
        raise ValueError("No matching prepared session; use the project launcher")
    return json.loads(expected.read_text())


def executor(root, tasks, policy, argv):
    plan = require_session(root)
    prompt = sys.stdin.read()
    header = re.search(r"\[Orchestrator context\] Task (\d+) of (\d+)", prompt)
    total = len(plan['task_ids']) if isinstance(plan, dict) else len(tasks)
    if not header or int(header[2]) != total or not 1 <= int(header[1]) <= total:
        raise ValueError("Unrecognized ktask executor context")
    task = tasks[int(header[1]) - 1]
    if task["body"] not in prompt:
        raise ValueError("Runtime packet differs from the canonical task")
    active = root / ".ktask/session/active.json"
    state = json.loads(active.read_text()) if active.exists() else None
    if not state or state["task"] != task["id"]:
        for dep in task["dependencies"]:
            receipt = root / ".ktask/session/accepted" / (dep + ".json")
            if not receipt.exists():
                raise ValueError(f"Unaccepted dependency: {dep}")
            git(root, "merge-base", "--is-ancestor", json.loads(receipt.read_text())["commit"], "HEAD")
        state = begin(root, task, policy)
        save(active, state)
    if state["packet"] != task["digest"]:
        raise ValueError("Active task packet changed")
    if "commit" in state:
        report = root / f".ktask/session/.ktask/queue/report-{header[1]}.md"
        report.write_text("KTASK_RESULT: DONE\nReviewed commit awaits remote confirmation.\n")
        return
    if git(root, "rev-parse", "HEAD") != state["baseline"]:
        raise ValueError("Worker checkpoint moved without acceptance")
    model = argv[argv.index('--model') + 1] if '--model' in argv else policy.get('worker_model', 'gpt-5.6-sol')
    effort = next((arg.split('=', 1)[1].strip('"') for arg in argv
                   if arg.startswith('model_reasoning_effort=')), 'high')
    argv = codex_args(model, effort, writable=True, cache=policy.get('gradle_cache')) + ['-']
    prompt += "\nThe delivery adapter owns commits and pushes. Do not perform either.\n"
    prompt += f"Baseline: {state['baseline']}\nEvidence directory: .ktask/session/evidence/{task['id']}\n"
    diagnosis = root / '.ktask/session/planning' / task['id'] / 'reason.txt'
    if diagnosis.exists():
        prompt += '\nCoordinator diagnosis:\n' + diagnosis.read_text()
    run_model(argv, root, 7200, prompt, check=True)


def validate(root, tasks):
    catalog = (root / "docs/BLOCK_CATALOG.md").read_text()
    blocks = set(re.findall(r"^### (T[0-7]-\d+)", catalog, re.M)) - {"T7-09"}
    assigned = []
    for task in tasks:
        assigned.extend([] if task["Blocks"] == "none" else task["Blocks"].split(", "))
        for path in task["Spec"].split(", "):
            if not (root / path).is_file():
                raise ValueError(f"Missing specification: {path}")
    if len(assigned) != len(set(assigned)) or set(assigned) != blocks:
        raise ValueError(f"Block ownership mismatch: missing={blocks - set(assigned)}, "
                         f"extra={set(assigned) - blocks}")
    print(f"{len(tasks)} ordered tasks; {len(blocks)} in-scope block entries owned once.", flush=True)


def main():
    command, *arguments = sys.argv[1:]
    if command in ("run", "resume", "retry", "status", "reconcile"):
        os.execv(sys.executable, [sys.executable, str(ROOT / 'scripts/ktask_supervisor.py'), command, *arguments])
    tasks, policy = load_project(ROOT)
    if command == "validate":
        validate(ROOT, tasks)
    elif command == "scope":
        task = next(task for task in tasks if task["id"] == arguments[0])
        print(json.dumps(task["scope"], indent=2))
    elif command == "executor":
        executor(ROOT, tasks, policy, arguments)
    elif command in ("accept", "check-evidence"):
        require_session(ROOT)
        state = json.loads((ROOT / ".ktask/session/active.json").read_text())
        task = next(task for task in tasks if task["id"] == state["task"])
        if command == 'check-evidence':
            candidate = candidate_state(ROOT, task, state, policy)[1]
            evidence(ROOT, task, state, candidate)
            print(f'{task["id"]}: current candidate evidence is valid')
        else:
            print(accept(ROOT, task, state, policy))
    elif command == 'record':
        require_session(ROOT)
        state = json.loads((ROOT / '.ktask/session/active.json').read_text())
        task = next(task for task in tasks if task['id'] == state['task'])
        import argparse
        parser = argparse.ArgumentParser()
        parser.add_argument('phase')
        parser.add_argument('--artifact', action='append', default=[])
        if '--' not in arguments:
            raise ValueError('Use record PHASE -- COMMAND [ARGS]')
        split = arguments.index('--')
        options = parser.parse_args(arguments[:split])
        phase, argv = options.phase, arguments[split + 1:]
        candidate = candidate_state(ROOT, task, state, policy)[1]
        record(ROOT / '.ktask/session/evidence' / task['id'], evidence_binding(state),
               candidate, phase, argv, ROOT, 7200 if phase == 'soak' else 1800, options.artifact)
    else:
        raise ValueError("Use validate, status, run, resume, retry or check-evidence")


if __name__ == "__main__":
    try:
        main()
    except (ValueError, OSError, subprocess.SubprocessError, StopIteration) as error:
        print(f"ktask delivery refused: {error}", file=sys.stderr)
        sys.exit(1)
