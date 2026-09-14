"""Task packets and independent-review acceptance; no process or Git side effects."""

import fnmatch
import hashlib
import json
from pathlib import PurePosixPath
import re

FIELDS = {"Milestone", "Owner", "Depends", "Spec", "Blocks", "Scope", "Contract",
          "Red", "Accept", "Bounds", "Evidence"}
CHECKS = ("correctness", "placement", "simplicity", "scope", "boundaries",
          "tests", "comments", "performance", "evidence")
CONTROL = (".ktask/", "scripts/ktask_", "scripts/test_ktask_", "AGENTS.md",
           "docs/DEVELOPER_GUIDE.md", "docs/ARCHITECTURE.md", "docs/PERFORMANCE.md")


def module_scope(owner):
    """One canonical path convention, shared by packets and delivery enforcement."""
    if not re.fullmatch(r"[a-z]+", owner):
        raise ValueError("Invalid module scope")
    package = "org/jd/infestusfrontier/"
    paths = [f"core/src/main/java/{package}{owner}/**",
             f"core/src/test/java/{package}{owner}/**",
             f"src/main/java/{package}{owner}/**",
             f"src/main/java/{package}platform/{owner}/**",
             f"src/testMod/java/{package}testmod/{owner}/**",
             f"src/testMod/java/{package}testmod/platform/{owner}/**",
             f"src/testMod/resources/{owner}/**", f"assets/provenance/{owner}.md",
             f"src/main/java/{package}InfestusFrontier.java",
             "src/main/resources/assets/infestusfrontier/lang/en_us.json",
             "src/main/resources/data/minecraft/tags/block/mineable/*.json",
             "src/main/resources/data/minecraft/tags/block/needs_*_tool.json"]
    for kind in ("models/block", "models/item", "textures", "geo", "animations", "blockstates"):
        paths.append(f"src/main/resources/assets/infestusfrontier/{kind}/{owner}/**")
    for kind in ("recipe", "loot_table", "advancement", "tags/block", "tags/item",
                 "tags/fluid", "tags/entity_type", "modonomicon/books/the_waking_genome"):
        paths.append(f"src/main/resources/data/infestusfrontier/{kind}/{owner}/**")
    if owner == "campaign":
        return [path for path in paths if not path.startswith(("src/main/", "core/src/main/"))]
    return paths


def parse_tasks(content):
    """Read canonical, ordered packets; runtime status markers are not specifications."""
    tasks, known = [], set()
    for block in re.split(r"^---\s*$", content, flags=re.M):
        lines = [line for line in block.splitlines() if line.strip() and not line.startswith("#")]
        if not lines:
            continue
        match = re.fullmatch(r"(IF-\d{3}) (.+)", lines[0])
        if not match or match[1] in known:
            raise ValueError("Invalid or duplicate task ID")
        fields = {}
        for line in lines[1:]:
            key, separator, value = line.partition(": ")
            if not separator or key not in FIELDS or key in fields or not value.strip():
                raise ValueError(f"{match[1]} invalid field: {line}")
            fields[key] = value
        if fields.keys() != FIELDS:
            raise ValueError(f"{match[1]} missing fields: {FIELDS - fields.keys()}")
        dependencies = [] if fields["Depends"] == "none" else fields["Depends"].split(", ")
        if any(dep not in known for dep in dependencies):
            raise ValueError(f"{match[1]} dependency must precede consumer")
        scope = json.loads(fields["Scope"])
        if not isinstance(scope, list) or not scope or any(
                not isinstance(path, str) or not path or path.startswith(("/", "*"))
                or ".." in PurePosixPath(path).parts for path in scope):
            raise ValueError(f"{match[1]} invalid scope")
        expanded = []
        for rule in scope:
            if rule.startswith("@module:"):
                expanded.extend(module_scope(rule[8:]))
            elif rule.startswith("@"):
                raise ValueError(f"{match[1]} unknown scope profile")
            else:
                expanded.append(rule)
        if not set(fields["Evidence"].split(", ")) <= {"rules", "game", "visual", "integration", "soak"}:
            raise ValueError(f"{match[1]} unknown evidence type")
        tasks.append(dict(fields, id=match[1], title=match[2], scope=expanded,
                          dependencies=dependencies, body="\n".join(lines),
                          digest=hashlib.sha256("\n".join(lines).encode()).hexdigest()))
        known.add(match[1])
    if not tasks:
        raise ValueError("Empty implementation queue")
    return tasks


def check_scope(task, paths, allowed_controls=()):
    """Every changed path must be explicitly allowed; workers cannot rewrite their gates."""
    for path in paths:
        if ((path.startswith(CONTROL) and path not in allowed_controls) or ".." in PurePosixPath(path).parts
                or not any(fnmatch.fnmatchcase(path, rule) for rule in task["scope"])):
            raise ValueError(f"{task['id']} out-of-scope change: {path}")


def check_review(review, task_id, candidate):
    """A fresh verdict must cover every review responsibility without unresolved findings."""
    if (not isinstance(review, dict) or review.get("task") != task_id or review.get("candidate") != candidate
            or review.get("verdict") != "accept" or review.get("findings") != []
            or set(review.get("checks", {})) != set(CHECKS)):
        raise ValueError("Independent review rejected, incomplete, or stale")
    for value in review['checks'].values():
        if (not isinstance(value, dict) or value.get('status') != 'pass'
                or not isinstance(value.get('evidence'), str) or not value['evidence'].strip()):
            raise ValueError('Independent review needs supporting evidence for every check')


def review_schema():
    return {
        "type": "object", "additionalProperties": False,
        "required": ["task", "candidate", "verdict", "checks", "findings"],
        "properties": {
            "task": {"type": "string"}, "candidate": {"type": "string"},
            "verdict": {"type": "string", "enum": ["accept", "reject"]},
            "checks": {"type": "object", "additionalProperties": False,
                       "required": list(CHECKS),
                       "properties": {key: {"type": "object", "additionalProperties": False,
                                             "required": ["status", "evidence"],
                                             "properties": {
                                                 "status": {"type": "string", "enum": ["pass", "fail"]},
                                                 "evidence": {"type": "string"}}}
                                      for key in CHECKS}},
            "findings": {"type": "array", "items": {"type": "string"}},
        },
    }
