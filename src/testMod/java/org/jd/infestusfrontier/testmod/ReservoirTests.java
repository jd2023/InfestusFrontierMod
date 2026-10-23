package org.jd.infestusfrontier.testmod;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.jd.infestusfrontier.core.foundation.Biomass;
import org.jd.infestusfrontier.core.storage.BiomassTank;
import org.jd.infestusfrontier.logistics.LogisticsModule;
import org.jd.infestusfrontier.logistics.Port;
import org.jd.infestusfrontier.logistics.VascularJunctionBlock;
import org.jd.infestusfrontier.organ.OrganModule;
import org.jd.infestusfrontier.processing.ProcessingModule;
import org.jd.infestusfrontier.storage.ReservoirCellBlock;
import org.jd.infestusfrontier.storage.StorageModule;
import org.jd.infestusfrontier.storage.api.BiomassFluids;

/**
 * Reservoir cells: touching cells form one rectangular body that fills from the bottom and drains from the top; an
 * incomplete row waits outside with its own biomass; a broken cell takes its share and the rest forms again; a body
 * with a cell out of reach refuses; the recipe carries the Bladder's biomass.
 */
@GameTestHolder("infestusfrontier_tests")
@PrefixGameTestTemplate(false)
public final class ReservoirTests {
    private static final int Y = 1;
    /** Ticks for a placed cell to look for its body, and a little more. */
    private static final int FORMED = 4;

    private ReservoirTests() {}

    private static BlockPos cell(int x, int layer, int z) {
        return new BlockPos(x, Y + 1 + layer, z);
    }

    /** Substrate under x0..x1, z0..z1 and cells on it, {@code layers} high. */
    private static void body(GameTestHelper h, int x0, int x1, int z0, int z1, int layers) {
        for (int x = x0; x <= x1; x++)
            for (int z = z0; z <= z1; z++) {
                h.setBlock(new BlockPos(x, Y, z), EcologyTests.mature());
                for (int layer = 0; layer < layers; layer++)
                    h.setBlock(cell(x, layer, z), StorageModule.RESERVOIR_CELL.get());
            }
    }

