package org.jd.infestusfrontier.testmod;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.jd.infestusfrontier.core.foundation.Biomass;
import org.jd.infestusfrontier.ecology.EcologyModule;
import org.jd.infestusfrontier.storage.BiomassBladderBlock;
import org.jd.infestusfrontier.storage.BiomassBladderMenu;
import org.jd.infestusfrontier.storage.StorageModule;
import org.jd.infestusfrontier.storage.api.BiomassCapabilities;
import org.jd.infestusfrontier.storage.api.BiomassFluids;

/** The Biomass Bladder: buckets in and out, the fill level, the fluid capability and the screen. */
@GameTestHolder("infestusfrontier_tests")
@PrefixGameTestTemplate(false)
public final class StorageTests {
    private static final BlockPos GROUND = new BlockPos(1, 1, 1);
    private static final BlockPos BLADDER = GROUND.above();

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void bucketsFillTheBladderAndShowTheLevel(GameTestHelper h) {
        placeBladder(h);
        Player player = EcologyTests.holding(h, GameType.SURVIVAL, StorageModule.BIOMASS_BUCKET.get(), 1);
        EcologyTests.use(h, player, BLADDER, Direction.UP);
        h.assertTrue(player.getMainHandItem().is(Items.BUCKET), "empty bucket returned");
        h.assertValueEqual(tank(h).stored(), Biomass.BUCKET, "one bucket stored");
        assertFill(h, 1);
        Player carrier = EcologyTests.holding(h, GameType.SURVIVAL, Items.BUCKET, 1);
        EcologyTests.use(h, carrier, BLADDER, Direction.UP);
        h.assertTrue(carrier.getMainHandItem().is(StorageModule.BIOMASS_BUCKET.get()), "bucket filled back");
        h.assertValueEqual(tank(h).stored(), Biomass.ZERO, "bladder empty again");
        assertFill(h, 0);
        h.succeed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void fullBladderRefusesAFifthBucketUnchanged(GameTestHelper h) {
        placeBladder(h);
        Player player = EcologyTests.holding(h, GameType.CREATIVE, StorageModule.BIOMASS_BUCKET.get(), 1);
        for (int i = 0; i < 4; i++) EcologyTests.use(h, player, BLADDER, Direction.UP);
        h.assertValueEqual(tank(h).stored(), Biomass.of(4000), "four buckets fill it");
        assertFill(h, 4);
        EcologyTests.use(h, player, BLADDER, Direction.UP);
        h.assertValueEqual(tank(h).stored(), Biomass.of(4000), "fifth bucket refused");
        h.succeed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void waterBucketIsRefusedAndKept(GameTestHelper h) {
        placeBladder(h);
        Player player = EcologyTests.holding(h, GameType.SURVIVAL, Items.WATER_BUCKET, 1);
        EcologyTests.use(h, player, BLADDER, Direction.UP);
        h.assertTrue(player.getMainHandItem().is(Items.WATER_BUCKET), "water bucket kept");
        h.assertValueEqual(tank(h).stored(), Biomass.ZERO, "nothing stored");
        h.assertValueEqual(
                BiomassFluids.refusal(
                        new FluidStack(Fluids.WATER, 1000),
                        h.getLevel()
                                .getCapability(Capabilities.FluidHandler.BLOCK, h.absolutePos(BLADDER), Direction.UP),
                        h.getLevel().getCapability(BiomassCapabilities.BLOCK, h.absolutePos(BLADDER), Direction.UP)),
                BiomassFluids.WRONG_FLUID,
                "water is the wrong fluid");
        h.assertBlockPresent(Blocks.AIR, BLADDER.above());
        h.succeed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void fluidHandlersMovePartialAmountsLikePipes(GameTestHelper h) {
        placeBladder(h);
        BlockPos other = BLADDER.east();
        h.setBlock(GROUND.east(), EcologyModule.LIVING_SUBSTRATE.get());
        h.setBlock(other, StorageModule.BIOMASS_BLADDER.get());
        Player player = EcologyTests.holding(h, GameType.SURVIVAL, StorageModule.BIOMASS_BUCKET.get(), 1);
        EcologyTests.use(h, player, BLADDER, Direction.UP);
        IFluidHandler source = fluidHandler(h, BLADDER);
        IFluidHandler sink = fluidHandler(h, other);
        h.assertTrue(BiomassFluids.isBiomass(source.getFluidInTank(0)), "the tank reports biomass");
        h.assertValueEqual(source.getFluidInTank(0).getAmount(), 1000, "one bucket as fluid");
        h.assertValueEqual(source.getTankCapacity(0), 4000, "capacity as fluid");
        FluidStack moved = FluidUtil.tryFluidTransfer(sink, source, 250, true);
        h.assertValueEqual(moved.getAmount(), 250, "a quarter bucket moved");
        h.assertValueEqual(tank(h).stored(), Biomass.of(750), "source keeps the rest");
        h.assertValueEqual(ProcessingTests.handler(h, other).tank().stored(), Biomass.of(250), "sink has it");
        h.assertValueEqual(
                sink.fill(new FluidStack(Fluids.WATER, 1000), IFluidHandler.FluidAction.EXECUTE), 0, "water refused");
        h.succeed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void menuReadsTheLiveBalanceAndAHeldBlockOpensIt(GameTestHelper h) {
        placeBladder(h);
        Player player = EcologyTests.holding(h, GameType.SURVIVAL, StorageModule.BIOMASS_BUCKET.get(), 1);
        EcologyTests.use(h, player, BLADDER, Direction.UP);
        // No GameTest player can receive a screen yet, so the menu is built the way the block entity builds it.
        MenuProvider provider = h.getBlockEntity(BLADDER);
        AbstractContainerMenu menu = provider.createMenu(1, player.getInventory(), player);
        h.assertTrue(menu instanceof BiomassBladderMenu, "the bladder offers its menu");
        BiomassBladderMenu bladder = (BiomassBladderMenu) menu;
        h.assertValueEqual(bladder.stored(), 1000, "menu reads the stored amount");
        h.assertValueEqual(bladder.capacity(), 4000, "menu reads the capacity");
        h.assertTrue(bladder.clickMenuButton(player, BiomassBladderMenu.FILL_BUTTON), "fill is handled");
        h.assertValueEqual(bladder.stored(), 1000, "with nothing in the slot it moves nothing");
        Player builder = EcologyTests.holding(h, GameType.SURVIVAL, Items.STONE, 1);
        h.assertValueEqual(
                EcologyTests.useHeld(h, builder, BLADDER, Direction.UP),
                ItemInteractionResult.CONSUME,
                "the bladder answers a held stone itself");
        EcologyTests.use(h, builder, BLADDER, Direction.UP);
        h.assertBlockPresent(Blocks.AIR, BLADDER.above());
        h.assertValueEqual(builder.getMainHandItem().getCount(), 1, "a held item opens the screen instead of building");
        h.succeed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void bladderPlacesOnlyOnLivingSubstrate(GameTestHelper h) {
        h.setBlock(GROUND, Blocks.DIRT);
        Player player = EcologyTests.holding(h, GameType.SURVIVAL, StorageModule.BIOMASS_BLADDER_ITEM.get(), 1);
        h.placeAt(player, player.getMainHandItem(), GROUND, Direction.UP);
        h.assertBlockPresent(Blocks.AIR, BLADDER);
        h.assertValueEqual(player.getMainHandItem().getCount(), 1, "bladder kept");
        h.succeed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void overflowingTankIsUnreadableNotACrash(GameTestHelper h) {
        var registries = unreadableBladder(h, Integer.MAX_VALUE, 1, Integer.MAX_VALUE);
        Player player = EcologyTests.holding(h, GameType.SURVIVAL, StorageModule.BIOMASS_BUCKET.get(), 1);
        EcologyTests.use(h, player, BLADDER, Direction.UP);
        h.assertTrue(player.getMainHandItem().is(StorageModule.BIOMASS_BUCKET.get()), "the bucket stays full");
        CompoundTag kept = h.getBlockEntity(BLADDER).saveWithoutMetadata(registries);
        h.assertValueEqual(kept.getCompound("tank").getInt("stored"), Integer.MAX_VALUE, "the bytes stay");
        h.succeed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void wrongCapacityIsUnreadable(GameTestHelper h) {
        var registries = unreadableBladder(h, 1000, 0, 5000);
        Player player = EcologyTests.holding(h, GameType.SURVIVAL, Items.BUCKET, 1);
        EcologyTests.use(h, player, BLADDER, Direction.UP);
        h.assertTrue(player.getMainHandItem().is(Items.BUCKET), "nothing comes out of a tank that is not ours");
        CompoundTag kept = h.getBlockEntity(BLADDER).saveWithoutMetadata(registries);
        h.assertValueEqual(kept.getCompound("tank").getInt("capacity"), 5000, "the bytes stay");
        h.succeed();
    }

    private static HolderLookup.Provider unreadableBladder(GameTestHelper h, int stored, int reserved, int capacity) {
        placeBladder(h);
        var registries = h.getLevel().registryAccess();
        BlockEntity bladder = h.getBlockEntity(BLADDER);
        CompoundTag tag = bladder.saveWithoutMetadata(registries);
        CompoundTag tank = new CompoundTag();
        tank.putInt("stored", stored);
        tank.putInt("reserved", reserved);
        tank.putInt("capacity", capacity);
        tag.put("tank", tank);
        bladder.loadWithComponents(tag, registries);
        return registries;
    }

    static void placeBladder(GameTestHelper h) {
        h.setBlock(GROUND, EcologyModule.LIVING_SUBSTRATE.get());
        h.setBlock(BLADDER, StorageModule.BIOMASS_BLADDER.get());
    }

    static org.jd.infestusfrontier.core.storage.BiomassTank tank(GameTestHelper h) {
        return ProcessingTests.handler(h, BLADDER).tank();
    }

    static IFluidHandler fluidHandler(GameTestHelper h, BlockPos pos) {
        IFluidHandler handler = h.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, h.absolutePos(pos), null);
        if (handler == null) h.fail("no fluid capability", pos);
        return handler;
    }

    static void assertFill(GameTestHelper h, int level) {
        h.assertBlockState(
                BLADDER,
                (BlockState s) -> s.getValue(BiomassBladderBlock.FILL) == level,
                () -> "expected fill " + level);
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void joinedHandlerRoutesEachFluidToItsTank(GameTestHelper h) {
        FluidTank water = new FluidTank(2000, stack -> stack.is(Fluids.WATER));
        FluidTank biomass = new FluidTank(2000, BiomassFluids::isBiomass);
        IFluidHandler joined = BiomassFluids.join(water, biomass);
        h.assertValueEqual(joined.getTanks(), 2, "two tanks");
        FluidStack bucketOfBiomass = BiomassFluids.of(Biomass.BUCKET);
        h.assertValueEqual(joined.fill(bucketOfBiomass, IFluidHandler.FluidAction.EXECUTE), 1000, "biomass filled");
        h.assertValueEqual(
                joined.fill(new FluidStack(Fluids.WATER, 500), IFluidHandler.FluidAction.EXECUTE), 500, "water filled");
        h.assertValueEqual(
                joined.fill(new FluidStack(Fluids.LAVA, 1000), IFluidHandler.FluidAction.EXECUTE), 0, "lava refused");
        h.assertValueEqual(water.getFluidAmount(), 500, "water in the water tank");
        h.assertValueEqual(biomass.getFluidAmount(), 1000, "biomass in the biomass tank");
        h.assertTrue(joined.getFluidInTank(1).is(StorageModule.BIOMASS.get()), "tank 1 is biomass");
        FluidStack bucketful = joined.drain(1000, IFluidHandler.FluidAction.SIMULATE);
        h.assertTrue(
                BiomassFluids.isBiomass(bucketful) && bucketful.getAmount() == 1000,
                "a bucket takes the fluid that fills it, not the 500 mB of water in front");
        FluidStack drained = joined.drain(bucketOfBiomass, IFluidHandler.FluidAction.EXECUTE);
        h.assertValueEqual(drained.getAmount(), 1000, "biomass drained by kind");
        h.assertValueEqual(
                joined.drain(1000, IFluidHandler.FluidAction.SIMULATE).getAmount(), 500, "water is what is left");
        h.succeed();
    }
}
