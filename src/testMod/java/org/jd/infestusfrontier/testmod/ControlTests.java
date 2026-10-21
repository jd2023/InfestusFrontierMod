package org.jd.infestusfrontier.testmod;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.jd.infestusfrontier.control.ControlModule;
import org.jd.infestusfrontier.control.SensorPolypBlock;
import org.jd.infestusfrontier.core.foundation.Biomass;
import org.jd.infestusfrontier.storage.StorageModule;
import org.jd.infestusfrontier.storage.api.BiomassFluids;

/** The Sensor Polyp: it reads biomass, fluid and items into power, out of every side but its eye. */
@GameTestHolder("infestusfrontier_tests")
@PrefixGameTestTemplate(false)
public final class ControlTests {
    private static final BlockPos GROUND = new BlockPos(2, 1, 2);
    private static final BlockPos TARGET = GROUND.above();
    private static final BlockPos POLYP = TARGET.east();

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", timeoutTicks = 100)
    public static void thePolypReadsABladderIntoPowerAndTellsItsNeighbours(GameTestHelper h) {
        h.setBlock(GROUND, EcologyTests.mature());
        h.setBlock(TARGET, StorageModule.BIOMASS_BLADDER.get());
        Player builder = EcologyTests.holding(h, GameType.SURVIVAL, ControlModule.SENSOR_POLYP_ITEM.get(), 1);
        h.placeAt(builder, builder.getMainHandItem(), TARGET, Direction.EAST);
        h.assertBlockPresent(ControlModule.SENSOR_POLYP.get(), POLYP);
        h.assertValueEqual(
                h.getBlockState(POLYP).getValue(SensorPolypBlock.FACING), Direction.WEST, "it looks at the bladder");
        h.setBlock(POLYP.east(), Blocks.REDSTONE_LAMP);
        IFluidHandler tank =
                h.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, h.absolutePos(TARGET), Direction.UP);
        tank.fill(BiomassFluids.of(Biomass.of(2000)), IFluidHandler.FluidAction.EXECUTE);
        h.runAfterDelay(30, () -> {
            h.assertValueEqual(h.getBlockState(POLYP).getValue(SensorPolypBlock.POWER), 7, "half full is 7");
            h.assertTrue(
                    h.getBlockState(POLYP.east()).getValue(net.minecraft.world.level.block.RedstoneLampBlock.LIT),
                    "the lamp behind it lit");
            h.assertValueEqual(
                    h.getLevel().getSignal(h.absolutePos(POLYP), Direction.WEST), 7, "power out of the back");
            h.assertValueEqual(h.getLevel().getSignal(h.absolutePos(POLYP), Direction.EAST), 0, "none out of the eye");
            tank.drain(1999, IFluidHandler.FluidAction.EXECUTE);
            h.runAfterDelay(20, () -> {
                h.assertValueEqual(h.getBlockState(POLYP).getValue(SensorPolypBlock.POWER), 1, "a drop is still 1");
                tank.drain(1, IFluidHandler.FluidAction.EXECUTE);
                h.runAfterDelay(20, () -> {
                    h.assertValueEqual(h.getBlockState(POLYP).getValue(SensorPolypBlock.POWER), 0, "empty is 0");
                    h.assertFalse(
                            h.getBlockState(POLYP.east())
                                    .getValue(net.minecraft.world.level.block.RedstoneLampBlock.LIT),
                            "the lamp went out");
                    h.succeed();
                });
            });
        });
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", timeoutTicks = 60)
    public static void thePolypReadsAChestLikeAComparator(GameTestHelper h) {
        h.setBlock(GROUND, Blocks.STONE);
        h.setBlock(TARGET, Blocks.CHEST);
        ChestBlockEntity chest = (ChestBlockEntity) h.getBlockEntity(TARGET);
        for (int slot = 0; slot < 27; slot++) chest.setItem(slot, new ItemStack(Items.STONE, 64));
        h.setBlock(
                POLYP,
                ControlModule.SENSOR_POLYP.get().defaultBlockState().setValue(SensorPolypBlock.FACING, Direction.WEST));
        h.runAfterDelay(15, () -> {
            h.assertValueEqual(h.getBlockState(POLYP).getValue(SensorPolypBlock.POWER), 15, "a full chest is 15");
            h.assertValueEqual(
                    h.getBlockState(POLYP).getAnalogOutputSignal(h.getLevel(), h.absolutePos(POLYP)),
                    15,
                    "and the comparator value");
            h.succeed();
        });
    }
}
