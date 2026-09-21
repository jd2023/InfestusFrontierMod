"""Publish this owned Xvfb session to the second disposable client, then exec Java."""
import json
import os
from pathlib import Path
import sys


def launch(destination, command):
    path = Path(destination)
    temporary = path.with_suffix(".tmp")
    temporary.write_text(json.dumps({key: os.environ[key] for key in ("DISPLAY", "XAUTHORITY")}))
    temporary.replace(path)
    os.execvp(command[0], command)


if __name__ == "__main__":
    launch(sys.argv[1], sys.argv[2:])
