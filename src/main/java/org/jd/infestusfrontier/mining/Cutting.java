package org.jd.infestusfrontier.mining;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.piston.PistonHeadBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.PushReaction;

/**
 * What the cutting organs share: whether a block can be cut at a stone pickaxe's grade, its ordinary drops rolled
 * once in its real loot context, whether the chunks around a cut are loaded, and how drops are added to a finite set
 * of slots. The Tunnel Root and the Fracture Jaw each name their own reasons.
 */
final class Cutting {
    /** What a block is to a cutter. */
    enum Verdict {
        /** Air: there is nothing to cut. */
        CLEAR,
        CUTTABLE,
        /** The block holds fluid. */
        FLUID,
        /** The block holds a block entity, or is a piston arm. */
        UNSUPPORTED,
        /** A stone pickaxe does not harvest it, or nothing breaks it. */
        TOO_HARD
    }

    private Cutting() {}

    /**
     * What the block at {@code cut} is to a stone-grade cutter. A piston arm is refused: removing it breaks its piston
     * with drops whatever the update flags, so the piston would drop loose or twice.
     */
    static Verdict judge(Level level, BlockPos cut) {
        BlockState state = level.getBlockState(cut);
        if (!state.getFluidState().isEmpty()) return Verdict.FLUID;
        if (state.isAir()) return Verdict.CLEAR;
        if (state.hasBlockEntity() || state.getBlock() instanceof PistonHeadBlock) return Verdict.UNSUPPORTED;
        if (!withinGrade(state, level, cut)) return Verdict.TOO_HARD;
        return Verdict.CUTTABLE;
    }

    /** True for a breakable block a stone pickaxe harvests: one that needs no tool, or one stone is enough for. */
    static boolean withinGrade(BlockState state, Level level, BlockPos pos) {
        return state.getDestroySpeed(level, pos) >= 0
                && (!state.requiresCorrectToolForDrops()
                        || new ItemStack(Items.STONE_PICKAXE).isCorrectToolForDrops(state));
    }

    /** The ordinary drops of the block at {@code cut}, rolled once in its real loot context with a stone pickaxe. */
    static List<ItemStack> loot(ServerLevel level, BlockPos cut) {
        BlockState state = level.getBlockState(cut);
        if (state.isAir()) return List.of();
        return Block.getDrops(state, level, cut, null, null, new ItemStack(Items.STONE_PICKAXE));
    }

    /**
     * True when a fragile block touches {@code cut}: one that vanilla breaks loose when it is pushed or loses its
     * support (push reaction DESTROY: torches, signs, ladders, plants, carpets, doors, lanterns, redstone parts), or a
     * rail. Such a block may rest on the cut and fall off with drops once it is gone, so the cut is refused. Reads the
     * six neighbours; changes nothing.
     */
    static boolean touchesFragile(Level level, BlockPos cut) {
        for (Direction side : Direction.values()) {
            BlockState neighbour = level.getBlockState(cut.relative(side));
            if (neighbour.isAir() || neighbour.getBlock() instanceof LiquidBlock) continue;
            if (neighbour.getPistonPushReaction() == PushReaction.DESTROY || neighbour.is(BlockTags.RAILS)) return true;
        }
        return false;
    }

    /** True when every chunk within {@code reach} blocks of {@code pos} is loaded. Nothing is loaded to find out. */
    static boolean loaded(Level level, BlockPos pos, int reach) {
        int lowX = SectionPos.blockToSectionCoord(pos.getX() - reach);
        int highX = SectionPos.blockToSectionCoord(pos.getX() + reach);
        int lowZ = SectionPos.blockToSectionCoord(pos.getZ() - reach);
        int highZ = SectionPos.blockToSectionCoord(pos.getZ() + reach);
        for (int x = lowX; x <= highX; x++) for (int z = lowZ; z <= highZ; z++) if (!level.hasChunk(x, z)) return false;
        return true;
    }

    /**
     * Copies of {@code slots} with {@code drops} added, filling stacks of the same item and components first and then
     * empty slots; or empty when they do not all fit. The slots and the drops are not changed.
     */
    static Optional<List<ItemStack>> merged(List<ItemStack> slots, List<ItemStack> drops) {
        List<ItemStack> after = new ArrayList<>(slots.size());
        for (ItemStack stack : slots) after.add(stack.copy());
        for (ItemStack drop : drops) {
            ItemStack left = drop.copy();
            for (ItemStack slot : after) {
                if (left.isEmpty()) break;
                if (slot.isEmpty() || !ItemStack.isSameItemSameComponents(slot, left)) continue;
                int moved = Math.min(left.getCount(), slot.getMaxStackSize() - slot.getCount());
                slot.grow(moved);
                left.shrink(moved);
            }
            if (left.isEmpty()) continue;
            int free = indexOfEmpty(after);
            if (free < 0) return Optional.empty();
            after.set(free, left);
        }
        return Optional.of(after);
    }

    private static int indexOfEmpty(List<ItemStack> slots) {
        for (int slot = 0; slot < slots.size(); slot++) if (slots.get(slot).isEmpty()) return slot;
        return -1;
    }
}
