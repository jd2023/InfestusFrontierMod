package org.jd.infestusfrontier.testmod;

import static org.jd.infestusfrontier.testmod.ShapingPalmTests.stack;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.TagParser;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import org.jd.infestusfrontier.logistics.LogisticsModule;
import org.jd.infestusfrontier.logistics.MouthBlock;
import org.jd.infestusfrontier.logistics.MouthMode;
import org.jd.infestusfrontier.processing.ProcessingModule;
import org.jd.infestusfrontier.testmod.ShapingPalmTests.MenuPlayer;

/**
 * What the Palm tests build: a Shaping Palm that stands on a Mouth, Item Veins along a row, and Mouths with chests. The
 * Palm's Mouth is at y=1 and the Palm at y=2; every far Mouth stands at y=1 and its store above it.
 */
final class PalmNet {
    static final String STATUS = "infestusfrontier.status.palm.";
    static final String REFUSAL = "infestusfrontier.refusal.processing.";
    static final String ACCESS = "infestusfrontier.refusal.logistics.";
    /** The Palm of a standard rig. */
    static final BlockPos PALM = new BlockPos(2, 2, 2);
    /** Ticks that let the network discover itself and the Mouths' first pulse pass. */
    static final int SETTLE = 20;

    /** A Palm with Reach and its Mouth, and a far Mouth {@code distance} blocks along the row with a chest on it. */
    record Rig(BlockPos palm, BlockPos far) {
        BlockPos door() {
            return palm.below();
        }

        BlockPos chest() {
            return far.above();
        }
    }

    private PalmNet() {}

    static void use(GameTestHelper h, MenuPlayer player, ItemStack held, BlockPos at) {
        player.setItemInHand(InteractionHand.MAIN_HAND, held);
        BlockPos absolute = h.absolutePos(at);
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(absolute), Direction.UP, absolute, false);
        player.moveTo(absolute.getX() + 0.5, absolute.getY() + 1, absolute.getZ() - 1.5);
        player.gameMode.useItemOn(player, h.getLevel(), player.getMainHandItem(), InteractionHand.MAIN_HAND, hit);
    }

    /** Gives the Palm at {@code at} Reach with one Contractile Fiber used on it, as a player does. */
    static void reach(GameTestHelper h, BlockPos at) {
        MenuPlayer player = new MenuPlayer(h.getLevel(), "palm_fiber");
        use(h, player, stack(LogisticsModule.CONTRACTILE_FIBER.get(), 1), at);
        h.assertTrue(
                h.getBlockEntity(at).collectComponents().getOrDefault(ProcessingModule.PALM_REACH.get(), false)
                        == Boolean.TRUE,
                "the fiber gave the Palm Reach");
    }

    static void mouth(GameTestHelper h, BlockPos cell, MouthMode mode, String settings) {
        h.setBlock(cell, LogisticsModule.MOUTH.get().defaultBlockState().setValue(MouthBlock.MODE, mode));
        if (settings == null) return;
        try {
            h.getBlockEntity(cell)
                    .loadCustomOnly(TagParser.parseTag(settings), h.getLevel().registryAccess());
        } catch (CommandSyntaxException e) {
            h.fail("bad Mouth settings " + settings + ": " + e.getMessage());
        }
    }

    /** A chest on the Mouth cell {@code cell}, holding {@code contents} from its first slot. */
    static ChestBlockEntity chest(GameTestHelper h, BlockPos cell, ItemStack... contents) {
        h.setBlock(cell.above(), Blocks.CHEST);
        ChestBlockEntity box = h.getBlockEntity(cell.above());
        for (int slot = 0; slot < contents.length; slot++) box.setItem(slot, contents[slot]);
        return box;
    }

    /** Item Veins in a straight row from {@code from} to {@code to}, both included. */
    static void veins(GameTestHelper h, BlockPos from, BlockPos to) {
        for (BlockPos at : BlockPos.betweenClosed(from, to))
            h.setBlock(at.immutable(), LogisticsModule.ITEM_VEIN.get());
    }

    /** A Palm at {@code palm}, on a Mouth set to {@code mode}, given Reach. */
    static BlockPos palm(GameTestHelper h, BlockPos palm, MouthMode mode) {
        mouth(h, palm.below(), mode, null);
        h.setBlock(palm, ProcessingModule.SHAPING_PALM.get());
        reach(h, palm);
        return palm;
    }

    /**
     * The standard rig: the Palm at (2, 2, 2) on a Mouth set to {@code door}, veins along x, and a far Mouth set to
     * {@code far} {@code distance} blocks east of the Palm's Mouth, with a chest on it holding {@code contents}.
     */
    static Rig rig(
            GameTestHelper h, int distance, MouthMode door, MouthMode far, String farSettings, ItemStack... contents) {
        BlockPos at = palm(h, PALM, door);
        BlockPos cell = new BlockPos(PALM.getX() + distance, PALM.getY() - 1, PALM.getZ());
        veins(h, at.below().east(), cell.west());
        mouth(h, cell, far, farSettings);
        chest(h, cell, contents);
        return new Rig(at, cell);
    }

    static Rig rig(GameTestHelper h, int distance, MouthMode door, MouthMode far, ItemStack... contents) {
        return rig(h, distance, door, far, null, contents);
    }

    /** How many {@code item} the store above {@code cell} holds, read through its item handler. */
    static int count(GameTestHelper h, BlockPos cell, Item item) {
        IItemHandler handler =
                h.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, h.absolutePos(cell.above()), null);
        int total = 0;
        for (int slot = 0; slot < handler.getSlots(); slot++)
            if (handler.getStackInSlot(slot).is(item))
                total += handler.getStackInSlot(slot).getCount();
        return total;
    }
}
