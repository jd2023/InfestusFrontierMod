"""Exercise the client shutdown method against an in-flight-packet boundary.

Compile the actual method with small lifecycle doubles, without loading a GPU or
Minecraft. The live capture gate separately exercises the real client/connection.
"""
import subprocess
import tempfile
import unittest
from pathlib import Path


class ClientDisconnectTest(unittest.TestCase):
    def test_transport_closes_before_world_and_registry_teardown(self):
        root = Path(__file__).resolve().parents[2]
        source = (root / "src/testMod/java/org/jd/infestusfrontier/testmod/integration/BootstrapClient.java").read_text()
        start = source.index("    private void disconnect(Minecraft minecraft) {")
        end = source.index("\n    private void prepareUi", start)
        method = source[start:end]
        program = """
public class DisconnectRegression {
    enum Stage { STOP }
    Stage stage;
    static boolean exitLogged;
    static final Log LOGGER = new Log();
    static class Log {
        void info(String message) { exitLogged = true; }
    }
    static class TitleScreen {}
    static class Level {
        boolean connected = true;
        void disconnect() { connected = false; }
    }
    static class Minecraft {
        Level level = new Level();
        boolean tornDown;
        void disconnect(TitleScreen screen) {
            // Registry reset must never overlap a still-live packet decoder.
            if (level != null && level.connected)
                throw new AssertionError("Network still receiving at registry teardown");
            if (exitLogged) throw new AssertionError("Exit reported before teardown");
            level = null;
            tornDown = true;
        }
    }
    public static void main(String[] args) {
        for (boolean hasLevel : new boolean[] {true, false}) {
            var client = new Minecraft();
            if (!hasLevel) client.level = null;
            var automation = new DisconnectRegression();
            exitLogged = false;
            automation.disconnect(client);
            if (!client.tornDown || !exitLogged || automation.stage != Stage.STOP)
                throw new AssertionError("Disconnect did not finish before stop");
        }
    }
""" + method + "\n}\n"
        with tempfile.TemporaryDirectory() as raw:
            path = Path(raw) / "DisconnectRegression.java"
            path.write_text(program)
            result = subprocess.run(["java", str(path)], capture_output=True, text=True, timeout=15)
        self.assertEqual(0, result.returncode, result.stdout + result.stderr)
