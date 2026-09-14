"""Project delivery adapter. ktask owns task selection, retries and provider waits."""

import hashlib
import json
import os
from pathlib import Path
import re
import shutil
import signal
import subprocess
import sys
import tomllib

from ktask_contracts import parse_tasks, check_scope, check_review, review_schema

ROOT = Path(__file__).resolve().parents[1]


def git(root, *args):
    return subprocess.run(["git", *args], cwd=root, check=True, capture_output=True,
                          text=True).stdout.strip()


def save(path, value):
    path.parent.mkdir(parents=True, exist_ok=True)
    temporary = path.with_suffix(".tmp")
    temporary.write_text(json.dumps(value, indent=2) + "\n")
    temporary.replace(path)


def run(argv, root, timeout, prompt=None):
    """Terminate owned descendants on timeout or the runner's interruption."""
    runner_group = os.getpgrp() == os.getpid()
    child = subprocess.Popen(argv, cwd=root, stdin=subprocess.PIPE if prompt is not None else None,
                             text=True, start_new_session=not runner_group)
    group = os.getpgrp() if runner_group else child.pid
    def stop(signum, frame):
        os.killpg(group, signal.SIGKILL)
        raise KeyboardInterrupt
    previous = {sig: signal.signal(sig, stop) for sig in (signal.SIGTERM, signal.SIGINT)}
    try:
        child.communicate(prompt, timeout=timeout)
        if child.returncode:
            raise ValueError(f"Command failed ({child.returncode}): {argv}")
    except (subprocess.TimeoutExpired, KeyboardInterrupt):
        if child.poll() is None:
            os.killpg(group, signal.SIGKILL)
        child.wait()
        raise
    finally:
        for sig, handler in previous.items():
            signal.signal(sig, handler)


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
                branch=policy["branch"], protected=file_digest(root, untracked(root)))


def candidate_state(root, task, state, policy, committed=False):
    if (git(root, "branch", "--show-current") != state["branch"]
            or state["branch"] != policy["branch"] or task["digest"] != state["packet"]
            or git(root, "rev-parse", "HEAD") != state["commit" if committed else "baseline"]):
        raise ValueError("Task, branch or checkpoint changed outside delivery")
    if file_digest(root, state["protected"]) != state["protected"]:
        raise ValueError("Pre-existing untracked user files changed")
    paths = set(git(root, "diff", "--name-only", "-z", state["baseline"]).split("\0")) - {""}
    paths |= untracked(root) - state["protected"].keys()
    check_scope(task, paths)
    if not paths:
        raise ValueError("No implementation change to accept")
    fingerprint = hashlib.sha256()
    fingerprint.update(git(root, "diff", "--binary", state["baseline"]).encode())
    fingerprint.update(json.dumps(file_digest(root, paths), sort_keys=True).encode())
    return sorted(paths), fingerprint.hexdigest()


def evidence(root, task, state):
    folder = root / ".ktask/session/evidence" / task["id"]
    files = ["evidence.json"]
    try:
        value = json.loads((folder / "evidence.json").read_text())
        if value["task"] != task["id"] or value["baseline"] != state["baseline"]:
            raise ValueError("Stale test evidence")
        for phase in ("red", "green"):
            record = value[phase]
            if (not isinstance(record["command"], list) or not record["command"]
                    or type(record["exit"]) is not int
                    or (record["exit"] == 0) != (phase == "green")):
                raise ValueError("Invalid red/green evidence")
            safe_artifact(folder, record["log"])
            files.append(record["log"])
        for kind in task["Evidence"].split(", "):
            if not value["artifacts"][kind]:
                raise ValueError(f"Missing {kind} evidence")
            for name in value["artifacts"][kind]:
                safe_artifact(folder, name)
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
    run(["codex", "exec", "--ephemeral", "--sandbox", "read-only",
         "--model", policy["reviewer_model"], "-c",
         f'model_reasoning_effort="{policy["reviewer_effort"]}"',
         "--output-schema", str(schema), "-o", str(output), "-C", str(root), "-"],
        root, policy["review_timeout"], prompt)
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
    credentials = (["-c", "credential.helper=", "-c", "credential.helper=!gh auth git-credential"]
                   if policy.get("github_cli_credentials", False) else [])
    git(root, *credentials, "push", policy["remote"], f"HEAD:refs/heads/{policy['branch']}")
    remote = git(root, "ls-remote", policy["remote"], f"refs/heads/{policy['branch']}")
    if not remote or remote.split()[0] != commit:
        raise ValueError("Remote branch does not contain the accepted commit")
    save(root / ".ktask/session/accepted" / (task["id"] + ".json"), state)
    return commit


