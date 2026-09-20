package org.jd.infestusfrontier.testmod.integration.client;

import java.util.List;
import net.minecraft.client.Minecraft;

/** Observes an owner's server-prepared scene before the shared world capture. */
public interface ContentVisualScenario {
    boolean ready(Minecraft minecraft);

    default List<View> detailViews() {
        return List.of();
    }

    record View(String filename, float yaw, float pitch) {
        public View {
            if (!filename.matches("[a-z][a-z0-9-]{0,63}\\.png") || filename.startsWith("bootstrap-")
                    || !Float.isFinite(yaw) || Math.abs(yaw) > 180
                    || !Float.isFinite(pitch) || Math.abs(pitch) > 90) {
                throw new IllegalArgumentException("Invalid detail capture view");
            }
        }
    }
}
