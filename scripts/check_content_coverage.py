#!/usr/bin/env python3
"""Validate bounded, explicit catalog-to-runtime coverage contributions.

The working-tree checkpoint is authoritative.  This gate never infers gameplay
content from compiled classes, runtime registries, Git receipts, or ignored files.
"""

from __future__ import annotations

import argparse
import json
import re
import sys
import unittest
from dataclasses import dataclass
from pathlib import Path
from typing import Any


ROOT = Path(__file__).resolve().parents[1]
TASK_ID = re.compile(r"IF-\d{3}\Z")
CATALOG_ID = re.compile(r"(?:I\d{3}|T\d+-\d+|[MHCLB]\d+\.I{1,3})\Z")
RESOURCE_ID = re.compile(r"[a-z0-9_.-]+:[a-z0-9_./-]+\Z")
MAX_IDENTIFIERS = 4096
MAX_EDGES = 16384
MAX_INPUT_BYTES = 1024 * 1024
KNOWN_ALIASES = {frozenset(("I001", "T0-16")): "I001/T0-16"}


class CoverageError(ValueError):
    """A diagnostic that identifies the rejected task or catalog obligation."""


@dataclass(frozen=True)
class Task:
    identifier: str
    owner: str
    blocks: tuple[str, ...]


def _load_json(path: Path, label: str) -> Any:
    try:
        if path.stat().st_size > MAX_INPUT_BYTES:
            raise CoverageError(f"{label}: exceeds 1 MiB input bound")
        return json.loads(path.read_text(encoding="utf-8"))
    except OSError as exc:
        raise CoverageError(f"{label}: cannot read {path}: {exc}") from exc
    except json.JSONDecodeError as exc:
        raise CoverageError(f"{label}: invalid JSON at {path}: {exc}") from exc


def _keys(value: Any, expected: set[str], label: str) -> None:
    if not isinstance(value, dict):
        raise CoverageError(f"{label}: expected object")
    difference = set(value) ^ expected
    if difference:
        raise CoverageError(f"{label}: missing or unknown fields {sorted(difference)}")


def _tasks(path: Path) -> list[Task]:
    try:
        text = path.read_text(encoding="utf-8")
    except OSError as exc:
        raise CoverageError(f"task queue: cannot read {path}: {exc}") from exc
    headers = list(
        re.finditer(r"(?m)^(?:\[[A-Z][A-Z_ -]*\] )?(IF-\d{3})\b[^\n]*$", text)
    )
    if not headers:
        raise CoverageError("task queue: no IF-nnn packets")
    result: list[Task] = []
    seen: set[str] = set()
    for index, match in enumerate(headers):
        identifier = match.group(1)
        if identifier in seen:
            raise CoverageError(f"task queue: duplicate {identifier}")
        seen.add(identifier)
        end = headers[index + 1].start() if index + 1 < len(headers) else len(text)
        body = text[match.end() : end]
        owner_match = re.search(r"(?m)^Owner: ([a-z][a-z0-9_-]*)$", body)
        blocks_match = re.search(r"(?m)^Blocks: (.+)$", body)
        if not owner_match or not blocks_match:
            raise CoverageError(f"{identifier}: missing Owner or Blocks field")
        raw_blocks = blocks_match.group(1)
        blocks = () if raw_blocks == "none" else tuple(
            part.strip() for part in raw_blocks.split(",")
        )
        if any(not re.fullmatch(r"T\d+-\d+", block) for block in blocks):
            raise CoverageError(f"{identifier}: invalid Blocks field {raw_blocks!r}")
        if len(blocks) != len(set(blocks)):
            raise CoverageError(f"{identifier}: duplicate block obligation")
        result.append(Task(identifier, owner_match.group(1), blocks))
    return result