def accept(root, task, state, policy):
    if "commit" in state:
        return push_receipt(root, task, state, policy)
    paths, candidate = candidate_state(root, task, state, policy)
    evidence_digest = evidence(root, task, state)
    run_gate(root)
    if candidate_state(root, task, state, policy)[1] != candidate:
        raise ValueError("Candidate changed during tests")
    if evidence(root, task, state) != evidence_digest:
        raise ValueError("Test evidence changed during acceptance")
    verdict = review_candidate(root, task, state, candidate, policy)
    save(root / ".ktask/session/last-review.json", verdict)
    check_review(verdict, task["id"], candidate)
    if candidate_state(root, task, state, policy)[1] != candidate:
        raise ValueError("Candidate changed during review")
    if evidence(root, task, state) != evidence_digest:
        raise ValueError("Test evidence changed during review")
    git(root, "add", "--", *paths)
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
              "review.md", "autoresolve.md")]
    paths += ["scripts/ktask_workflow.py", "scripts/ktask_contracts.py"]
    paths += ["AGENTS.md", "VISION.md", ".ktask/verify.sh"]
    paths += [str(path.relative_to(root)) for path in sorted((root / "docs").glob("*.md"))]
    return hashlib.sha256(json.dumps(file_digest(root, paths), sort_keys=True).encode()).hexdigest()


def require_session(root):
    expected = root / ".ktask/session/plan.json"
    if not expected.exists() or json.loads(expected.read_text())["digest"] != plan_digest(root):
        raise ValueError("No matching prepared session; use the project launcher")


def prepare_session(root, policy):
    session = root / ".ktask/session"
    if session.exists():
        require_session(root)
        return session
    if not tracked_clean(root) or git(root, "branch", "--show-current") != policy["branch"]:
        raise ValueError("Commit the plan on the configured branch before launching")
    runtime = session / ".ktask"
    runtime.mkdir(parents=True)
    for name in ("tasks.md", "context.md", "prompt.md", "autoresolve.md"):
        shutil.copyfile(root / ".ktask" / name, runtime / name)
    config = (root / ".ktask/config.toml").read_text()
    config = re.sub(r'^project_dir = .*$', "project_dir = " + json.dumps(str(root)),
                    config, count=1, flags=re.M)
    (runtime / "config.toml").write_text(config)
    save(session / "plan.json", dict(digest=plan_digest(root)))
    return session


def executor(root, tasks, policy, argv):
    require_session(root)
    prompt = sys.stdin.read()
    header = re.search(r"\[Orchestrator context\] Task (\d+) of (\d+)", prompt)
    if not header or int(header[2]) != len(tasks) or not 1 <= int(header[1]) <= len(tasks):
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
    argv = [arg for arg in argv if arg != "--dangerously-bypass-approvals-and-sandbox"]
    argv[1:1] = ["--sandbox", "workspace-write"]
    prompt += "\nThe delivery adapter owns commits and pushes. Do not perform either.\n"
    prompt += f"Baseline: {state['baseline']}\nEvidence directory: .ktask/session/evidence/{task['id']}\n"
    run(["codex", *argv], root, 7200, prompt)


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
    tasks, policy = load_project(ROOT)
    command, *arguments = sys.argv[1:]
    if command == "validate":
        validate(ROOT, tasks)
    elif command == "scope":
        task = next(task for task in tasks if task["id"] == arguments[0])
        print(json.dumps(task["scope"], indent=2))
    elif command == "executor":
        executor(ROOT, tasks, policy, arguments)
    elif command == "accept":
        require_session(ROOT)
        state = json.loads((ROOT / ".ktask/session/active.json").read_text())
        task = next(task for task in tasks if task["id"] == state["task"])
        print(accept(ROOT, task, state, policy))
    elif command in ("run", "resume", "retry", "status"):
        validate(ROOT, tasks)
        session = ROOT if command == "status" and not (ROOT / ".ktask/session").exists() else prepare_session(ROOT, policy)
        os.chdir(session)
        os.execv(policy["runner"], [policy["runner"], command, *arguments])
    else:
        raise ValueError("Use validate, status, run, resume or retry")


if __name__ == "__main__":
    try:
        main()
    except (ValueError, OSError, subprocess.SubprocessError, StopIteration) as error:
        print(f"ktask delivery refused: {error}", file=sys.stderr)
        sys.exit(1)
