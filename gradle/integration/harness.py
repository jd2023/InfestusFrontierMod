#!/usr/bin/env python3
"""Bounded process supervision and evidence evaluation for bootstrap runs."""

from __future__ import annotations

import argparse
import hashlib
import json
import os
import re
import shutil
import socket
import struct
import zlib
import zipfile
import subprocess
import sys
import time
import uuid
from pathlib import Path
from typing import Any


if __package__:
    from .supervisor import (
        HarnessFailure,
        Supervisor,
        _process_alive,
        group_members,
        MAX_LOG,
    )
else:
    from supervisor import (
        HarnessFailure,
        Supervisor,
        _process_alive,
        group_members,
        MAX_LOG,
    )


PROFILES = {"required", "jei", "curios", "combined"}
REQUIRED_MODS = {
    "minecraft": "1.21.1",
    "neoforge": "21.1.249",
    "infestusfrontier": "0.1.0-dev.1",
    "modonomicon": "1.120.4",
    "geckolib": "4.9.2",
}
OPTIONAL_MODS = {"jei": "19.56.0.438", "curios": "9.5.1+1.21.1"}
PROFILE_TRANSITIVE_MODS = {"jei": {"mezz_config": "0.5.6"}}
DEFAULT_DEADLINES = {
    "readiness": 120,
    "join": 30,
    "capture": 30,
    "disconnect": 30,
    "shutdown": 30,
    "cleanup": 5,
    "profileSmoke": 180,
    "clientScenario": 420,
}
MAX_RESULT = 1024 * 1024
MAX_CAPTURE = 4 * 1024 * 1024
IDENTITY = re.compile(r"INFESTUS_INTEGRATION_RUNTIME\s+([^\r\n]+)")
CONTENT_MARKER = "INFESTUS_CONTENT_ASSERTION"
GUIDE_MARKER = "INFESTUS_GUIDE_ASSERTION"


