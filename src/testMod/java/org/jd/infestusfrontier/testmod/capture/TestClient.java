package org.jd.infestusfrontier.testmod.capture;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLPaths;

/**
 * Client entry point of the test mod. Starts the capture client when {@code infestusfrontier.capture.dir} is set (the
 * run directory under it comes from {@code capture-run.txt} in the game directory),
 * the demo client when {@code infestusfrontier.demo.scene} is set, and nothing otherwise.
 */
@Mod(value = "infestusfrontier_tests", dist = Dist.CLIENT)
public final class TestClient {
    public TestClient() {
        String captureDir = System.getProperty("infestusfrontier.capture.dir");
        String demoScene = System.getProperty("infestusfrontier.demo.scene");
        try {
            if (captureDir != null) {
                // The build names this run's directory in capture-run.txt, so no run reuses another's output.
                Path runName = FMLPaths.GAMEDIR.get().resolve("capture-run.txt");
                new CaptureClient(
                        Path.of(captureDir).resolve(Files.readString(runName).trim()),
                        Scenario.loadAll(
                                System.getProperty("infestusfrontier.capture.folder", "captures"),
                                System.getProperty("infestusfrontier.capture.scenarios", "")));
            } else if (demoScene != null) {
                new DemoClient(Scenario.load("captures", demoScene));
            }
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
