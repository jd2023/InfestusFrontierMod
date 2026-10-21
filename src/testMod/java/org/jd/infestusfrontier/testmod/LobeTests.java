package org.jd.infestusfrontier.testmod;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.IItemHandler;
import org.jd.infestusfrontier.core.foundation.Biomass;
import org.jd.infestusfrontier.logistics.LogisticsModule;
import org.jd.infestusfrontier.logistics.MouthBlock;
import org.jd.infestusfrontier.logistics.MouthMode;
import org.jd.infestusfrontier.organ.api.Organs;
import org.jd.infestusfrontier.processing.ProcessingModule;
import org.jd.infestusfrontier.storage.api.BiomassCapabilities;

/**
 * Feeding and Yield Lobes beside an organ: a Loom run from chest to chest through lobes on Mouths, one-way handlers,
 * the number of lobes an organ's stage serves, a lobe facing nothing, and an organ across an unloaded chunk.
 * Cells sit at y=1, organs and lobes at y=2.
 */
@GameTestHolder("infestusfrontier_tests")
@PrefixGameTestTemplate(false)
public final class LobeTests {
    private static final int Y = 1;

    private LobeTests() {}

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", timeoutTicks = 600)
    public static void lobesOnMouthsRunALoomFromChestToChest(GameTestHelper h) {
        // chest > Mouth (output) > vein > Mouth (intake) under the Feeding Lobe | Loom | Yield Lobe on a Mouth (output)
        // > vein > Mouth (intake) under the plate chest; the two networks do not touch
        BlockPos loom = cell(4).above();
        h.setBlock(cell(4), EcologyTests.mature());
        mouth(h, cell(1), MouthMode.OUTPUT);
        h.setBlock(cell(2), LogisticsModule.ITEM_VEIN.get());
        mouth(h, cell(3), MouthMode.INTAKE);
        mouth(h, cell(5), MouthMode.OUTPUT);
        h.setBlock(cell(6), LogisticsModule.ITEM_VEIN.get());
        mouth(h, cell(7), MouthMode.INTAKE);
        ChestBlockEntity source = chest(h, cell(1));
        ChestBlockEntity target = chest(h, cell(7));
        source.setItem(0, new ItemStack(Items.BONE, 2));
        h.setBlock(loom, ProcessingModule.BONE_LOOM.get());
        h.getLevel()
                .getCapability(BiomassCapabilities.BLOCK, h.absolutePos(loom), null)
                .insert(Biomass.of(200), false);
        lobe(h, cell(3).above(), true, Direction.EAST);
        lobe(h, cell(5).above(), false, Direction.WEST);
        h.runAfterDelay(500, () -> {
            h.assertValueEqual(count(target, ProcessingModule.BONE_PLATE.get()), 1, "a plate reached the chest");
            h.assertValueEqual(count(target, Items.BONE), 0, "no bone went past the organ");
            h.assertTrue(count(source, Items.BONE) <= 1, "the loom took its bones from the source chest");
            h.succeed();
        });
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void aFeedingLobeOnlyFeedsAndAYieldLobeOnlyYields(GameTestHelper h) {
        BlockPos loom = new BlockPos(2, Y + 1, 2);
        h.setBlock(loom.below(), EcologyTests.mature());
        h.setBlock(loom, ProcessingModule.BONE_LOOM.get());
        lobe(h, loom.west(), true, Direction.EAST);
        lobe(h, loom.east(), false, Direction.WEST);
        IItemHandler feeding = handler(h, loom.west());
        IItemHandler yield = handler(h, loom.east());
        h.assertTrue(feeding != null && yield != null, "both lobes serve the loom");
        h.assertTrue(insertAnywhere(feeding, new ItemStack(Items.BONE)).isEmpty(), "the feeding lobe takes a bone");
        h.assertTrue(extractAnything(feeding).isEmpty(), "and gives nothing back");
        h.assertTrue(extractAnything(yield).isEmpty(), "the yield lobe never gives the ingredient");
        h.assertFalse(insertAnywhere(yield, new ItemStack(Items.BONE)).isEmpty(), "and takes nothing in");
        h.succeed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void anOrganServesOneLobeOfAKindAndOneMoreForEachStage(GameTestHelper h) {
        BlockPos furnace = new BlockPos(2, Y + 1, 2);
        h.setBlock(furnace.below(), EcologyTests.mature());
        h.setBlock(furnace, ProcessingModule.BIO_FURNACE.get());
        lobe(h, furnace.north(), true, Direction.SOUTH);
        lobe(h, furnace.east(), true, Direction.WEST);
        lobe(h, furnace.west(), true, Direction.EAST);
        h.assertTrue(handler(h, furnace.north()) != null, "the young furnace serves its first lobe");
        h.assertTrue(handler(h, furnace.east()) == null, "but not a second");
        h.assertTrue(handler(h, furnace.west()) == null, "or a third");
        h.setBlock(furnace, h.getBlockState(furnace).setValue(Organs.STAGE, 1));
        h.assertTrue(handler(h, furnace.east()) != null, "a grown furnace serves the second, east before west");
        h.assertTrue(handler(h, furnace.west()) == null, "but not the third");
        lobe(h, furnace.south(), false, Direction.NORTH);
        h.assertTrue(handler(h, furnace.south()) != null, "a lobe of the other kind counts apart");
        h.succeed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void aLobeFacingNoOrganServesNothing(GameTestHelper h) {
        BlockPos at = new BlockPos(2, Y + 1, 2);
        h.setBlock(at.below(), EcologyTests.mature());
        h.setBlock(at.east(), Blocks.CHEST);
        lobe(h, at, true, Direction.EAST);
        h.assertTrue(handler(h, at) == null, "a chest is not an organ a lobe joins");
        h.succeed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", batch = "lobe_boundary")
    public static void aLobeAtAChunkEdgeNeverLoadsItsOrgansChunk(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        BlockPos origin = h.absolutePos(BlockPos.ZERO);
        // x ends in 15: the organ the lobe faces would stand in the next chunk, which nothing has loaded
        BlockPos edge = new BlockPos((origin.getX() & ~15) + 176015, 80, (origin.getZ() & ~15) + 176008);
        level.getChunkAt(edge);
        int quiet = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;
        level.setBlock(edge.below(), Blocks.STONE.defaultBlockState(), quiet); // substrate would look around
        level.setBlock(
                edge,
                LogisticsModule.FEEDING_LOBE
                        .get()
                        .defaultBlockState()
                        .setValue(HorizontalDirectionalBlock.FACING, Direction.EAST),
                quiet);
        h.assertFalse(level.hasChunkAt(edge.east()), "the organ's chunk starts unloaded");
        h.assertTrue(level.getCapability(Capabilities.ItemHandler.BLOCK, edge, Direction.DOWN) == null, "no handler");
        h.assertFalse(level.hasChunkAt(edge.east()), "and the lookup loaded nothing");
        h.succeed();
    }

    // --- fixtures ---

    private static BlockPos cell(int x) {
        return new BlockPos(x, Y, 1);
    }

    private static void mouth(GameTestHelper h, BlockPos at, MouthMode mode) {
        h.setBlock(at, LogisticsModule.MOUTH.get().defaultBlockState().setValue(MouthBlock.MODE, mode));
    }

    private static void lobe(GameTestHelper h, BlockPos at, boolean feeds, Direction toOrgan) {
        BlockState state = (feeds ? LogisticsModule.FEEDING_LOBE : LogisticsModule.YIELD_LOBE)
                .get()
                .defaultBlockState()
                .setValue(HorizontalDirectionalBlock.FACING, toOrgan);
        if (!h.getBlockState(at.below()).is(Organs.FOUNDATIONS)) h.setBlock(at.below(), EcologyTests.mature());
        h.setBlock(at, state);
    }

    private static IItemHandler handler(GameTestHelper h, BlockPos at) {
        return h.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, h.absolutePos(at), Direction.DOWN);
    }

    private static ChestBlockEntity chest(GameTestHelper h, BlockPos cell) {
        h.setBlock(cell.above(), Blocks.CHEST);
        return (ChestBlockEntity) h.getBlockEntity(cell.above());
    }

    private static int count(ChestBlockEntity chest, Item item) {
        return chest.countItem(item);
    }

    private static ItemStack insertAnywhere(IItemHandler handler, ItemStack stack) {
        for (int slot = 0; slot < handler.getSlots() && !stack.isEmpty(); slot++)
            stack = handler.insertItem(slot, stack, false);
        return stack;
    }

    private static ItemStack extractAnything(IItemHandler handler) {
        for (int slot = 0; slot < handler.getSlots(); slot++) {
            ItemStack out = handler.extractItem(slot, 64, false);
            if (!out.isEmpty()) return out;
        }
        return ItemStack.EMPTY;
    }
}