def _plan(path: Path, tasks: dict[str, Task]) -> tuple[dict[str, str], dict[str, list[str]]]:
    plan = _load_json(path, "content plan")
    _keys(
        plan,
        {"items", "mutations", "services", "requires", "excluded_items"},
        "content plan",
    )
    ownership: dict[str, str] = {}
    for section, pattern in (
        ("items", re.compile(r"I\d{3}\Z")),
        ("mutations", re.compile(r"[MHCLB]\d+\.I{1,3}\Z")),
        ("services", re.compile(r"[a-z][a-z0-9_]*\Z")),
    ):
        values = plan[section]
        if not isinstance(values, dict):
            raise CoverageError(f"content plan: {section} must be an object")
        for identifier, task in values.items():
            if not isinstance(identifier, str) or not pattern.fullmatch(identifier):
                raise CoverageError(f"content plan: invalid {section} identifier {identifier!r}")
            if task not in tasks:
                raise CoverageError(f"{identifier}: unknown producer task {task!r}")
            if identifier in ownership:
                raise CoverageError(f"{identifier}: duplicate ownership")
            ownership[identifier] = task

    excluded = plan["excluded_items"]
    if (
        not isinstance(excluded, list)
        or any(not isinstance(item, str) or not re.fullmatch(r"I\d{3}", item) for item in excluded)
        or len(excluded) != len(set(excluded))
    ):
        raise CoverageError("content plan: excluded_items must contain unique item IDs")
    overlap = set(excluded) & ownership.keys()
    if overlap:
        raise CoverageError(f"content plan: excluded item also owned: {sorted(overlap)[0]}")

    for task in tasks.values():
        for block in task.blocks:
            if block in ownership:
                raise CoverageError(f"{block}: duplicate block ownership")
            ownership[block] = task.identifier

    if len(ownership) > MAX_IDENTIFIERS:
        raise CoverageError(f"content plan exceeds {MAX_IDENTIFIERS} identifiers")

    requires = plan["requires"]
    if not isinstance(requires, dict):
        raise CoverageError("content plan: requires must be an object")
    edge_count = 0
    for consumer, producers in requires.items():
        if consumer not in tasks:
            raise CoverageError(f"content plan: unknown consumer {consumer}")
        if not isinstance(producers, list) or any(
            not isinstance(producer, str) for producer in producers
        ):
            raise CoverageError(f"{consumer}: requires must be a string list")
        edge_count += len(producers)
        if edge_count > MAX_EDGES:
            raise CoverageError(f"content plan exceeds {MAX_EDGES} edges")
    return ownership, requires


def _resource(value: Any, label: str) -> str:
    if not isinstance(value, str) or not RESOURCE_ID.fullmatch(value):
        raise CoverageError(f"{label}: expected namespaced identifier")
    return value


def _string_list(value: Any, label: str, pattern: re.Pattern[str]) -> list[str]:
    if (
        not isinstance(value, list)
        or not value
        or len(value) > MAX_IDENTIFIERS
        or any(not isinstance(item, str) or not pattern.fullmatch(item) for item in value)
        or len(value) != len(set(value))
    ):
        raise CoverageError(f"{label}: expected a non-empty unique identifier list")
    return value


