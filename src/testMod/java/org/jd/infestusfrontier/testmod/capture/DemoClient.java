package org.jd.infestusfrontier.testmod.capture;

import com.mojang.logging.LogUtils;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;

/**
 * Builds the demo world and hands it to the owner: fresh flat world, the commands of one capture scenario, teleport
 * to its first view, then normal play. Active only when {@code infestusfrontier.demo.scene} is set.
 */
final class DemoClient {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String WORLD = "demo";
    private static final int STAGE_TIMEOUT_TICKS = 1200;
    private static final List<String> BASELINE = List.of(
            "gamerule sendCommandFeedback false",
            "gamerule doDaylightCycle false",
            "gamerule doWeatherCycle false",
            "gamerule doMobSpawning false",
            "time set noon",
            "weather clear");
    private static final List<String> FINAL = List.of("gamerule sendCommandFeedback true");

    private final Scenario scene;
    private final CommandFeed commands = new CommandFeed();
    private Stage stage = Stage.TITLE;
    private int ticksInStage;
    /** How many commands the command stage started with; its deadline allows one tick each. */
    private int commandsInStage;

    DemoClient(Scenario scene) {
        this.scene = scene;
        NeoForge.EVENT_BUS.addListener(this::onTick);
    }

    private void onTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        try {
            // the command stage sends one command a tick, so its deadline grows with the scenario
            int deadline = STAGE_TIMEOUT_TICKS + (stage == Stage.COMMANDS ? commandsInStage : 0);
            if (++ticksInStage > deadline && stage != Stage.DONE) {
                throw new IllegalStateException("Timed out in stage " + stage);
            }
            switch (stage) {
                case TITLE -> {
                    if (!FlatWorld.atTitle(minecraft)) return;
                    LOGGER.info("INFESTUS_DEMO_WORLD_CREATE scene={}", scene.name());
                    FlatWorld.create(minecraft, WORLD);
                    enter(Stage.JOIN);
                }
                case JOIN -> {
                    if (!FlatWorld.joined(minecraft)) return;
                    commands.addAll(BASELINE);
                    commands.addAll(scene.commands());
                    commands.addAll(scene.play());
                    commands.addAll(FINAL);
                    commandsInStage = commands.size();
                    enter(Stage.COMMANDS);
                }
                case COMMANDS -> {
                    if (commands.tick(minecraft)) {
                        Scenario.View view = scene.views().get(0);
                        FlatWorld.command(
                                minecraft,
                                String.format(
                                        Locale.ROOT,
                                        "tp @s %.3f %.3f %.3f %.2f %.2f",
                                        view.x(),
                                        view.y(),
                                        view.z(),
                                        view.yaw(),
                                        view.pitch()));
                        minecraft.gui.getChat().clearMessages(true);
                        LOGGER.info(
                                "INFESTUS_DEMO_READY commands={}",
                                scene.commands().size() + scene.play().size());
                        enter(Stage.DONE);
                    }
                }
                case DONE -> {}
            }
        } catch (Exception e) {
            // The world stays open so the owner can still look at it.
            LOGGER.error("INFESTUS_DEMO_FAILURE stage={}", stage, e);
            enter(Stage.DONE);
        }
    }

    private void enter(Stage next) {
        stage = next;
        ticksInStage = 0;
    }

    private enum Stage {
        TITLE,
        JOIN,
        COMMANDS,
        DONE
    }
}