def _sha256(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as stream:
        for block in iter(lambda: stream.read(65536), b""):
            digest.update(block)
    return digest.hexdigest()


def _candidate_identity(root: Path) -> str:
    supplied = os.environ.get("KTASK_CANDIDATE_ID")
    if supplied:
        return supplied
    head = subprocess.run(
        ["git", "rev-parse", "HEAD"],
        cwd=root,
        text=True,
        capture_output=True,
        check=True,
    ).stdout.strip()
    diff = subprocess.run(
        ["git", "diff", "--binary", "HEAD"], cwd=root, capture_output=True, check=True
    ).stdout
    digest = hashlib.sha256(diff)
    untracked = subprocess.run(
        ["git", "ls-files", "--others", "--exclude-standard", "-z"],
        cwd=root,
        capture_output=True,
        check=True,
    ).stdout.split(b"\0")
    for raw in sorted(path for path in untracked if path):
        path = root / os.fsdecode(raw)
        digest.update(raw)
        digest.update(b"\0")
        digest.update(path.read_bytes())
    return f"{head}+dirty-{digest.hexdigest()[:16]}"


def _write_result(path: Path, result: dict[str, Any]) -> None:
    encoded = json.dumps(result, indent=2, sort_keys=True) + "\n"
    if len(encoded.encode()) > MAX_RESULT:
        raise HarnessFailure("result JSON exceeds 1 MiB")
    path.write_text(encoded, encoding="utf-8")


def validate_profile_file(
    path: Path | None, profile: str, scenario: str
) -> dict[str, Any]:
    if profile not in PROFILES:
        raise HarnessFailure(f"invalid profile {profile!r}")
    if scenario != "bootstrap":
        raise HarnessFailure(f"invalid scenario {scenario!r}")
    defaults: dict[str, Any] = {
        "schema": 1,
        "scenario": "bootstrap",
        "profiles": ["required", "jei", "curios", "combined"],
        "seed": 11,
        "deadlinesSeconds": DEFAULT_DEADLINES.copy(),
    }
    if path is None:
        return defaults
    try:
        value = json.loads(path.read_text(encoding="utf-8"))
    except (OSError, json.JSONDecodeError) as exc:
        raise HarnessFailure(f"invalid integration profile file: {exc}") from exc
    if value != defaults:
        raise HarnessFailure(
            "integration profile file does not match schema 1 bootstrap contract"
        )
    if profile not in value["profiles"] or scenario != value["scenario"]:
        raise HarnessFailure(
            "CLI profile/scenario disagree with integration profile file"
        )
    return value


def load_content_requirements(path: Path | None) -> dict[str, list[str]]:
    empty = {"gameTest": [], "client": []}
    if path is None:
        return empty
    try:
        if path.stat().st_size > 1024 * 1024:
            raise HarnessFailure("content requirements exceed 1 MiB")
        value = json.loads(path.read_text(encoding="utf-8"))
        assertions = value["assertions"]
    except (OSError, json.JSONDecodeError, KeyError, TypeError) as exc:
        raise HarnessFailure(f"invalid content requirements: {exc}") from exc
    probes_by_role = value.get("harnessAssertions", {})
    if not isinstance(assertions, dict) or not isinstance(probes_by_role, dict):
        raise HarnessFailure("invalid content assertion requirements")
    result = {}
    for role in empty:
        names = assertions.get(role)
        probes = probes_by_role.get(role, [])
        if isinstance(names, list) and isinstance(probes, list):
            names = names + probes
        else:
            raise HarnessFailure(f"invalid {role} content assertion requirements")
        if (
            not isinstance(names, list)
            or len(names) > 4096
            or any(
                not isinstance(name, str)
                or not re.fullmatch(r"[a-z0-9_.-]+:[a-z0-9_./-]+", name)
                for name in names
            )
            or len(names) != len(set(names))
        ):
            raise HarnessFailure(f"invalid {role} content assertion requirements")
        result[role] = names
    return result


def _named_assertions(log: str, names: list[str], marker: str) -> dict[str, bool]:
    observed = set(
        re.findall(re.escape(marker) + r" name=([a-z0-9_.-]+:[a-z0-9_./-]+)", log)
    )
    return {f"content:{name}": name in observed for name in names}


def _png_dimensions(path: Path) -> tuple[int, int]:
    """Decode the bounded, non-interlaced RGB(A) PNG format emitted by Minecraft."""
    if path.stat().st_size > MAX_CAPTURE:
        raise HarnessFailure(f"capture oversized: {path}")
    data = path.read_bytes()
    try:
        if data[:8] != b"\x89PNG\r\n\x1a\n":
            raise ValueError("signature")
        offset, compressed, dimensions, ended = 8, bytearray(), None, False
        while offset < len(data):
            (size,) = struct.unpack_from(">I", data, offset)
            tag = data[offset + 4 : offset + 8]
            payload = data[offset + 8 : offset + 8 + size]
            (crc,) = struct.unpack_from(">I", data, offset + 8 + size)
            if zlib.crc32(tag + payload) != crc:
                raise ValueError("CRC")
            if tag == b"IHDR":
                if offset != 8 or size != 13:
                    raise ValueError("IHDR order")
                width, height, depth, color, method, filtering, interlace = (
                    struct.unpack(">IIBBBBB", payload)
                )
                if (
                    (width, height) != (1280, 720)
                    or depth != 8
                    or color not in (2, 6)
                    or (method, filtering, interlace) != (0, 0, 0)
                ):
                    raise ValueError("expected 1280x720 RGB(A) PNG")
                dimensions = width, height
                channels = 3 if color == 2 else 4
            elif tag == b"IDAT":
                if dimensions is None:
                    raise ValueError("missing IHDR")
                compressed.extend(payload)
            elif tag == b"IEND":
                if size or offset + 12 != len(data):
                    raise ValueError("IEND")
                ended = True
            elif tag[:1].isupper():
                raise ValueError("unsupported critical chunk")
            offset += size + 12
        if not ended or not dimensions:
            raise ValueError("truncated PNG")
        row_size = width * channels
        expected = (row_size + 1) * height
        decoder = zlib.decompressobj()
        pixels = decoder.decompress(compressed, expected + 1)
        if (
            len(pixels) != expected
            or not decoder.eof
            or decoder.unused_data
            or decoder.unconsumed_tail
        ):
            raise ValueError("truncated/oversized pixels")
        # Every scanline must have a valid PNG reconstruction filter.
        if any(pixels[row * (row_size + 1)] > 4 for row in range(height)):
            raise ValueError("invalid scanline filter")
        return dimensions
    except (ValueError, struct.error, zlib.error, UnboundLocalError) as exc:
        raise HarnessFailure(f"capture is not a readable PNG: {path}: {exc}") from exc


CLIENT_ASSERTIONS = {
    "serverReady",
    "serverPlayerJoin",
    "serverPlayerDisconnect",
    "clientPlayEntry",
    "clientPlayExit",
    "cleanStop",
    "releaseJarOnly",
    "resourcesClean",
    "cameraRendered",
    "portReleased",
}
SMOKE_ASSERTIONS = {"gameTest", "realServerState", "cleanStop", "ordinaryExit"}
NEGATIVE_ASSERTIONS = {
    "namedLoaderDiagnostic",
    "refusedBeforeReadiness",
    "expectedLoaderRefusal",
    "cleanStop",
}
CAPTURES = ("bootstrap-title.png", "bootstrap-world.png")


def _loader_assertions(log, omission, cleanup):
    return dict(
        namedLoaderDiagnostic=bool(
            re.search(
                r"Mod ID: ['\"]"
                + re.escape(omission)
                + r"['\"][^\n]*Requested by: ['\"]infestusfrontier['\"]",
                log,
            )
        ),
        refusedBeforeReadiness="Started game test server" not in log
        and "INFESTUS_INTEGRATION_RUNTIME" not in log,
        expectedLoaderRefusal="Missing or unsupported mandatory dependencies" in log
        and "Mod loading has failed" in log,
        cleanStop=cleanup,
    )


class ResultEvaluator:
    def evaluate(self, result, *, require_captures=False):
        if result.get("schema") != 1 or result.get("success") is not True:
            raise HarnessFailure(
                f"run failed: {result.get('failure', 'invalid schema or status')}"
            )
        profile = result.get("profile", "required")
        validate_profile_file(None, profile, result.get("scenario", "bootstrap"))
        command = result.get("command", "profileSmoke")
        if command not in ("profileSmoke", "captureClient", "packagedServerSmoke"):
            raise HarnessFailure("invalid result command")
        omission = result.get("omittedRequired")
        if omission and (
            command != "profileSmoke"
            or profile != "required"
            or omission not in ("modonomicon", "geckolib")
        ):
            raise HarnessFailure("invalid omission fixture")
        roles = {"server"} if command == "profileSmoke" else {"server", "client"}
        if result.get("multiplayer") is True:
            roles.add("observer")
        runtime = result.get("runtimeMods", {})
        expected = REQUIRED_MODS.copy()
        expected["infestusfrontier"] = result.get(
            "builtModVersion", REQUIRED_MODS["infestusfrontier"]
        )
        expected.update(
            {
                key: value
                for key, value in OPTIONAL_MODS.items()
                if profile in (key, "combined")
            }
        )
        for selected, transitive in PROFILE_TRANSITIVE_MODS.items():
            if profile in (selected, "combined"):
                expected.update(transitive)
        if not omission:
            if set(runtime) != roles:
                raise HarnessFailure("runtime mod process roles missing or unexpected")
            for role, mods in runtime.items():
                for mod, version in expected.items():
                    if mods.get(mod) != version:
                        raise HarnessFailure(
                            f"{role} mod {mod} expected {version}, got {mods.get(mod)}"
                        )
                fixtures = (
                    {"infestusfrontier_tests": "1.0.0"}
                    if command == "profileSmoke"
                    else (
                        {"infestusfrontier_client": "1.0.0"} if role in ("client", "observer") else {}
                    )
                )
                if mods != expected | fixtures:
                    raise HarnessFailure(
                        f"{role} optional/fixture/transitive mod identities do not match pinned profile"
                    )
        elif runtime:
            raise HarnessFailure(
                "negative loader fixture must refuse before runtime initialization"
            )
        required = (
            NEGATIVE_ASSERTIONS
            if omission
            else (SMOKE_ASSERTIONS if command == "profileSmoke" else CLIENT_ASSERTIONS)
        )
        if result.get("multiplayer") is True:
            required = required | {"probeProgressConsistent", "probeReopened", "probeOwnerRefused", "probeCancelledBoth",
                                   "observerJoined", "observerLeft", "observerResourcesClean", "probeIdleSilent"}
        assertions = result.get("assertions", {})
        if not required.issubset(assertions) or not all(
            value is True for value in assertions.values()
        ):
            raise HarnessFailure(
                "missing or failed assertions: "
                + ", ".join(
                    sorted(
                        required - assertions.keys()
                        | {k for k, v in assertions.items() if v is not True}
                    )
                )
            )
        children = result.get("children", [])
        child_roles = [child.get("role") for child in children]
        if (
            len(set(child_roles)) != len(child_roles)
            or not roles.issubset(child_roles)
            or set(child_roles) - roles - {"installer"}
        ):
            raise HarnessFailure("missing or unexpected child roles")
        for child in children:
            code = child.get("exitCode")
            expected_exit = type(code) is int and (
                code in (0, 1) if omission and child["role"] == "server" else code == 0
            )
            if (
                not expected_exit
                or child.get("alive") is not False
                or child.get("groupReaped") is not True
                or type(child.get("pid")) is not int
                or child["pid"] <= 0
                or _process_alive(child["pid"])
                or group_members(child["pid"])
            ):
                raise HarnessFailure(
                    f"child {child.get('role')} exit/reaping incomplete: {code}"
                )
        for field in ("runId", "candidateIdentity", "javaVersion", "builtModVersion"):
            if not isinstance(result.get(field), str) or not result[field]:
                raise HarnessFailure(f"missing metadata: {field}")
        if not re.search(r"\b21(?:[.\"]|\b)", result["javaVersion"]):
            raise HarnessFailure("Java 21 is required")
        phases = {"shutdown", "cleanup", "total"} | (
            {"loaderRefusal"}
            if omission
            else {"readiness", "gameTest"}
            if command == "profileSmoke"
            else {
                "readiness",
                "clientReadiness",
                "titleCapture",
                "join",
                "capture",
                "disconnect",
            }
        )
        durations = result.get("phaseDurationsSeconds", {})
        if not phases.issubset(durations) or any(
            type(n) not in (int, float) or not 0 <= n < float("inf")
            for n in durations.values()
        ):
            raise HarnessFailure("missing or invalid phase durations")
        phase_bounds = {
            "readiness": 120,
            "clientReadiness": 120,
            "shutdown": 30,
            "cleanup": 5,
            "titleCapture": 30,
            "join": 30,
            "capture": 30,
            "disconnect": 30,
            "setup": 600,
            "total": 180 if command == "profileSmoke" else 420,
        }
        for name, seconds in durations.items():
            bound = phase_bounds.get(name)
            if name.startswith("probe-") or name in ("observerJoin", "observerDisconnect"):
                bound = 30
            elif name in ("observerReadiness", "observerDisplay"):
                bound = 120
            if bound is not None and seconds > bound:
                raise HarnessFailure(f"{name}: exceeded duration bound")
        if command != "profileSmoke" and not re.fullmatch(
            "[a-f0-9]{64}", result.get("releaseJarSha256", "")
        ):
            raise HarnessFailure("release JAR digest missing")
        artifacts, digests = (
            result.get("artifacts", {}),
            result.get("artifactDigests", {}),
        )
        required_files = {f"{role}.log" for role in child_roles}
        if command != "profileSmoke" or require_captures:
            required_files.update(CAPTURES)
            required_files.update(result.get("requiredCaptures", []))
        if not required_files.issubset(artifacts) or set(artifacts) != set(digests):
            raise HarnessFailure("missing capture/log artifacts or digests")
        started = result.get("runStartedNs")
        if type(started) is not int or started <= 0:
            raise HarnessFailure("missing run start time")
        for name, raw in artifacts.items():
            path = Path(raw)
            cap = MAX_CAPTURE if name.endswith(".png") else MAX_LOG
            if (
                not path.is_file()
                or not 0 < path.stat().st_size <= cap
                or path.stat().st_mtime_ns < started
            ):
                raise HarnessFailure(
                    f"capture/log {name} is missing, stale, empty or oversized"
                )
            if digests[name] != _sha256(path):
                raise HarnessFailure(f"artifact {name} digest mismatch")
            if name.endswith(".png"):
                _png_dimensions(path)
        if omission and not all(
            _loader_assertions(
                Path(artifacts["server.log"]).read_text(), omission, True
            ).values()
        ):
            raise HarnessFailure("negative fixture lacks the named loader refusal")


def publish_result(output, result):
    result["success"] = result.get("failure") is None
    if result["success"]:
        try:
            ResultEvaluator().evaluate(result)
        except (HarnessFailure, OSError, ValueError) as exc:
            result["failure"] = f"evaluation: {exc}"
            result["success"] = False
    _write_result(output / "result.json", result)
    return 0 if result["success"] else 1


def _parse_runtime(log):
    matches = IDENTITY.findall(log)
    if not matches:
        raise HarnessFailure("runtime identity marker is missing")
    return dict(item.split("=", 1) for item in matches[-1].strip().split(","))


def _free_loopback_port():
    with socket.socket() as listener:
        listener.bind(("127.0.0.1", 0))
        return listener.getsockname()[1]


def _port_released(port):
    with socket.socket() as probe:
        probe.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
        try:
            probe.bind(("127.0.0.1", port))
            return True
        except OSError:
            return False


def _offline_uuid(name):
    digest = bytearray(hashlib.md5(f"OfflinePlayer:{name}".encode()).digest())
    digest[6] = (digest[6] & 0x0F) | 0x30
    digest[8] = (digest[8] & 0x3F) | 0x80
    return str(uuid.UUID(bytes=bytes(digest)))


def _read_spec(path):
    spec = json.loads(path.read_text())
    if (
        spec.get("schema") != 1
        or not isinstance(spec.get("command"), list)
        or not all(isinstance(v, str) for v in spec["command"])
    ):
        raise HarnessFailure("invalid launch specification")
    return spec["command"]


def _clear_output(output, captures=()):
    output.mkdir(parents=True, exist_ok=True)
    for name in (
        "result.json",
        "server.log",
        "client.log",
        "installer.log",
        "success.receipt",
    ) + CAPTURES + tuple(captures):
        (output / name).unlink(missing_ok=True)


def _base_result(
    root,
    output,
    supervisor,
    command,
    profile,
    started,
    failure,
    assertions,
    omission=None,
    release=None,
    fixture=False,
    captures=(),
):
    runtime, artifacts, digests = {}, {}, {}
    for role, child in supervisor.children.items():
        if role != "installer" and not omission:
            try:
                runtime[role] = _parse_runtime(child.text())
            except HarnessFailure as exc:
                failure = failure or str(exc)
        if child.log_path.is_file():
            name = f"{role}.log"
            artifacts[name], digests[name] = (
                str(child.log_path.resolve()),
                _sha256(child.log_path),
            )
    for name in CAPTURES + tuple(captures):
        if (output / name).is_file():
            artifacts[name], digests[name] = (
                str((output / name).resolve()),
                _sha256(output / name),
            )
    version = REQUIRED_MODS["infestusfrontier"]
    if not fixture:
        version = re.search(
            r"^mod_version=(.+)$", (root / "gradle.properties").read_text(), re.M
        )[1]
    return dict(
        schema=1,
        runId=supervisor.run_id,
        runStartedNs=started,
        command=command,
        profile=profile,
        scenario="bootstrap",
        omittedRequired=omission,
        javaVersion='openjdk version "21" (fixture)'
        if fixture
        else subprocess.run(
            ["java", "-version"], capture_output=True, text=True, timeout=5
        ).stderr.splitlines()[0],
        candidateIdentity="fixture" if fixture else _candidate_identity(root),
        builtModVersion=version,
        releaseJarSha256=_sha256(release) if release else None,
        runtimeMods=runtime,
        assertions=assertions,
        children=[
            dict(
                role=role,
                pid=child.process.pid,
                exitCode=child.process.returncode,
                alive=_process_alive(child.process.pid),
                groupReaped=not group_members(child.process.pid),
            )
            for role, child in supervisor.children.items()
        ],
        phaseDurationsSeconds=supervisor.durations,
        artifacts=artifacts,
        artifactDigests=digests,
        requiredCaptures=list(captures),
        failure="; ".join(
            str(item) for item in (failure, supervisor.cleanup_failure) if item
        )
        or None,
    )


def _server_command(server_dir):
    args = server_dir / "libraries/net/neoforged/neoforge/21.1.249/unix_args.txt"
    if not args.is_file():
        raise HarnessFailure("installed server unix_args.txt missing")
    return [
        "java",
        "-Dinfestus.integration=true",
        "@user_jvm_args.txt",
        f"@{args.relative_to(server_dir)}",
        "nogui",
    ]


def _release_clean(path):
    with zipfile.ZipFile(path) as jar:
        return not any(
            "/testmod/" in n
            or "infestusfrontier_tests" in n
            or "infestusfrontier_client" in n
            for n in jar.namelist()
        )


def _prepare_server(s, staged, output, multiplayer=False):
    server_dir, client_dir = s.run_dir / "server", s.run_dir / "client"
    server_dir.mkdir()
    client_dir.mkdir()
    installer = staged / "installer/neoforge-installer.jar"
    releases = list((staged / "mods").glob("infestusfrontier-*.jar"))
    if not installer.is_file() or len(releases) != 1:
        raise HarnessFailure("setup: staged installer or release JAR missing")
    setup_started = s.clock()
    s.deadline = setup_started + 600
    child = s.start(
        "installer",
        ["java", "-jar", str(installer), "--installServer", str(server_dir)],
        s.run_dir,
        output / "installer.log",
    )
    s.phase("setup", 600, lambda: child.finished())
    if child.process.returncode != 0 or not child.reap():
        raise HarnessFailure("setup: installer failed or left children")
    s.begin_runtime()
    mods_dir = server_dir / "mods"
    mods_dir.mkdir()
    for mod in (staged / "mods").glob("*.jar"):
        shutil.copy2(mod, mods_dir / mod.name)
    if not _release_clean(releases[0]):
        raise HarnessFailure("setup: release JAR contains development fixtures")
    port = _free_loopback_port()
    (server_dir / "eula.txt").write_text("eula=true\n")
    (server_dir / "server.properties").write_text(
        "\n".join(
            [
                "allow-flight=false",
                "difficulty=peaceful",
                "enable-command-block=false",
                "enforce-whitelist=true",
                "gamemode=survival",
                "generate-structures=false",
                'generator-settings={"layers":[{"block":"minecraft:bedrock","height":1},{"block":"minecraft:dirt","height":2},{"block":"minecraft:grass_block","height":1}],"biome":"minecraft:plains"}',
                "level-name=world",
                "level-seed=11",
                "level-type=minecraft:flat",
                f"max-players={2 if multiplayer else 1}",
                "online-mode=false",
                "server-ip=127.0.0.1",
                f"server-port={port}",
                "simulation-distance=4",
                "spawn-protection=0",
                "sync-chunk-writes=true",
                "view-distance=4",
                "white-list=true",
                "",
            ]
        )
    )
    (server_dir / "whitelist.json").write_text(
        json.dumps([{"uuid": _offline_uuid(name), "name": name}
                    for name in (["FixturePlayer", "FixtureObserver"] if multiplayer else ["FixturePlayer"])])
    )
    (server_dir / "user_jvm_args.txt").write_text("-Xms512M\n-Xmx1536M\n")
    _prepare_client(client_dir)
    return server_dir, client_dir, releases[0], port


def _prepare_client(directory):
    (directory / "options.txt").write_text(
        "onboardAccessibility:false\npauseOnLostFocus:false\nrenderDistance:4\nsimulationDistance:5\ntutorialStep:none\n"
    )
    # FML 4.0.44's optional splash races union-filesystem teardown during startup.
    # The actual title/world renderer and resource-error checks remain enabled.
    config = directory / "config"
    config.mkdir(exist_ok=True)
    (config / "fml.toml").write_text("earlyWindowControl=false\n")


def _visual_setup(root, requirements, filename="visual-setup.json"):
    """Bounded owner fixtures run only on this harness's disposable server."""
    commands, captures = [], []
    active = set((requirements or {}).get("gameTest", []))
    for path in sorted((root / "src/testMod/resources").glob("*/" + filename)):
        if path.stat().st_size > 8192:
            raise HarnessFailure(f"oversized visual setup: {path}")
        fixture = json.loads(path.read_text())
        batch = fixture.get("commands")
        if (set(fixture) not in ({"requires", "commands"}, {"requires", "commands", "captures"})
                or not isinstance(fixture["requires"], str)
                or not isinstance(batch, list) or not 1 <= len(batch) <= 16
                or any(not isinstance(line, str) or not 1 <= len(line) <= 256
                       or "\n" in line or "\r" in line for line in batch)):
            raise HarnessFailure(f"invalid visual setup: {path}")
        images = fixture.get("captures", [])
        if (not isinstance(images, list) or len(images) > 28
                or any(not isinstance(name, str) or not re.fullmatch(r"[a-z][a-z0-9-]{0,63}\.png", name)
                       or name in CAPTURES for name in images)
                or len(set(images)) != len(images)):
            raise HarnessFailure(f"invalid visual setup captures: {path}")
        if fixture["requires"] in active:
            commands.extend(batch)
            captures.extend(images)
        if len(captures) > 28 or len(set(captures)) != len(captures):
            raise HarnessFailure("excessive or duplicate visual setup captures")
        if len(commands) > 72:
            raise HarnessFailure("excessive visual setup commands")
    return commands, captures


def visual_setup_commands(root, requirements):
    """Bounded commands for active owner fixtures."""
    return _visual_setup(root, requirements)[0]


def visual_setup_captures(root, requirements):
    """Additional required images from the same disposable client and deadline."""
    return _visual_setup(root, requirements)[1]


def _client_lifecycle(
    s, server_command, client_command, server_dir, client_dir, output, port, setup_commands=(), observer_command=None, discovery=False, discovery_setup=()
):
    if len(setup_commands) + len(discovery_setup) > 72:
        raise HarnessFailure("excessive combined setup commands")
    server = s.start("server", server_command, server_dir, output / "server.log")
    s.markers("readiness", s.limits["readiness"], [(server, "Done (")])
    env = os.environ | {
        "INFESTUS_CAPTURE_DIR": str(output),
        "INFESTUS_SERVER": f"127.0.0.1:{port}",
    }
    if observer_command:
        env.update(INFESTUS_PROBE_ROLE="owner", INFESTUS_COORDINATION=str(output))
    client = s.start("client", client_command, client_dir, output / "client.log", env)
    observer = None
    if observer_command:
        display_file = s.run_dir / "shared-display.json"
        s.phase("observerDisplay", s.limits["readiness"], display_file.is_file)
        if display_file.stat().st_size > 4096:
            raise HarnessFailure("observer: oversized display configuration")
        shared_display = json.loads(display_file.read_text())
        if set(shared_display) != {"DISPLAY", "XAUTHORITY"}:
            raise HarnessFailure("observer: invalid display configuration")
        observer_dir = s.run_dir / "observer"
        observer_dir.mkdir()
        _prepare_client(observer_dir)
        observer = s.start("observer", observer_command, observer_dir, output / "observer.log",
                           env | shared_display | {"INFESTUS_PROBE_ROLE": "observer", "INFESTUS_CAPTURE_DIR": str(output / "observer")})
    s.markers(
        "clientReadiness",
        s.limits["readiness"],
        [(client, "INFESTUS_CLIENT_TITLE_READY")],
    )
    s.markers(
        "titleCapture",
        s.limits["capture"],
        [(client, "INFESTUS_CLIENT_CAPTURE name=bootstrap-title.png")],
    )
    identity = f"name=FixturePlayer uuid={_offline_uuid('FixturePlayer')}"
    s.markers(
        "join",
        s.limits["join"],
        [
            (server, f"INFESTUS_INTEGRATION_PLAYER_JOIN {identity}"),
            (client, f"INFESTUS_CLIENT_PLAY_ENTER {identity}"),
        ],
    )
    if observer:
        s.markers("observerReadiness", s.limits["readiness"], [(observer, "INFESTUS_CLIENT_TITLE_READY")])
        s.markers("observerJoin", s.limits["join"], [
            (server, f"INFESTUS_INTEGRATION_PLAYER_JOIN name=FixtureObserver uuid={_offline_uuid('FixtureObserver')}"),
            (observer, "INFESTUS_PROBE_READY role=observer")])
    if discovery:
        _discovery_actions(s, server, client, output, discovery_setup)
        server.send("gamemode creative @a")
    for command in setup_commands:
        server.send(command)
    if observer:
        server.send("tp FixtureObserver -0.5 -60 0.5 0 15")
    s.markers(
        "capture",
        s.limits["capture"],
        [(client, "INFESTUS_CLIENT_CAPTURE name=bootstrap-world.png")],
    )
    if observer:
        _probe_actions(s, client, observer, output)
    s.markers(
        "disconnect",
        s.limits["disconnect"],
        [
            (client, "INFESTUS_CLIENT_PLAY_EXIT"),
            (server, f"INFESTUS_INTEGRATION_PLAYER_LEAVE {identity}"),
        ],
    )


def _discovery_actions(s, server, client, output, setup_commands):
    s.markers("discovery-ready", 30, [(client, "INFESTUS_DISCOVERY_READY")])
    for command in setup_commands:
        server.send(command)
    (output / "discovery-stage").write_text("run")
    s.markers("discovery-survival", 100, [(client, "INFESTUS_DISCOVERY_KEY_WITHOUT_BOOK"),
        (client, "INFESTUS_DISCOVERY_SURVIVAL_RENEWED"), (client, "INFESTUS_DISCOVERY_SURVIVAL_COMPLETE")])


def _observer_command(command, run_dir):
    """Copy only the generated launch arguments into this owned run."""
    result = list(command)
    args = Path(result[-1].removeprefix("@"))
    text = args.read_text()
    if "FixturePlayer" not in text:
        raise HarnessFailure("observer: generated client identity missing")
    args_copy = run_dir / "observer-args.txt"
    args_copy.write_text(text.replace("FixturePlayer", "FixtureObserver")
                         .replace("11111111-1111-1111-1111-111111111111", _offline_uuid("FixtureObserver")))
    result[-1] = "@" + str(args_copy)
    return result


def _probe_snapshots(log):
    return re.findall(r"INFESTUS_PROBE_SNAPSHOT role=\w+ revision=(\d+) work=(\d+) required=(\d+) water=(\d+) state=(\w+) slots=(.+) refusal=\w+", log)


def _probe_consistent(owner_log, observer_log, state):
    owners = [row for row in _probe_snapshots(owner_log) if row[4] == state]
    observers = [row for row in _probe_snapshots(observer_log) if row[4] == state]
    if not owners or not observers:
        return False
    a, b = owners[-1], observers[-1]
    return a[0] == b[0] and a[2:] == b[2:] and abs(int(a[1]) - int(b[1])) <= 10


def _set_probe_phase(output, name):
    temporary = output / "probe-stage.tmp"
    temporary.write_text(name)
    temporary.replace(output / "probe-stage")


def _probe_actions(s, client, observer, output):
    def phase(name, markers):
        _set_probe_phase(output, name)
        s.markers("probe-" + name, 30, markers)
    s.markers("probe-ready", 30, [(client, "INFESTUS_PROBE_READY role=owner"),
                                  (observer, "INFESTUS_PROBE_READY role=observer")])
    phase("open", [(client, "INFESTUS_PROBE_OPEN role=owner"), (observer, "INFESTUS_PROBE_OPEN role=observer")])
    phase("reset", [(client, "INFESTUS_PROBE_RESET role=owner"), (observer, "INFESTUS_PROBE_RESET role=observer")])
    phase("start", [(client, "INFESTUS_PROBE_WORKING role=owner"), (observer, "INFESTUS_PROBE_WORKING role=observer")])
    if not _probe_consistent(client.text(), observer.text(), "WORKING"):
        raise HarnessFailure("probe: clients disagree on authoritative progress")
    phase("close", [(client, "INFESTUS_PROBE_CLOSED role=owner")])
    phase("reopen", [(client, "INFESTUS_PROBE_REOPENED role=owner")])
    phase("wrong-owner", [(observer, "INFESTUS_PROBE_OWNER_REFUSAL")])
    phase("cancel", [(client, "INFESTUS_PROBE_CANCELLED role=owner"), (observer, "INFESTUS_PROBE_CANCELLED role=observer")])
    phase("idle", [(client, "INFESTUS_PROBE_IDLE_SILENT role=owner"), (observer, "INFESTUS_PROBE_IDLE_SILENT role=observer")])
    _set_probe_phase(output, "done")
    s.markers("observerDisconnect", 30, [(observer, "INFESTUS_CLIENT_PLAY_EXIT"),
        (s.children["server"], f"INFESTUS_INTEGRATION_PLAYER_LEAVE name=FixtureObserver uuid={_offline_uuid('FixtureObserver')}")])


def _probe_assertions(s):
    owner = s.children.get("client")
    observer = s.children.get("observer")
    server = s.children.get("server")
    a, b, log = owner.text() if owner else "", observer.text() if observer else "", server.text() if server else ""
    identity = f"name=FixtureObserver uuid={_offline_uuid('FixtureObserver')}"
    return dict(
        probeProgressConsistent=_probe_consistent(a, b, "WORKING"),
        probeReopened="INFESTUS_PROBE_REOPENED role=owner" in a,
        probeIdleSilent="INFESTUS_PROBE_IDLE_SILENT role=owner" in a and "INFESTUS_PROBE_IDLE_SILENT role=observer" in b,
        probeOwnerRefused="INFESTUS_PROBE_OWNER_REFUSAL" in b,
        probeCancelledBoth="INFESTUS_PROBE_CANCELLED role=owner" in a and "INFESTUS_PROBE_CANCELLED role=observer" in b
                           and _probe_consistent(a, b, "IDLE"),
        observerJoined=f"INFESTUS_INTEGRATION_PLAYER_JOIN {identity}" in log and f"INFESTUS_CLIENT_PLAY_ENTER {identity}" in b,
        observerLeft=f"INFESTUS_INTEGRATION_PLAYER_LEAVE {identity}" in log and "INFESTUS_CLIENT_PLAY_EXIT" in b,
        observerResourcesClean=not re.search(r"missing.?texture|Unable to load model|FileNotFoundException|INFESTUS_CLIENT_FAILURE|/ERROR\]|\[ERROR\]", b, re.I),
    )


def _client_assertions(s, port, release, content_requirements=None):
    server = s.children.get("server")
    client = s.children.get("client")
    slog, clog = server.text() if server else "", client.text() if client else ""
    identity = f"name=FixturePlayer uuid={_offline_uuid('FixturePlayer')}"
    join, leave = (
        f"INFESTUS_INTEGRATION_PLAYER_JOIN {identity}",
        f"INFESTUS_INTEGRATION_PLAYER_LEAVE {identity}",
    )
    assertions = dict(
        serverReady="Done (" in slog,
        serverPlayerJoin=join in slog,
        serverPlayerDisconnect=leave in slog
        and slog.find(leave) > slog.find(join) >= 0,
        clientPlayEntry=f"INFESTUS_CLIENT_PLAY_ENTER {identity}" in clog,
        clientPlayExit=clog.find("INFESTUS_CLIENT_PLAY_EXIT")
        > clog.find("INFESTUS_CLIENT_PLAY_ENTER")
        >= 0,
        cleanStop=s.cleanup_ok and "Stopping server" in slog,
        releaseJarOnly=bool(release and _release_clean(release))
        and "infestusfrontier_tests" not in slog
        and "infestusfrontier_client" not in slog,
        resourcesClean=not re.search(
            r"missing.?texture|Unable to load model|FileNotFoundException|INFESTUS_CLIENT_FAILURE|/ERROR\]|\[ERROR\]",
            clog,
            re.I,
        ),
        cameraRendered="INFESTUS_CLIENT_CAMERA_RENDERED yaw=0.0 pitch=15.0" in clog,
        portReleased=port is not None and _port_released(port),
    )
    assertions.update(
        _named_assertions(
            clog, (content_requirements or {}).get("client", []), GUIDE_MARKER
        )
    )
    return assertions


def run_client_scenario(
    root,
    staged,
    launch_spec,
    output,
    profile,
    capture,
    *,
    fixture=None,
    limits=None,
    content_requirements=None,
):
    validate_profile_file(None, profile, "bootstrap")
    multiplayer = not fixture and "infestusfrontier_tests:interaction.synaptic_probe.use" in (content_requirements or {}).get("gameTest", [])
    if (root / "build/integration/runs").resolve() in output.resolve().parents:
        raise HarnessFailure("evidence output must be outside owned run directories")
    limits = limits or DEFAULT_DEADLINES
    command = "captureClient" if capture else "packagedServerSmoke"
    started, failure, release, port = time.time_ns(), None, None, None
    captures = []
    s = Supervisor(root, f"{command}-{uuid.uuid4()}", limits, limits["clientScenario"], max_children=4 if multiplayer else 3)

    def finish(failure):
        result = _base_result(
            root,
            output,
            s,
            command,
            profile,
            started,
            failure,
            _client_assertions(s, port, release, content_requirements),
            release=release,
            fixture=bool(fixture),
            captures=captures,
        )
        if multiplayer:
            result["multiplayer"] = True
            result["assertions"].update(_probe_assertions(s))
        return publish_result(output, result)

    s.on_finished = finish
    try:
        with s:
            captures = [] if fixture else visual_setup_captures(root, content_requirements)
            _clear_output(output, captures)
            (output / "discovery-stage").unlink(missing_ok=True)
            observer_command = None
            if fixture:
                server_dir = client_dir = s.run_dir
                release = s.run_dir / "release.jar"
                with zipfile.ZipFile(release, "w") as jar:
                    jar.writestr("fixture-release", "fixture")
                port = _free_loopback_port()
                script = str(Path(__file__).parent / "fixtures/fixture_child.py")
                server_command = [
                    sys.executable,
                    script,
                    fixture,
                    "server",
                    str(port),
                    str(output),
                ]
                client_command = [
                    sys.executable,
                    script,
                    fixture,
                    "client",
                    str(port),
                    str(output),
                ]
                if fixture == "setup-failure":
                    installer = s.start(
                        "installer",
                        server_command,
                        server_dir,
                        output / "installer.log",
                    )
                    s.markers("setup", 1, [(installer, "Done (")])
                    raise HarnessFailure("setup: deliberate failure after launch")
            else:
                server_dir, client_dir, release, port = _prepare_server(
                    s, staged, output, multiplayer
                )
                server_command = _server_command(server_dir)
                client_command = [
                    "xvfb-run",
                    "-a",
                    "-s",
                    "-screen 0 1280x720x24",
                ] + _read_spec(launch_spec)
                if multiplayer:
                    observer_command = _observer_command(_read_spec(launch_spec), s.run_dir)
                    client_command = client_command[:4] + [sys.executable,
                        str(Path(__file__).parent / "display_client.py"), str(s.run_dir / "shared-display.json")
                    ] + client_command[4:]
                    (output / "probe-stage").unlink(missing_ok=True)
            _client_lifecycle(
                s, server_command, client_command, server_dir, client_dir, output, port,
                () if fixture else visual_setup_commands(root, content_requirements), observer_command,
                discovery=not fixture and "infestusfrontier_client:discovery.waking_genome.guide"
                    in (content_requirements or {}).get("client", []),
                discovery_setup=() if fixture else _visual_setup(root, content_requirements, "survival-setup.json")[0],
            )
    except Exception as exc:
        failure = str(exc)
    if s.result_code is None:
        raise HarnessFailure(failure or "ownership/result publication failed")
    return s.result_code


def run_fixture_scenario(scenario, output):
    limits = dict.fromkeys(DEFAULT_DEADLINES, 1) | {
        "cleanup": 2,
        "clientScenario": 12,
        "profileSmoke": 8,
    }
    return run_client_scenario(
        output, None, None, output, "required", True, fixture=scenario, limits=limits
    )


def run_profile_smoke(
    root, launch_spec, output, profile, omission, content_requirements=None
):
    validate_profile_file(None, profile, "bootstrap")
    if omission and (
        omission not in ("modonomicon", "geckolib") or profile != "required"
    ):
        raise HarnessFailure("invalid omission fixture")
    s = Supervisor(
        root,
        f"profileSmoke-{uuid.uuid4()}",
        DEFAULT_DEADLINES,
        DEFAULT_DEADLINES["profileSmoke"],
    )
    started, failure = time.time_ns(), None

    def finish(failure):
        child = s.children.get("server")
        log = child.text() if child else ""
        sys.stdout.write(log)
        if omission:
            assertions = _loader_assertions(log, omission, s.cleanup_ok)
        else:
            assertions = dict(
                gameTest=bool(re.search(r"All [1-9][0-9]* required tests passed", log)),
                realServerState=bool(
                    re.search(
                        r"INFESTUS_INTEGRATION_GAME_STATE dimension=minecraft:overworld gameTime=\d+ loaded=true",
                        log,
                    )
                ),
                cleanStop="Game test server shutting down" in log and s.cleanup_ok,
                ordinaryExit=child is not None and child.process.returncode == 0,
            )
            assertions.update(
                _named_assertions(
                    log,
                    (content_requirements or {}).get("gameTest", []),
                    CONTENT_MARKER,
                )
            )
        return publish_result(
            output,
            _base_result(
                root,
                output,
                s,
                "profileSmoke",
                profile,
                started,
                failure,
                assertions,
                omission,
            ),
        )

    s.on_finished = finish
    try:
        with s:
            _clear_output(output)
            (s.run_dir / "server.properties").write_text(
                "level-name=world\nlevel-seed=11\nmax-tick-time=60000\n"
            )
            child = s.start(
                "server", _read_spec(launch_spec), s.run_dir, output / "server.log"
            )
            if omission:
                s.phase(
                    "loaderRefusal",
                    DEFAULT_DEADLINES["readiness"],
                    lambda: child.finished(),
                )
            else:
                s.markers(
                    "readiness",
                    DEFAULT_DEADLINES["readiness"],
                    [(child, "Started game test server")],
                )
                s.phase(
                    "gameTest",
                    DEFAULT_DEADLINES["profileSmoke"],
                    lambda: bool(
                        re.search(
                            r"All [1-9][0-9]* required tests passed", child.text()
                        )
                    ),
                )
    except Exception as exc:
        failure = str(exc)
    if s.result_code is None:
        raise HarnessFailure(failure or "ownership/result publication failed")
    return s.result_code


def main():
    parser = argparse.ArgumentParser()
    sub = parser.add_subparsers(dest="command", required=True)
    fixture = sub.add_parser("fixture")
    fixture.add_argument("scenario")
    fixture.add_argument("output", type=Path)
    evaluate = sub.add_parser("evaluate")
    evaluate.add_argument("result", type=Path)
    evaluate.add_argument("--captures", action="store_true")
    validate = sub.add_parser("validate-config")
    validate.add_argument("--profile", required=True)
    validate.add_argument("--scenario", required=True)
    validate.add_argument("--file", type=Path)
    for name in ("profile-smoke", "client-scenario"):
        p = sub.add_parser(name)
        for flag in ("root", "launch-spec", "output"):
            p.add_argument("--" + flag, type=Path, required=True)
        p.add_argument("--profile", required=True)
        p.add_argument("--content-requirements", type=Path)
        if name == "client-scenario":
            p.add_argument("--staged", type=Path, required=True)
            p.add_argument("--capture", action="store_true")
        else:
            p.add_argument("--omit")
    args = parser.parse_args()
    if args.command == "fixture":
        return run_fixture_scenario(args.scenario, args.output.resolve())
    if args.command == "evaluate":
        ResultEvaluator().evaluate(
            json.loads(args.result.read_text()), require_captures=args.captures
        )
        return 0
    if args.command == "validate-config":
        validate_profile_file(args.file, args.profile, args.scenario)
        return 0
    if args.command == "profile-smoke":
        content_requirements = load_content_requirements(args.content_requirements)
        return run_profile_smoke(
            args.root.resolve(),
            args.launch_spec.resolve(),
            args.output.resolve(),
            args.profile,
            args.omit,
            content_requirements,
        )
    content_requirements = load_content_requirements(args.content_requirements)
    return run_client_scenario(
        args.root.resolve(),
        args.staged.resolve(),
        args.launch_spec.resolve(),
        args.output.resolve(),
        args.profile,
        args.capture,
        content_requirements=content_requirements,
    )


if __name__ == "__main__":
    try:
        raise SystemExit(main())
    except HarnessFailure as exc:
        print(f"integration harness: {exc}", file=sys.stderr)
        raise SystemExit(1)