def _read_contributors(
    root: Path,
    task_by_id: dict[str, Task],
    task_position: dict[str, int],
    through_position: int,
    ownership: dict[str, str],
    base_edges: int,
) -> tuple[dict[str, dict[str, Any]], dict[str, str]]:
    resources = root / "src/testMod/resources"
    paths = sorted(resources.rglob("coverage.json")) if resources.is_dir() else []
    representations: dict[str, dict[str, Any]] = {}
    assertion_owners: dict[str, str] = {}
    contribution_tasks: dict[str, str] = {}
    identifiers = set(ownership) | set(task_by_id)
    edge_count = base_edges

    for path in paths:
        relative = path.relative_to(resources)
        if len(relative.parts) != 2:
            raise CoverageError(f"coverage contributor must use <owner>/coverage.json: {relative}")
        path_owner = relative.parts[0]
        document = _load_json(path, f"{path_owner} contributor")
        _keys(document, {"schema", "owner", "contributions"}, path_owner)
        if document["schema"] != 1 or document["owner"] != path_owner:
            raise CoverageError(f"{path_owner}: schema 1 and matching owner required")
        contributions = document["contributions"]
        if not isinstance(contributions, list) or not contributions:
            raise CoverageError(f"{path_owner}: contributions must be non-empty")
        for contribution in contributions:
            _keys(contribution, {"task", "entries"}, f"{path_owner} contribution")
            task_id = contribution["task"]
            if not isinstance(task_id, str) or task_id not in task_by_id:
                raise CoverageError(f"{task_id}: unknown contribution task")
            if task_position[task_id] > through_position:
                raise CoverageError(f"{task_id}: contribution is later than checkpoint")
            if task_by_id[task_id].owner != path_owner:
                raise CoverageError(f"{task_id}: contributor owner must be {task_by_id[task_id].owner}")
            if task_id in contribution_tasks:
                raise CoverageError(f"{task_id}: duplicate contribution")
            contribution_tasks[task_id] = path_owner
            entries = contribution["entries"]
            if not isinstance(entries, list) or not entries:
                raise CoverageError(f"{task_id}: entries must be non-empty")
            for entry in entries:
                catalog_hint = "entry"
                if isinstance(entry, dict) and isinstance(entry.get("catalog"), list):
                    catalog_hint = "/".join(map(str, entry["catalog"]))
                _keys(
                    entry,
                    {"catalog", "registry", "producer", "assertions"},
                    catalog_hint,
                )
                catalogs = _string_list(entry["catalog"], catalog_hint, CATALOG_ID)
                catalog_hint = "/".join(catalogs)
                registries = _string_list(entry["registry"], catalog_hint, RESOURCE_ID)
                producer = _resource(entry["producer"], f"{catalog_hint} producer")
                assertions = entry["assertions"]
                _keys(assertions, {"obtain", "use", "guide"}, catalog_hint)
                normalized_assertions = {
                    role: _resource(assertions[role], f"{catalog_hint} {role} assertion")
                    for role in ("obtain", "use", "guide")
                }
                identifiers.update(registries)
                identifiers.add(producer)
                identifiers.update(normalized_assertions.values())
                if len(identifiers) > MAX_IDENTIFIERS:
                    raise CoverageError(
                        f"{catalog_hint}: coverage exceeds {MAX_IDENTIFIERS} identifiers"
                    )
                edge_count += len(catalogs) + len(registries) + 4
                if edge_count > MAX_EDGES:
                    raise CoverageError(
                        f"{catalog_hint}: coverage exceeds {MAX_EDGES} edges"
                    )
                for catalog in catalogs:
                    if catalog not in ownership:
                        raise CoverageError(f"{catalog}: unknown catalog contribution")
                    if ownership[catalog] != task_id:
                        raise CoverageError(
                            f"{catalog}: belongs to {ownership[catalog]}, not {task_id}"
                        )
                    if catalog in representations:
                        raise CoverageError(f"{catalog}: duplicate representation")
                for role, assertion in normalized_assertions.items():
                    if assertion in assertion_owners:
                        raise CoverageError(
                            f"{catalog_hint}: {role} assertion already proves {assertion_owners[assertion]}"
                        )
                    assertion_owners[assertion] = catalog_hint
                normalized = {
                    "task": task_id,
                    "owner": path_owner,
                    "catalog": catalogs,
                    "registry": registries,
                    "producer": producer,
                    "assertions": normalized_assertions,
                }
                for catalog in catalogs:
                    representations[catalog] = normalized

    for alias, label in KNOWN_ALIASES.items():
        present = alias & representations.keys()
        if present and present != alias:
            raise CoverageError(f"{label}: alias obligations must share one representation")
        if present:
            values = {id(representations[catalog]) for catalog in alias}
            if len(values) != 1 or set(next(iter(representations[c] for c in alias))["catalog"]) != alias:
                raise CoverageError(f"{label}: aliases must map to one representation")
    return representations, contribution_tasks


