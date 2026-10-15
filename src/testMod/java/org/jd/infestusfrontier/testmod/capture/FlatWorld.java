package org.jd.infestusfrontier.testmod.capture;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.protocol.game.ServerboundPlayerAbilitiesPacket;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;

/** Creates and joins the disposable flat Creative world the test clients use. Surface at y=-61, player at y=-60. */
final class FlatWorld {
    private FlatWorld() {}

    /** True once the title screen is up with no loading overlay. */
    static boolean atTitle(Minecraft minecraft) {
        return minecraft.isGameLoadFinished()
                && minecraft.screen instanceof TitleScreen
                && minecraft.getOverlay() == null;
    }

    /** Creates a fresh flat, peaceful, Creative world with cheats, seed 0, and starts loading it. */
    static void create(Minecraft minecraft, String name) {
        LevelSettings settings = new LevelSettings(
                name,
                GameType.CREATIVE,
                false,
                Difficulty.PEACEFUL,
                true,
                new GameRules(),
                WorldDataConfiguration.DEFAULT);
        WorldOptions options = new WorldOptions(0L, false, false);
        minecraft
                .createWorldOpenFlows()
                .createFreshLevel(
                        name,
                        settings,
                        options,
                        registry -> registry.registryOrThrow(Registries.WORLD_PRESET)
                                .getHolderOrThrow(WorldPresets.FLAT)
                                .value()
                                .createWorldDimensions(),
                        new TitleScreen());
    }

    /** True once the player is in the world, no screen is open, and the 5x5 chunks around the player exist. */
    static boolean joined(Minecraft minecraft) {
        if (minecraft.level == null || minecraft.player == null || minecraft.screen != null) return false;
        var chunk = minecraft.player.chunkPosition();
        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                if (!minecraft.level.hasChunk(chunk.x + x, chunk.z + z)) return false;
            }
        }
        return true;
    }

    /** Starts flying, the vanilla way a Creative player does, so a view at any height holds still. */
    static void fly(Minecraft minecraft) {
        var abilities = minecraft.player.getAbilities();
        abilities.flying = true;
        minecraft.player.connection.send(new ServerboundPlayerAbilitiesPacket(abilities));
    }

    /** Sends one command as the player; fails loudly when the client parser rejects it. */
    static void command(Minecraft minecraft, String command) {
        if (!minecraft.player.connection.sendUnsignedCommand(command)) {
            throw new IllegalStateException("Command rejected by the client parser: " + command);
        }
    }
}
