"""Exclusive, deadline-bounded ownership of integration process groups."""

from __future__ import annotations

import ctypes
import fcntl
import json
import os
from pathlib import Path
import signal
import shutil
import subprocess
import threading
import time

MAX_LOG = 8 * 1024 * 1024


class HarnessFailure(RuntimeError):
    pass


def group_members(pgid):
    members = {}
    for entry in Path("/proc").iterdir():
        if not entry.name.isdigit():
            continue
        try:
            fields = (entry / "stat").read_text().rsplit(")", 1)[1].split()
            if int(fields[2]) == pgid:
                members[int(entry.name)] = fields[0]
        except (FileNotFoundError, ProcessLookupError):
            pass
    return members


def _process_alive(pid):
    return Path(f"/proc/{pid}").exists()


class CappedProcess:
    def __init__(self, command, cwd, log_path, env=None):
        self.log_path = log_path
        self.data = bytearray()
        self.overflow = False
        self.read_failure = None
        self.process = subprocess.Popen(
            command,
            cwd=cwd,
            env=env,
            stdin=subprocess.PIPE,
            stdout=subprocess.PIPE,
            stderr=subprocess.STDOUT,
            start_new_session=True,
        )
        self._thread = threading.Thread(target=self._drain, daemon=True)
        self._thread.start()

    def _drain(self):
        try:
            with self.process.stdout as stream:
                while chunk := os.read(stream.fileno(), 65536):
                    available = MAX_LOG - len(self.data)
                    self.data.extend(chunk[:available])
                    if len(chunk) > available:
                        self.overflow = True
        except OSError as exc:
            self.read_failure = str(exc)

    def finished(self):
        return self.process.poll() is not None and not self._thread.is_alive()

    def text(self):
        return self.data.decode("utf-8", errors="replace")

    def check(self, phase):
        if self.overflow:
            raise HarnessFailure(f"{phase}: log overflow")
        if self.read_failure:
            raise HarnessFailure(f"{phase}: log read failed: {self.read_failure}")

    def send(self, line):
        try:
            if self.process.poll() is None:
                self.process.stdin.write((line + "\n").encode())
                self.process.stdin.flush()
        except (BrokenPipeError, OSError):
            pass

    def signal(self, sig):
        try:
            os.killpg(self.process.pid, sig)
        except ProcessLookupError:
            pass

    def reap(self):
        self.process.poll()
        if self.process.returncode is not None:
            while True:
                try:
                    pid, _ = os.waitpid(-self.process.pid, os.WNOHANG)
                    if pid == 0:
                        break
                except ChildProcessError:
                    break
        return not group_members(self.process.pid)

    def save_log(self):
        self.log_path.parent.mkdir(parents=True, exist_ok=True)
        self.log_path.write_bytes(self.data)
        self.process.stdin.close()


