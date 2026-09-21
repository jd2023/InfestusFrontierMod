package org.jd.infestusfrontier.testmod.interaction.client;

import com.mojang.logging.LogUtils;
import java.nio.file.Files;
import java.nio.file.Path;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.jd.infestusfrontier.processing.client.CultureBowlScreen;
import org.jd.infestusfrontier.processing.menu.CultureBowlMenu;
import org.jd.infestusfrontier.processing.menu.CultureBowlMenuSnapshot;
import org.jd.infestusfrontier.processing.menu.BowlRefusal;
import org.jd.infestusfrontier.testmod.integration.client.ContentMultiplayerScenario;
import org.jd.infestusfrontier.ui.OrganScreenLayout;

public final class ProbeMultiplayerScenario implements ContentMultiplayerScenario {
    private static final BlockPos BOWL = new BlockPos(-2, -60, 3);
    private String previous = "";
    private boolean announced;
    private boolean opening;
    private int ticks;
    private int lastProgress;
    private long activeRevision;
    private CultureBowlMenuSnapshot last;

    @Override public boolean tick(Minecraft game, String role, Path coordination) throws Exception {
        if (!announced) {
            if (game.screen != null) game.player.closeContainer();
            LogUtils.getLogger().info("INFESTUS_PROBE_READY role={}", role);
            announced = true;
        }
        Path command = coordination.resolve("probe-stage");
        if (!Files.exists(command)) return false;
        if (Files.size(command) > 32) throw new IllegalStateException("Oversized probe phase");
        String phase = Files.readString(command).trim();
        if (!phase.equals(previous)) { previous = phase; ticks = 0; opening = false; }
        ticks++;
        boolean owner = role.equals("owner");
        if (phase.equals("done")) return true;
        if (phase.equals("open") || phase.equals("reopen")) {
            if (!(game.screen instanceof CultureBowlScreen)) {
                if (!opening) {
                    open(game);
                    opening = true;
                }
                return false;
            }
        }
        if (!(game.screen instanceof CultureBowlScreen) || !(game.player.containerMenu instanceof CultureBowlMenu menu)) return false;
        var snapshot = menu.snapshot();
        if (phase.equals("idle") && ticks > 1 && snapshot != last)
            throw new IllegalStateException("Idle menu received an unnecessary snapshot");
        if (!snapshot.equals(last)) {
            LogUtils.getLogger().info("INFESTUS_PROBE_SNAPSHOT role={} revision={} work={} required={} water={} state={} slots={} refusal={}",
                    role, snapshot.revision(), snapshot.completedWork(), snapshot.requiredWork(), snapshot.water(),
                    snapshot.state(), snapshot.slots(), snapshot.refusal());
            last = snapshot;
        }
        switch (phase) {
            case "open" -> {
                if (ticks >= 2) LogUtils.getLogger().info("INFESTUS_PROBE_OPEN role={}", role);
            }
            case "reset" -> {
                if (owner && ticks == 1) click(game, "cancel");
                if (snapshot.state() == CultureBowlMenuSnapshot.State.IDLE)
                    LogUtils.getLogger().info("INFESTUS_PROBE_RESET role={}", role);
            }
            case "start" -> {
                if (owner && ticks == 1) { click(game, "recipe-7"); click(game, "start"); }
                if (snapshot.state() == CultureBowlMenuSnapshot.State.WORKING && snapshot.completedWork() >= 20) {
                    activeRevision = snapshot.revision();
                    lastProgress = snapshot.completedWork();
                    LogUtils.getLogger().info("INFESTUS_PROBE_WORKING role={} revision={} work={}", role, activeRevision, lastProgress);
                }
            }
            case "close" -> {
                if (owner) {
                    lastProgress = snapshot.completedWork();
                    activeRevision = snapshot.revision();
                    game.player.closeContainer();
                    LogUtils.getLogger().info("INFESTUS_PROBE_CLOSED role=owner work={}", lastProgress);
                }
            }
            case "reopen" -> {
                if (owner && snapshot.state() == CultureBowlMenuSnapshot.State.WORKING
                        && snapshot.revision() == activeRevision && snapshot.completedWork() >= lastProgress) {
                    LogUtils.getLogger().info("INFESTUS_PROBE_REOPENED role=owner work={}", snapshot.completedWork());
                }
            }
            case "wrong-owner" -> {
                if (!owner && ticks == 1) click(game, "cancel");
                if (!owner && snapshot.refusal() == BowlRefusal.WRONG_OWNER
                        && snapshot.state() == CultureBowlMenuSnapshot.State.WORKING)
                    LogUtils.getLogger().info("INFESTUS_PROBE_OWNER_REFUSAL");
            }
            case "idle" -> {
                if (ticks == 22) LogUtils.getLogger().info("INFESTUS_PROBE_IDLE_SILENT role={}", role);
            }
            case "cancel" -> {
                // Deliberately wait after the most recent progress packet before using its revision.
                if (owner && ticks == 3) click(game, "cancel");
                if (snapshot.state() == CultureBowlMenuSnapshot.State.IDLE && snapshot.revision() > activeRevision
                        && (!owner || snapshot.refusal() == BowlRefusal.NONE))
                    LogUtils.getLogger().info("INFESTUS_PROBE_CANCELLED role={}", role);
            }
            default -> throw new IllegalStateException("Unknown probe phase " + phase);
        }
        return false;
    }
    private static void open(Minecraft game) {
        game.player.getInventory().selected = 6;
        game.gameMode.useItemOn(game.player, InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(BOWL), Direction.UP, BOWL, false));
    }
    private static void click(Minecraft game, String id) {
        var b = OrganScreenLayout.centered(game.screen.width, game.screen.height).control(id).bounds();
        if (!game.screen.mouseClicked(b.centerX(), b.centerY(), 0)) throw new IllegalStateException("Unclickable " + id);
        game.screen.mouseReleased(b.centerX(), b.centerY(), 0);
    }
}
