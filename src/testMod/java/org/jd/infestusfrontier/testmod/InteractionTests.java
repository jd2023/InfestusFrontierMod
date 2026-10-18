package org.jd.infestusfrontier.testmod;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.jd.infestusfrontier.core.foundation.Biomass;
import org.jd.infestusfrontier.ecology.EcologyModule;
import org.jd.infestusfrontier.interaction.InteractionModule;
import org.jd.infestusfrontier.processing.ProcessingModule;

/** The Synaptic Probe: reads a tank without opening the organ's screen, passes elsewhere. */
@GameTestHolder("infestusfrontier_tests")
@PrefixGameTestTemplate(false)
public final class InteractionTests {
    private static final BlockPos GROUND = new BlockPos(1, 1, 1);
    private static final BlockPos TARGET = GROUND.above();

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void organsStepAsideForTheProbe(GameTestHelper h) {
        h.setBlock(GROUND, EcologyModule.LIVING_SUBSTRATE.get());
        h.setBlock(TARGET, ProcessingModule.CULTURE_BOWL.get());
        Player player = EcologyTests.holding(h, GameType.SURVIVAL, InteractionModule.SYNAPTIC_PROBE.get(), 1);
        h.assertValueEqual(
                EcologyTests.useHeld(h, player, TARGET, Direction.UP),
                ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION,
                "the bowl leaves the probe to its own use");
        BlockPos absolute = h.absolutePos(TARGET);
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(absolute), Direction.UP, absolute, false);
        h.assertValueEqual(
                h.getBlockState(TARGET).useWithoutItem(h.getLevel(), player, hit),
                InteractionResult.PASS,
                "the bowl does not open for a probe hand");
        h.assertValueEqual(probe(h), InteractionResult.CONSUME, "the probe reads the bowl");
        h.succeed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void probeReportsASacBalance(GameTestHelper h) {
        h.setBlock(GROUND, EcologyModule.LIVING_SUBSTRATE.get());
        h.setBlock(TARGET, ProcessingModule.DIGESTIVE_SAC.get());
        h.assertValueEqual(probe(h), InteractionResult.CONSUME, "the probe acts on a sac");
        h.assertValueEqual(ProcessingTests.handler(h, TARGET).tank().stored(), Biomass.ZERO, "tank unchanged");
        h.succeed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void probePassesOnStone(GameTestHelper h) {
        h.setBlock(TARGET, Blocks.STONE);
        h.assertValueEqual(probe(h), InteractionResult.PASS, "stone has nothing to read");
        h.succeed();
    }

    private static InteractionResult probe(GameTestHelper h) {
        Player player = EcologyTests.holding(h, GameType.SURVIVAL, InteractionModule.SYNAPTIC_PROBE.get(), 1);
        BlockPos absolute = h.absolutePos(TARGET);
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(absolute), Direction.UP, absolute, false);
        ItemStack stack = player.getMainHandItem();
        return stack.getItem().useOn(new UseOnContext(h.getLevel(), player, InteractionHand.MAIN_HAND, stack, hit));
    }
}
