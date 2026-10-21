package org.jd.infestusfrontier.logistics;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import org.jd.infestusfrontier.organ.api.Organs;
import org.jetbrains.annotations.Nullable;

/**
 * A port organ: it stands beside a functional organ, faces it and joins it. A Feeding Lobe only puts items into the
 * organ; a Yield Lobe only takes what the organ offers, its products. A Mouth under the lobe, or a hopper, works it.
 * An organ serves one lobe of each kind while young and one more for each growth stage; lobes beyond that are idle,
 * counted in the order north, east, south, west around the organ. A lobe keeps no state of its own.
 */
public final class LobeBlock extends HorizontalDirectionalBlock {
    private static final VoxelShape SHAPE = Block.box(2, 0, 2, 14, 12, 14);

    private final boolean feeds;
    private final MapCodec<LobeBlock> codec;

    LobeBlock(boolean feeds, Properties properties) {
        super(properties);
        this.feeds = feeds;
        this.codec = simpleCodec(p -> new LobeBlock(feeds, p));
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<LobeBlock> codec() {
        return codec;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    /**
     * On a foundation only. It faces the organ whose side was clicked, else the first organ beside it, else away
     * from the player.
     */
    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        if (!Organs.hasFoundation(level, pos)) return null;
        Direction clicked = context.getClickedFace().getOpposite();
        if (clicked.getAxis().isHorizontal() && isHost(level, pos.relative(clicked)))
            return defaultBlockState().setValue(FACING, clicked);
        for (Direction side : Direction.Plane.HORIZONTAL)
            if (isHost(level, pos.relative(side))) return defaultBlockState().setValue(FACING, side);
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection());
    }

    private static boolean isHost(Level level, BlockPos pos) {
        return level.hasChunkAt(pos) && level.getBlockState(pos).is(Organs.PORT_HOSTS);
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    /** What the lobe does now, as the player reads it. */
    private Component status(Level level, BlockPos pos, BlockState state) {
        BlockPos host = pos.relative(state.getValue(FACING));
        if (!isHost(level, host)) return Component.translatable("infestusfrontier.status.lobe.no_organ");
        BlockState organ = level.getBlockState(host);
        int slots = slots(organ);
        int place = place(level, pos, state);
        Component name = organ.getBlock().getName();
        return place < slots
                ? Component.translatable("infestusfrontier.status.lobe.serving", name, place + 1, slots)
                : Component.translatable("infestusfrontier.status.lobe.idle", name, slots);
    }

    @Override
    protected InteractionResult useWithoutItem(
            BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide) player.displayClientMessage(status(level, pos, state), true);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    /** Lobes of one kind an organ in {@code organ} serves: one, and one more for each growth stage. */
    static int slots(BlockState organ) {
        return 1 + Organs.stage(organ);
    }

    /**
     * This lobe's place among the lobes of its kind facing the same organ, counting north, east, south, west; a
     * value at or past the organ's slots means idle. Four loaded block reads at most.
     */
    private int place(Level level, BlockPos pos, BlockState state) {
        BlockPos host = pos.relative(state.getValue(FACING));
        int place = 0;
        for (Direction side : Direction.Plane.HORIZONTAL) {
            BlockPos at = host.relative(side);
            if (at.equals(pos)) return place;
            if (!level.hasChunkAt(at)) continue;
            BlockState other = level.getBlockState(at);
            if (other.is(this) && other.getValue(FACING) == side.getOpposite()) place++;
        }
        return place;
    }

    /**
     * The lobe's item handler: the organ's own, one way, while this lobe is served; null otherwise. The organ is
     * read only when its chunk is loaded.
     */
    @Nullable IItemHandler handler(Level level, BlockPos pos, BlockState state) {
        BlockPos host = pos.relative(state.getValue(FACING));
        if (!isHost(level, host)) return null;
        if (place(level, pos, state) >= slots(level.getBlockState(host))) return null;
        IItemHandler organ = level.getCapability(
                Capabilities.ItemHandler.BLOCK, host, state.getValue(FACING).getOpposite());
        return organ == null ? null : new OneWay(organ, feeds);
    }

    /** An organ's handler that only takes items in, or only gives them out. */
    private record OneWay(IItemHandler organ, boolean in) implements IItemHandler {
        @Override
        public int getSlots() {
            return organ.getSlots();
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return organ.getStackInSlot(slot);
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            return in ? organ.insertItem(slot, stack, simulate) : stack;
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return in ? ItemStack.EMPTY : organ.extractItem(slot, amount, simulate);
        }

        @Override
        public int getSlotLimit(int slot) {
            return organ.getSlotLimit(slot);
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return in && organ.isItemValid(slot, stack);
        }
    }
}