class Supervisor:
    """Own all children, phases and one shared shutdown budget for a scenario.

    The clock and process factory are injected for deterministic boundary tests;
    real fixture subprocesses exercise the same production path.
    """

    def __init__(
        self,
        root,
        run_id,
        limits,
        total,
        *,
        clock=time.monotonic,
        sleep=time.sleep,
        process_factory=CappedProcess,
    ):
        self.root, self.run_id, self.limits = root, run_id, limits
        self.clock, self.sleep, self.process_factory = clock, sleep, process_factory
        self.total = total
        self.children = {}
        self.durations = {}
        self.cleanup_ok = False
        self.cleanup_failure = None
        self.on_finished = None
        self.result_code = None
        self.lock = None
        self.old_handlers = {}
        self.launching = False
        self.pending_signal = None
        self.started = clock()
        self.deadline = self.started + total
        self.run_dir = root / "build/integration/runs" / run_id

    def __enter__(self):
        base = self.root / "build/integration"
        base.mkdir(parents=True, exist_ok=True)
        self.lock = (base / "scenario.lock").open("a+")
        try:
            fcntl.flock(self.lock, fcntl.LOCK_EX | fcntl.LOCK_NB)
        except BlockingIOError as exc:
            self.lock.close()
            raise HarnessFailure(
                "ownership: another integration scenario is active"
            ) from exc
        try:
            # Reparent orphaned grandchildren here so forced cleanup also reaps them.
            if ctypes.CDLL(None, use_errno=True).prctl(36, 1, 0, 0, 0) != 0:
                raise HarnessFailure("ownership: cannot enable child subreaping")
            runs = base / "runs"
            runs.mkdir(exist_ok=True)
            for old in runs.iterdir():
                manifest = old / "owner.json"
                if not manifest.is_file():
                    continue
                owned = json.loads(manifest.read_text())
                if any(group_members(pid) for pid in owned["groups"]):
                    raise HarnessFailure(f"ownership: live processes remain in {old}")
                if old.is_symlink() or old.parent != runs:
                    raise HarnessFailure("ownership: invalid run directory")
                shutil.rmtree(old)
            self.run_dir.mkdir()
            self._save_owner()
            for sig in (signal.SIGINT, signal.SIGTERM):
                self.old_handlers[sig] = signal.signal(sig, self._interrupt)
            return self
        except BaseException:
            self.lock.close()
            raise

    def _interrupt(self, sig, frame):
        if self.launching:
            self.pending_signal = sig
        else:
            raise HarnessFailure(f"interrupted: {signal.Signals(sig).name}")

    def _save_owner(self):
        (self.run_dir / "owner.json").write_text(
            json.dumps(
                {
                    "runId": self.run_id,
                    "groups": [c.process.pid for c in self.children.values()],
                }
            )
        )

    def start(self, role, command, cwd, log, env=None):
        if role in self.children or len(self.children) >= 3:
            raise HarnessFailure("ownership: duplicate role or child limit")
        self.launching = True
        try:
            child = self.process_factory(command, cwd, log, env)
            self.children[role] = child
            self._save_owner()
        finally:
            self.launching = False
        if self.pending_signal:
            self._interrupt(self.pending_signal, None)
        return child

    def begin_runtime(self):
        self.started = self.clock()
        self.deadline = self.started + self.total

    def phase_deadline(self, name, limit):
        deadline = min(
            self.clock() + limit,
            self.deadline - self.limits["shutdown"] - self.limits["cleanup"],
        )
        if deadline <= self.clock():
            raise HarnessFailure(f"{name}: scenario deadline exhausted")
        return deadline

    def phase(self, name, limit, condition):
        started = self.clock()
        deadline = self.phase_deadline(name, limit)
        try:
            while True:
                for child in self.children.values():
                    child.check(name)
                helpers = sum(
                    max(0, len(group_members(c.process.pid)) - 1)
                    for c in self.children.values()
                )
                if helpers > 4:
                    raise HarnessFailure(f"{name}: helper process limit exceeded")
                if self.clock() >= deadline:
                    raise HarnessFailure(f"{name}: timeout")
                if condition():
                    return
                self.sleep(max(0.0, min(0.1, deadline - self.clock())))
        finally:
            self.durations[name] = self.clock() - started

    def markers(self, name, limit, requirements):
        def complete():
            for child, marker in requirements:
                if marker not in child.text():
                    if (
                        child.process.poll() is not None
                        and not child._thread.is_alive()
                    ):
                        raise HarnessFailure(f"{name}: child exited before {marker!r}")
                    return False
            return True

        self.phase(name, limit, complete)

    def __exit__(self, exc_type, exc, traceback):
        for sig in self.old_handlers:
            signal.signal(sig, signal.SIG_IGN)
        try:
            started = self.clock()
            stop_deadline = min(
                started + self.limits["shutdown"],
                self.deadline - self.limits["cleanup"],
            )
            for role, child in self.children.items():
                if role == "server":
                    child.send("stop")
                elif exc is not None:
                    child.signal(signal.SIGTERM)
            while self.clock() < stop_deadline:
                complete = [
                    c.reap() and not c._thread.is_alive()
                    for c in self.children.values()
                ]
                if all(complete):
                    break
                self.sleep(max(0.0, min(0.1, stop_deadline - self.clock())))
            self.durations["shutdown"] = self.clock() - started
            remaining = [
                c
                for c in self.children.values()
                if not c.reap() or c._thread.is_alive()
            ]
            if remaining:
                self.cleanup_failure = "shutdown: timeout"
            for child in remaining:
                child.signal(signal.SIGKILL)
            started = self.clock()
            cleanup_deadline = min(started + self.limits["cleanup"], self.deadline)
            while self.clock() < cleanup_deadline:
                complete = [
                    c.reap() and not c._thread.is_alive()
                    for c in self.children.values()
                ]
                if all(complete):
                    break
                self.sleep(max(0.0, min(0.1, cleanup_deadline - self.clock())))
            self.cleanup_ok = all(
                [c.reap() and not c._thread.is_alive() for c in self.children.values()]
            )
            if not self.cleanup_ok:
                self.cleanup_failure = "cleanup: owned process group not reaped"
            for child in self.children.values():
                child.save_log()
                try:
                    child.check("shutdown")
                except HarnessFailure as error:
                    self.cleanup_failure = str(error)
            self.durations["cleanup"] = self.clock() - started
            self.durations["total"] = self.clock() - self.started
            self._save_owner()
            if self.on_finished is not None:
                self.result_code = self.on_finished(
                    str(exc) if exc is not None else None
                )
        finally:
            for sig, handler in self.old_handlers.items():
                signal.signal(sig, handler)
            self.lock.close()
        return False
