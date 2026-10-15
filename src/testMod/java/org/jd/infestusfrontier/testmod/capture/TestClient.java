package org.jd.infestusfrontier.testmod.capture;

import java.io.IOException;
import java.nio.file.Path;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.Mod;

/**
 * Client entry point of the test mod. Starts the capture client when {@code infestusfrontier.capture.dir} is set,
 * the demo client when {@code infestusfrontier.demo.scene} is set, and nothing otherwise.
 */
@Mod(value = "infestusfrontier_tests", dist = Dist.CLIENT)
public final class TestClient {
    public TestClient() {
        String captureDir = System.getProperty("infestusfrontier.capture.dir");
        String demoScene = System.getProperty("infestusfrontier.demo.scene");
        try {
            if (captureDir != null) {
                new CaptureClient(
                        Path.of(captureDir),
                        Scenario.loadAll("captures", System.getProperty("infestusfrontier.capture.scenarios", "")));
            } else if (demoScene != null) {
                new DemoClient(Scenario.load("captures", demoScene));
            }
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
