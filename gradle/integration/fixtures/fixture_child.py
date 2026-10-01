#!/usr/bin/env python3
"""Real socket/process fixtures consumed by the production bootstrap lifecycle."""

import hashlib
import json
import os
from pathlib import Path
import socket
import struct
import subprocess
import sys
import threading
import uuid
import zlib

mode, role, raw_port, raw_output = sys.argv[1:]
port, output = int(raw_port), Path(raw_output)
identity_bytes = bytearray(hashlib.md5(b"OfflinePlayer:FixturePlayer").digest())
identity_bytes[6] = (identity_bytes[6] & 15) | 48
identity_bytes[8] = (identity_bytes[8] & 63) | 128
identity = f"name=FixturePlayer uuid={uuid.UUID(bytes=bytes(identity_bytes))}"
mods = (
    "minecraft=1.21.1,neoforge=21.1.249,infestusfrontier=0.1.0-dev.1,modonomicon=1.120.4,geckolib="
    + ("4.9.1" if mode == "wrong-mod-version" else "4.9.2")
)
if role == "client":
    mods += ",infestusfrontier_client=1.0.0"
print("INFESTUS_INTEGRATION_RUNTIME " + mods, flush=True)


def capture(name):
    def chunk(tag, data):
        return (
            struct.pack(">I", len(data))
            + tag
            + data
            + struct.pack(">I", zlib.crc32(tag + data))
        )

    png = b"\x89PNG\r\n\x1a\n" + chunk(
        b"IHDR", struct.pack(">IIBBBBB", 1280, 720, 8, 2, 0, 0, 0)
    )
    png += chunk(
        b"IDAT", zlib.compress((b"\x00" + b"\x11\x22\x33" * 1280) * 720)
    ) + chunk(b"IEND", b"")
    path = output / name
    path.write_bytes(png)
    if mode == "stale-capture":
        os.utime(path, (1, 1))
    print(f"INFESTUS_CLIENT_CAPTURE name={name}", flush=True)


if role == "server":
    shutdown_hang = mode == "shutdown-timeout"
    code = "import time; time.sleep(60)"
    if shutdown_hang:
        code = "import signal; signal.signal(signal.SIGTERM, signal.SIG_IGN); " + code
    grandchild = subprocess.Popen([sys.executable, "-c", code])
    listener = socket.socket()
    listener.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
    listener.bind(("127.0.0.1", port))
    listener.listen()
    listener.settimeout(0.1)
    (output / "fixture-owned.json").write_text(
        json.dumps({"port": port, "pids": [os.getpid(), grandchild.pid]})
    )
    stopped = threading.Event()

    def stop_reader():
        for line in sys.stdin:
            if line.strip() == "stop" and not shutdown_hang:
                stopped.set()

    threading.Thread(target=stop_reader, daemon=True).start()
    if mode != "readiness-timeout":
        print("Done (fixture)", flush=True)
    if mode in ("newline-flood", "marker-flood"):
        sys.stdout.write("x" * (9 * 1024 * 1024))
        sys.stdout.flush()
    while not stopped.is_set():
        try:
            conn, _ = listener.accept()
        except socket.timeout:
            continue
        with conn:
            data = conn.recv(64)
            if data == b"join":
                print("INFESTUS_INTEGRATION_PLAYER_JOIN " + identity, flush=True)
                conn.sendall(b"ok")
                conn.recv(64)
                print("INFESTUS_INTEGRATION_PLAYER_LEAVE " + identity, flush=True)
            else:
                print("PING_OK", flush=True)
    print("Stopping server", flush=True)
    listener.close()
    grandchild.terminate()
    grandchild.wait()
else:
    print("INFESTUS_CLIENT_TITLE_READY", flush=True)
    capture("bootstrap-title.png")
    with socket.create_connection(("127.0.0.1", port), timeout=1) as conn:
        if mode == "failed-join":
            conn.sendall(b"ping")
        else:
            conn.sendall(b"join")
            conn.recv(64)
            print("INFESTUS_CLIENT_PLAY_ENTER " + identity, flush=True)
            print("INFESTUS_CLIENT_CAMERA_RENDERED yaw=0.0 pitch=15.0", flush=True)
            if mode != "missing-capture":
                capture("bootstrap-world.png")
            conn.sendall(b"leave")
            print("INFESTUS_CLIENT_PLAY_EXIT", flush=True)