def validate(root: Path = ROOT, through: str | None = None) -> dict[str, Any]:
    root = Path(root)
    tasks = _tasks(root / ".ktask/tasks.md")
    task_by_id = {task.identifier: task for task in tasks}
    task_position = {task.identifier: index for index, task in enumerate(tasks)}
    checkpoint_path = root / "src/testMod/resources/content-checkpoint.json"
    checkpoint = _load_json(checkpoint_path, "content checkpoint")
    _keys(checkpoint, {"schema", "through"}, "content checkpoint")
    tracked_through = checkpoint["through"]
    if checkpoint["schema"] != 1 or tracked_through not in task_by_id:
        raise CoverageError(f"content checkpoint: invalid through {tracked_through!r}")
    effective = through or tracked_through
    if not isinstance(effective, str) or not TASK_ID.fullmatch(effective) or effective not in task_by_id:
        raise CoverageError(f"content checkpoint: unknown explicit through {effective!r}")
    if task_position[effective] < task_position[tracked_through]:
        raise CoverageError(
            f"{effective}: explicit checkpoint cannot lower tracked {tracked_through}"
        )

    ownership, requires = _plan(root / ".ktask/content-plan.json", task_by_id)
    effective_position = task_position[effective]
    for consumer, dependencies in requires.items():
        if task_position[consumer] > effective_position:
            continue
        for dependency in dependencies:
            producer = ownership.get(dependency)
            if producer is None:
                raise CoverageError(f"{consumer}: unknown required producer {dependency}")
            if task_position[producer] > task_position[consumer]:
                raise CoverageError(
                    f"{consumer}: required producer {dependency} is later ({producer})"
                )

    required = {
        identifier
        for identifier, task_id in ownership.items()
        if CATALOG_ID.fullmatch(identifier)
        and task_position[task_id] <= effective_position
    }
    representations, contribution_tasks = _read_contributors(
        root,
        task_by_id,
        task_position,
        effective_position,
        ownership,
        sum(len(edges) for edges in requires.values()),
    )
    missing = sorted(required - representations.keys())
    if missing:
        raise CoverageError(f"{missing[0]}: missing coverage representation")
    extra = sorted(representations.keys() - required)
    if extra:
        raise CoverageError(f"{extra[0]}: contribution is not eligible through {effective}")

    tasks_requiring_contribution = {ownership[catalog] for catalog in required}
    missing_tasks = sorted(tasks_requiring_contribution - contribution_tasks.keys())
    if missing_tasks:
        raise CoverageError(f"{missing_tasks[0]}: missing owner contributor")

    unique_representations: list[dict[str, Any]] = []
    seen_objects: set[int] = set()
    for catalog in sorted(required, key=lambda value: (task_position[ownership[value]], value)):
        representation = representations[catalog]
        if id(representation) not in seen_objects:
            seen_objects.add(id(representation))
            unique_representations.append(representation)

    guide_active = "IF-004" in task_position and effective_position >= task_position["IF-004"]
    game_tests = sorted(
        assertion
        for representation in unique_representations
        for role, assertion in representation["assertions"].items()
        if role in ("obtain", "use")
    )
    guides = sorted(
        representation["assertions"]["guide"]
        for representation in unique_representations
    )
    return {
        "schema": 1,
        "through": effective,
        "trackedThrough": tracked_through,
        "guideMode": "execute" if guide_active else "staged",
        "representations": unique_representations,
        "assertions": {
            "gameTest": game_tests,
            "client": guides if guide_active else [],
            "stagedGuide": [] if guide_active else guides,
        },
        "limits": {"identifiers": MAX_IDENTIFIERS, "edges": MAX_EDGES},
    }


def _self_test() -> int:
    import test_content_coverage

    suite = unittest.defaultTestLoader.loadTestsFromModule(test_content_coverage)
    result = unittest.TextTestRunner(verbosity=2).run(suite)
    return 0 if result.wasSuccessful() else 1


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--self-test", action="store_true")
    parser.add_argument("--through")
    parser.add_argument("--output", type=Path)
    args = parser.parse_args(argv)
    if args.self_test:
        if args.through or args.output:
            parser.error("--self-test cannot be combined with other options")
        return _self_test()
    try:
        result = validate(ROOT, args.through)
        encoded = json.dumps(result, indent=2, sort_keys=True) + "\n"
        if args.output:
            args.output.parent.mkdir(parents=True, exist_ok=True)
            args.output.write_text(encoded, encoding="utf-8")
        print(
            f"Content coverage through {result['through']}: "
            f"{len(result['representations'])} representations, "
            f"{len(result['assertions']['gameTest'])} GameTest assertions, "
            f"{len(result['assertions']['client'])} active guide assertions."
        )
        return 0
    except CoverageError as exc:
        print(f"content coverage: {exc}", file=sys.stderr)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
