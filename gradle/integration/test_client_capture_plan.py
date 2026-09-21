"""Run the client's actual plan check against the harness's bounded manifest."""
import json
import subprocess
import tempfile
import unittest
from pathlib import Path
from gradle.integration import harness

ROOT = Path(__file__).resolve().parents[2]


class ClientCapturePlanTest(unittest.TestCase):
    def test_client_enforces_exact_manifest_without_a_second_global_budget(self):
        source = (ROOT / 'src/testMod/java/org/jd/infestusfrontier/testmod/integration/BootstrapClient.java').read_text()
        start = source.index('                        var names =')
        end = source.index('                        stage = Stage.WORLD_RENDER;', start)
        check = source[start:end]
        fixtures = [json.loads(path.read_text()) for path in
                    (ROOT / 'src/testMod/resources').glob('*/visual-setup.json')]
        expected = harness.visual_setup_captures(ROOT, {'gameTest': [f['requires'] for f in fixtures]})
        program = '''
import java.util.*;
public class CapturePlanRegression {
    record View(String filename) {}
    record Capture(View view) {}
    static String manifest;
    static String environment(String key, String fallback) {
        if (!key.equals("INFESTUS_CAPTURE_PLAN")) throw new AssertionError(key);
        return manifest;
    }
    static void check(List<String> requested) {
        // Both world and UI views count toward the same declared plan.
        int split = requested.size() / 2;
        var detailViews = requested.subList(0, split).stream().map(n -> new Capture(new View(n))).toList();
        var uiCaptures = requested.subList(split, requested.size()).stream().map(n -> new Capture(new View(n))).toList();
''' + check + '''
    }
    static void reject(List<String> requested) {
        try { check(requested); }
        catch (IllegalStateException expected) { return; }
        throw new AssertionError("Accepted missing, duplicate or undeclared capture: " + requested);
    }
    public static void main(String[] args) {
        var expected = List.of(args);
        manifest = String.join(",", expected);
        check(expected);
        var reverse = new ArrayList<>(expected);
        Collections.reverse(reverse);
        check(reverse);
        reject(expected.subList(0, expected.size() - 1));
        var duplicate = new ArrayList<>(expected);
        duplicate.set(duplicate.size() - 1, duplicate.getFirst());
        reject(duplicate);
        var unexpected = new ArrayList<>(expected);
        unexpected.set(unexpected.size() - 1, "undeclared.png");
        reject(unexpected);
        manifest = "";
        reject(expected);
        check(List.of());
    }
}
'''
        with tempfile.TemporaryDirectory() as raw:
            path = Path(raw) / 'CapturePlanRegression.java'
            path.write_text(program)
            result = subprocess.run(['java', str(path), *expected], capture_output=True, text=True, timeout=15)
        self.assertEqual(0, result.returncode, result.stdout + result.stderr)

    def test_lifecycle_passes_validated_plan_to_client(self):
        class ClientStarted(Exception):
            pass

        class Supervisor:
            limits = {'readiness': 1}
            def markers(self, *args):
                pass
            def start(self, name, command, cwd, log, env=None):
                if name == 'client':
                    self.environment = env
                    raise ClientStarted()

        with tempfile.TemporaryDirectory() as raw:
            supervisor = Supervisor()
            supervisor.run_dir = Path(raw)
            with self.assertRaises(ClientStarted):
                harness._client_lifecycle(supervisor, [], [], Path(raw), Path(raw), Path(raw), 25565,
                                          captures=['front.png', 'back.png'])
            self.assertEqual('front.png,back.png', supervisor.environment['INFESTUS_CAPTURE_PLAN'])