    private static IFluidHandler fluid(GameTestHelper h, BlockPos pos) {
        return h.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, h.absolutePos(pos), Direction.UP);
    }

    private static int fill(GameTestHelper h, BlockPos pos, int amount) {
        return fluid(h, pos).fill(BiomassFluids.of(Biomass.of(amount)), IFluidHandler.FluidAction.EXECUTE);
    }

    /** What the whole body holds, read through the cell at {@code pos}. */
    private static int body(GameTestHelper h, BlockPos pos) {
        return ProcessingTests.handler(h, pos).tank().stored().units();
    }

    /** The cell's own share, the balance it would carry on its item. */
    private static int share(GameTestHelper h, BlockPos pos) {
        BlockEntity cell = h.getBlockEntity(pos);
        BiomassTank tank = cell.collectComponents().get(StorageModule.TANK.get());
        return tank == null ? -1 : tank.stored().units();
    }

    private static boolean joined(GameTestHelper h, BlockPos pos, Direction side) {
        return h.getBlockState(pos).getValue(ReservoirCellBlock.JOINED.get(side));
    }

    private static boolean alone(GameTestHelper h, BlockPos pos) {
        for (Direction side : Direction.values()) if (joined(h, pos, side)) return false;
        return true;
    }

    private static int fillLevel(GameTestHelper h, BlockPos pos) {
        return h.getBlockState(pos).getValue(ReservoirCellBlock.FILL);
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void aFullBoxFormsOneBodyThatFillsFromTheBottom(GameTestHelper h) {
        body(h, 1, 2, 1, 1, 2);
        h.runAfterDelay(FORMED, () -> {
            h.assertTrue(joined(h, cell(1, 0, 1), Direction.EAST), "the bottom row joined");
            h.assertTrue(joined(h, cell(1, 0, 1), Direction.UP), "the layers joined");
            h.assertTrue(
                    joined(h, cell(2, 1, 1), Direction.WEST) && joined(h, cell(2, 1, 1), Direction.DOWN),
                    "the top joined");
            h.assertFalse(joined(h, cell(1, 0, 1), Direction.WEST), "the outer face stays a wall");
            h.assertValueEqual(fill(h, cell(2, 1, 1), 10000), 10000, "the top cell takes for the body");
            h.assertValueEqual(share(h, cell(1, 0, 1)), 4000, "the bottom layer filled first");
            h.assertValueEqual(share(h, cell(2, 0, 1)), 4000, "the bottom layer filled first");
            h.assertValueEqual(share(h, cell(1, 1, 1)), 1000, "the rest shared evenly above");
            h.assertValueEqual(share(h, cell(2, 1, 1)), 1000, "the rest shared evenly above");
            h.assertValueEqual(fillLevel(h, cell(1, 0, 1)), ReservoirCellBlock.FILL_STEPS, "the bottom shows full");
            h.assertValueEqual(fillLevel(h, cell(2, 1, 1)), 4, "the top shows a quarter");
            h.assertValueEqual(body(h, cell(1, 0, 1)), 10000, "every cell speaks for the body");
            h.assertValueEqual(
                    ProcessingTests.handler(h, cell(1, 0, 1)).tank().capacity().units(), 16000, "four cells");
            h.succeed();
        });
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void theTopLayerDrainsFirstAndABucketReachesItFromBelow(GameTestHelper h) {
        body(h, 1, 1, 1, 1, 2);
        h.runAfterDelay(FORMED, () -> {
            fill(h, cell(1, 0, 1), 5500);
            Player carrier = EcologyTests.holding(h, GameType.SURVIVAL, Items.BUCKET, 1);
            EcologyTests.use(h, carrier, cell(1, 0, 1), Direction.NORTH);
            h.assertTrue(carrier.getMainHandItem().is(StorageModule.BIOMASS_BUCKET.get()), "the bucket filled");
            h.assertValueEqual(share(h, cell(1, 1, 1)), 500, "the bucket took from the top first");
            h.assertValueEqual(share(h, cell(1, 0, 1)), 4000, "the bottom stays full");
            h.succeed();
        });
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void anIncompleteRowWaitsOutsideWithItsOwnBiomass(GameTestHelper h) {
        body(h, 1, 2, 1, 1, 1);
        h.setBlock(new BlockPos(1, Y, 2), EcologyTests.mature());
        h.setBlock(new BlockPos(2, Y, 2), EcologyTests.mature());
        h.runAfterDelay(FORMED, () -> {
            fill(h, cell(1, 0, 1), 2000);
            h.setBlock(cell(1, 0, 2), StorageModule.RESERVOIR_CELL.get());
            fill(h, cell(1, 0, 2), 700);
        });
        h.runAfterDelay(2 * FORMED, () -> {
            h.assertTrue(alone(h, cell(1, 0, 2)), "the start of a second row waits outside");
            h.assertValueEqual(body(h, cell(1, 0, 2)), 700, "with its own biomass");
            h.assertValueEqual(body(h, cell(1, 0, 1)), 2000, "the body keeps its own");
            h.setBlock(cell(2, 0, 2), StorageModule.RESERVOIR_CELL.get());
        });
        h.runAfterDelay(3 * FORMED, () -> {
            for (BlockPos pos : List.of(cell(1, 0, 1), cell(2, 0, 1), cell(1, 0, 2), cell(2, 0, 2)))
                h.assertValueEqual(share(h, pos), 675, "the full row joined and the biomass levelled at " + pos);
            h.assertTrue(joined(h, cell(1, 0, 2), Direction.NORTH), "the new row joined the body");
            h.succeed();
        });
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void aBrokenCellTakesItsShareAndTheRestFormAgain(GameTestHelper h) {
        body(h, 1, 2, 1, 2, 1);
        h.runAfterDelay(FORMED, () -> {
            h.assertValueEqual(fill(h, cell(1, 0, 1), 8000), 8000, "filled");
            h.getLevel().destroyBlock(h.absolutePos(cell(2, 0, 2)), true);
        });
        h.runAfterDelay(2 * FORMED, () -> {
            List<ItemEntity> drops = h.getEntities(EntityType.ITEM, cell(2, 0, 2), 2.0);
            h.assertValueEqual(drops.size(), 1, "one dropped cell");
            BiomassTank carried = drops.get(0).getItem().get(StorageModule.TANK.get());
            h.assertTrue(carried != null && carried.stored().units() == 2000, "its share on the item");
            int left = share(h, cell(1, 0, 1)) + share(h, cell(2, 0, 1)) + share(h, cell(1, 0, 2));
            h.assertValueEqual(left, 6000, "the rest stayed");
            int bodies = 0;
            for (BlockPos pos : List.of(cell(1, 0, 1), cell(2, 0, 1), cell(1, 0, 2))) if (alone(h, pos)) bodies++;
            h.assertValueEqual(bodies, 1, "two cells formed a row again, one waits alone");
            h.succeed();
        });
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void aBodyStopsAtFourOnASide(GameTestHelper h) {
        body(h, 1, 5, 1, 1, 1);
        h.runAfterDelay(FORMED, () -> {
            int joinedEast = 0;
            for (int x = 1; x <= 5; x++) if (joined(h, cell(x, 0, 1), Direction.EAST)) joinedEast++;
            h.assertValueEqual(joinedEast, 3, "four cells in one body");
            h.assertTrue(alone(h, cell(1, 0, 1)) || alone(h, cell(5, 0, 1)), "the fifth waits alone at one end");
            h.succeed();
        });
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", timeoutTicks = 200)
    public static void aJunctionUnderACornerFillsTheWholeBody(GameTestHelper h) {
        // the junction stands on the ground under the west column; the east column stands on substrate
        for (int x = 1; x <= 2; x++) h.setBlock(new BlockPos(x, Y, 1), EcologyTests.mature());
        h.setBlock(new BlockPos(2, Y + 1, 1), EcologyTests.mature());
        h.setBlock(
                new BlockPos(1, Y + 1, 1),
                LogisticsModule.VASCULAR_JUNCTION
                        .get()
                        .defaultBlockState()
                        .setValue(VascularJunctionBlock.PORT.get(Direction.UP), Port.OUTLET));
        for (int x = 1; x <= 2; x++)
            for (int layer = 1; layer <= 2; layer++) h.setBlock(cell(x, layer, 1), StorageModule.RESERVOIR_CELL.get());
        h.getLevel()
                .getCapability(Capabilities.FluidHandler.BLOCK, h.absolutePos(new BlockPos(1, Y + 1, 1)), null)
                .fill(BiomassFluids.of(Biomass.of(1000)), IFluidHandler.FluidAction.EXECUTE);
        h.runAfterDelay(120, () -> {
            h.assertValueEqual(body(h, cell(2, 2, 1)), 1000, "all of it went into the body");
            h.assertValueEqual(share(h, cell(1, 1, 1)), 500, "the bottom layer shares it");
            h.assertValueEqual(share(h, cell(2, 1, 1)), 500, "the cell beside the junction too");
            h.assertValueEqual(share(h, cell(2, 2, 1)), 0, "nothing above while the bottom has room");
            h.succeed();
        });
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void aCellStandsOnSubstrateACellOrAJunctionNotOnDirt(GameTestHelper h) {
        BlockPos dirt = new BlockPos(1, Y, 1);
        BlockPos substrate = new BlockPos(3, Y, 1);
        BlockPos ground = new BlockPos(5, Y, 1);
        h.setBlock(dirt, Blocks.DIRT);
        h.setBlock(substrate, EcologyTests.mature());
        h.setBlock(ground, EcologyTests.mature());
        h.setBlock(ground.above(), LogisticsModule.VASCULAR_JUNCTION.get());
        Player builder = EcologyTests.holding(h, GameType.SURVIVAL, StorageModule.RESERVOIR_CELL_ITEM.get(), 4);
        h.placeAt(builder, builder.getMainHandItem(), dirt, Direction.UP);
        h.assertBlockPresent(Blocks.AIR, dirt.above());
        h.placeAt(builder, builder.getMainHandItem(), substrate, Direction.UP);
        h.assertBlockPresent(StorageModule.RESERVOIR_CELL.get(), substrate.above());
        h.placeAt(builder, builder.getMainHandItem(), substrate.above(), Direction.UP);
        h.assertBlockPresent(StorageModule.RESERVOIR_CELL.get(), substrate.above(2));
        h.placeAt(builder, builder.getMainHandItem(), ground.above(), Direction.UP);
        h.assertBlockPresent(StorageModule.RESERVOIR_CELL.get(), ground.above(2));
        h.succeed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void aPlacedCellBringsItsBiomassIntoTheBody(GameTestHelper h) {
        body(h, 1, 1, 1, 1, 1);
        h.setBlock(new BlockPos(2, Y, 1), EcologyTests.mature());
        ItemStack carried = new ItemStack(StorageModule.RESERVOIR_CELL_ITEM.get());
        carried.set(StorageModule.TANK.get(), new BiomassTank(Biomass.of(3000), Biomass.ZERO, Biomass.of(4000)));
        Player builder = EcologyTests.holding(h, GameType.SURVIVAL, StorageModule.RESERVOIR_CELL_ITEM.get(), 1);
        builder.setItemInHand(InteractionHand.MAIN_HAND, carried);
        h.placeAt(builder, carried, new BlockPos(2, Y, 1), Direction.UP);
        h.runAfterDelay(FORMED, () -> {
            h.assertTrue(joined(h, cell(1, 0, 1), Direction.EAST), "the placed cell joined");
            h.assertValueEqual(share(h, cell(1, 0, 1)), 1500, "its biomass levelled across the layer");
            h.assertValueEqual(share(h, cell(2, 0, 1)), 1500, "its biomass levelled across the layer");
            h.succeed();
        });
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void anUnreadableCellKeepsItsDataAndStaysOutOfTheBody(GameTestHelper h) {
        body(h, 1, 2, 1, 1, 1);
        BlockPos broken = cell(2, 0, 1);
        CompoundTag data = new CompoundTag();
        CompoundTag tank = new CompoundTag();
        tank.putInt("stored", 500);
        tank.putInt("reserved", 0);
        tank.putInt("capacity", 999);
        data.put("tank", tank);
        h.getBlockEntity(broken).loadWithComponents(data, h.getLevel().registryAccess());
        h.runAfterDelay(FORMED, () -> {
            h.assertTrue(alone(h, broken), "an unreadable cell joins no body");
            h.assertTrue(alone(h, cell(1, 0, 1)), "and the other cannot form a box without it");
            h.assertValueEqual(
                    fluid(h, broken).fill(BiomassFluids.of(Biomass.of(100)), IFluidHandler.FluidAction.EXECUTE),
                    0,
                    "it refuses biomass");
            CompoundTag kept = h.getBlockEntity(broken).collectComponents().get(OrganModule.UNREADABLE_SAVE.get());
            h.assertTrue(kept != null && kept.getCompound("tank").equals(tank), "it keeps its bytes for the item");
            h.succeed();
        });
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void aSavedBodyLoadsBackAndOneSavedElsewhereFormsAgain(GameTestHelper h) {
        body(h, 1, 2, 1, 1, 1);
        BlockPos west = cell(1, 0, 1);
        h.runAfterDelay(FORMED, () -> {
            fill(h, west, 3000);
            BlockEntity cell = h.getBlockEntity(west);
            CompoundTag saved = cell.saveWithFullMetadata(h.getLevel().registryAccess());
            h.assertTrue(saved.contains("body"), "the body is saved");
            cell.loadWithComponents(saved, h.getLevel().registryAccess());
            h.assertValueEqual(body(h, cell(2, 0, 1)), 3000, "it loads back whole");
            // the same cell as if pasted from a structure saved a hundred blocks away
            BlockPos at = h.absolutePos(west);
            saved.putIntArray(
                    "body", new int[] {at.getX() + 100, at.getY(), at.getZ(), at.getX() + 101, at.getY(), at.getZ()});
            cell.loadWithComponents(saved, h.getLevel().registryAccess());
            h.assertValueEqual(
                    fluid(h, cell(2, 0, 1)).fill(BiomassFluids.of(Biomass.of(10)), IFluidHandler.FluidAction.EXECUTE),
                    0,
                    "the other cell finds its body broken and refuses");
            cell.onLoad(); // as the chunk would on loading it
        });
        h.runAfterDelay(3 * FORMED, () -> {
            h.assertTrue(joined(h, west, Direction.EAST), "the two formed again");
            h.assertValueEqual(body(h, west), 3000, "nothing lost");
            h.succeed();
        });
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void aBodyAsWideAsTheWorldIsNoBodyAndTheCellStillForms(GameTestHelper h) {
        body(h, 1, 2, 1, 1, 1);
        BlockPos west = cell(1, 0, 1);
        BlockPos at = h.absolutePos(west);
        CompoundTag data = new CompoundTag();
        CompoundTag tank = new CompoundTag();
        tank.putInt("stored", 1000);
        tank.putInt("reserved", 0);
        tank.putInt("capacity", 4000);
        data.put("tank", tank);
        data.putIntArray(
                "body", new int[] {Integer.MIN_VALUE, at.getY(), at.getZ(), Integer.MAX_VALUE, at.getY(), at.getZ()});
        h.getBlockEntity(west).loadWithComponents(data, h.getLevel().registryAccess());
        h.assertTrue(
                h.getBlockEntity(west).collectComponents().get(OrganModule.UNREADABLE_SAVE.get()) == null,
                "the biomass is readable; only the body is dropped");
        h.runAfterDelay(FORMED, () -> {
            h.assertTrue(joined(h, west, Direction.EAST), "the cell formed with its neighbour");
            h.assertValueEqual(body(h, west), 1000, "with its biomass");
            h.succeed();
        });
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void theRecipeCarriesTheBladdersBiomass(GameTestHelper h) {
        ItemStack bladder = new ItemStack(StorageModule.BIOMASS_BLADDER_ITEM.get());
        bladder.set(StorageModule.TANK.get(), new BiomassTank(Biomass.of(1234), Biomass.ZERO, Biomass.of(4000)));
        CraftingInput input = grid(bladder);
        var recipe = h.getLevel().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input, h.getLevel());
        h.assertTrue(recipe.isPresent(), "the reservoir recipe matches a full Bladder");
        ItemStack made = recipe.get().value().assemble(input, h.getLevel().registryAccess());
        h.assertTrue(made.is(StorageModule.RESERVOIR_CELL_ITEM.get()), "it makes a cell");
        BiomassTank kept = made.get(StorageModule.TANK.get());
        h.assertTrue(kept != null && kept.stored().units() == 1234, "the cell keeps the Bladder's biomass");
        ItemStack unreadable = new ItemStack(StorageModule.BIOMASS_BLADDER_ITEM.get());
        unreadable.set(OrganModule.UNREADABLE_SAVE.get(), new CompoundTag());
        h.assertFalse(
                h.getLevel()
                        .getRecipeManager()
                        .getRecipeFor(RecipeType.CRAFTING, grid(unreadable), h.getLevel())
                        .isPresent(),
                "an unreadable Bladder is not crafted away");
        h.succeed();
    }

    private static CraftingInput grid(ItemStack bladder) {
        List<ItemStack> items = new ArrayList<>(List.of(
                bladder,
                new ItemStack(ProcessingModule.BONE_PLATE.get()),
                new ItemStack(ProcessingModule.BONE_PLATE.get()),
                new ItemStack(ProcessingModule.MEMBRANE_SHEET.get()),
                new ItemStack(ProcessingModule.MEMBRANE_SHEET.get())));
        while (items.size() < 9) items.add(ItemStack.EMPTY);
        return CraftingInput.of(3, 3, items);
    }

    /**
     * A body across a chunk edge refuses while its far half is unloaded; a cell broken meanwhile leaves the far half
     * naming a body that is gone, and once that half loads it falls apart and forms again, losing nothing. Chunks
     * load in rings: one forced chunk keeps the next two loaded, so the near half sits two chunks from a forced chunk
     * and the far half three.
     */
    @GameTest(
            templateNamespace = "infestusfrontier_tests",
            template = "empty",
            batch = "reservoir_unload",
            timeoutTicks = 900)
    public static void aBodyAcrossAnUnloadedChunkRefusesAndHealsWhenItLoads(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        BlockPos origin = h.absolutePos(BlockPos.ZERO);
        BlockPos anchor = new BlockPos((origin.getX() & ~15) + 192000, 80, (origin.getZ() & ~15) + 256008);
        // two cells at the east end of the chunk two east of the anchor, two at the west end of the next
        BlockPos west = anchor.east(46);
        ChunkPos kept = new ChunkPos(anchor);
        ChunkPos near = new ChunkPos(west);
        ChunkPos far = new ChunkPos(west.east(2));
        level.setChunkForced(kept.x, kept.z, true);
        level.setChunkForced(near.x, near.z, true);
        level.setChunkForced(far.x, far.z, true);
        List<BlockPos> cells = List.of(west, west.east(), west.east(2), west.east(3));
        for (BlockPos pos : cells) {
            level.setBlockAndUpdate(pos.below(), EcologyTests.mature());
            level.setBlockAndUpdate(pos, StorageModule.RESERVOIR_CELL.get().defaultBlockState());
        }
        IFluidHandler[] port = new IFluidHandler[1];
        h.startSequence()
                .thenWaitUntil(() -> h.assertTrue(
                        level.getBlockState(west).getValue(ReservoirCellBlock.JOINED.get(Direction.EAST))
                                && level.getBlockState(west.east(2))
                                        .getValue(ReservoirCellBlock.JOINED.get(Direction.EAST)),
                        "the row of four formed"))
                .thenExecute(() -> {
                    port[0] = level.getCapability(Capabilities.FluidHandler.BLOCK, west, Direction.UP);
                    h.assertValueEqual(
                            port[0].fill(BiomassFluids.of(Biomass.of(8000)), IFluidHandler.FluidAction.EXECUTE),
                            8000,
                            "filled whole");
                    level.setChunkForced(near.x, near.z, false);
                    level.setChunkForced(far.x, far.z, false);
                })
                .thenWaitUntil(() -> h.assertTrue(
                        level.hasChunkAt(west) && !level.hasChunkAt(west.east(2)),
                        "the near half stays loaded, the far half unloaded"))
                .thenExecute(() -> {
                    h.assertValueEqual(
                            port[0].fill(BiomassFluids.of(Biomass.of(100)), IFluidHandler.FluidAction.EXECUTE),
                            0,
                            "a body with a cell out of reach refuses");
                    level.destroyBlock(west, false);
                    h.assertTrue(
                            !level.getBlockState(west.east()).getValue(ReservoirCellBlock.JOINED.get(Direction.EAST)),
                            "the near cell left behind stands alone");
                    h.assertValueEqual(
                            level.getCapability(Capabilities.FluidHandler.BLOCK, west.east(), Direction.UP)
                                    .getFluidInTank(0)
                                    .getAmount(),
                            2000,
                            "with its share");
                    level.setChunkForced(near.x, near.z, true);
                    level.setChunkForced(far.x, far.z, true);
                })
                .thenWaitUntil(() -> h.assertTrue(
                        level.hasChunkAt(west.east(3))
                                && level.getBlockState(west.east())
                                        .getValue(ReservoirCellBlock.JOINED.get(Direction.EAST))
                                && level.getBlockState(west.east(2))
                                        .getValue(ReservoirCellBlock.JOINED.get(Direction.EAST)),
                        "the far half fell apart and formed one row of three with the near cell"))
                .thenExecute(() -> h.assertValueEqual(
                        level.getCapability(Capabilities.FluidHandler.BLOCK, west.east(), Direction.UP)
                                .getFluidInTank(0)
                                .getAmount(),
                        6000,
                        "nothing lost"))
                .thenExecute(() -> {
                    level.setChunkForced(kept.x, kept.z, false);
                    level.setChunkForced(near.x, near.z, false);
                    level.setChunkForced(far.x, far.z, false);
                })
                .thenSucceed();
    }
}
