package org.jd.infestusfrontier.testmod.integration.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.PauseScreen;

/** Exercises contributor packaging and multi-tick UI verification without adding gameplay content. */
public final class BootstrapGuideScenario implements ContentGuideScenario {
    private PauseScreen screen;

    @Override
    public String assertion() {
        return "infestusfrontier_client:integration.world";
    }

    @Override
    public boolean tick(Minecraft minecraft) {
        if (minecraft.player == null || minecraft.level == null) {
            throw new IllegalStateException("Guide fixture requires a joined client");
        }
        if (screen == null) {
            screen = new PauseScreen(true);
            minecraft.setScreen(screen);
            return false;
        }
        if (minecraft.screen != screen || screen.children().isEmpty()) {
            throw new IllegalStateException("Guide fixture did not open the pause menu UI");
        }
        minecraft.setScreen(null);
        return true;
    }
}
