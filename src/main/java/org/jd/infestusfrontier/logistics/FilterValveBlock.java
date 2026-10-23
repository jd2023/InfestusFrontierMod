package org.jd.infestusfrontier.logistics;

import com.mojang.serialization.MapCodec;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.jd.infestusfrontier.organ.api.Organs;
import org.jd.infestusfrontier.storage.api.BiomassFluids;
import org.jetbrains.annotations.Nullable;

/**
 * A Filter Valve stands between a store of fluid or biomass and whatever drains it: it faces the store, and a Junction
 * intake or a bucket on its other sides drains the store through it, never below the reserve. Filling goes through
 * unchanged. The reserve is a block state, stepped by a stick and read with any other hand; the valve keeps no other
 * state, and its fluid handler looks the store up on every call.
 */
public final class FilterValveBlock extends HorizontalDirectionalBlock {
    /** The reserves a valve can keep, in mB (biomass counts 1 mB as 1 BU). */
    static final List<Integer> RESERVES = List.of(0, 250, 500, 1000, 2000, 4000, 8000, 16000);

    public static final IntegerProperty RESERVE = IntegerProperty.create("reserve", 0, RESERVES.size() - 1);
    private static final VoxelShape SHAPE = Block.box(2, 0, 2, 14, 12, 14);
    private static final MapCodec<FilterValveBlock> CODEC = simpleCodec(FilterValveBlock::new);

    FilterValveBlock(Properties properties) {
        super(properties);
        registerDefaultState(
                stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(RESERVE, 0));
    }

    @Override
    protected MapCodec<FilterValveBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, RESERVE);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    /** On a foundation only, facing the side clicked, else away from the player. */
    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        if (!Organs.hasFoundation(context.getLevel(), context.getClickedPos())) return null;
        Direction clicked = context.getClickedFace().getOpposite();
        Direction facing = clicked.getAxis().isHorizontal() ? clicked : context.getHorizontalDirection();
        return defaultBlockState().setValue(FACING, facing);
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState old, boolean moved) {
        if (!level.isClientSide && !old.is(this)) level.invalidateCapabilities(pos);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState next, boolean moved) {
        if (!level.isClientSide && !state.is(next.getBlock())) level.invalidateCapabilities(pos);
        super.onRemove(state, level, pos, next, moved);
    }

    static int reserve(BlockState state) {
        return RESERVES.get(state.getValue(RESERVE));
    }

    /**
     * A bucket drains or fills the store through the valve, by the face it clicks; the face toward the store offers
     * nothing. A stick steps the reserve up, wrapping to none after the largest.
     */
    @Override
    protected ItemInteractionResult useItemOn(
            ItemStack stack,
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            InteractionHand hand,
            BlockHitResult hit) {
        if (!stack.is(Items.STICK)) return BiomassFluids.useOn(stack, level, pos, hit.getDirection(), player, hand);
        if (!level.isClientSide) {
            BlockState next = state.setValue(RESERVE, (state.getValue(RESERVE) + 1) % RESERVES.size());
            level.setBlock(pos, next, Block.UPDATE_ALL);
            player.displayClientMessage(status(next), true);
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected InteractionResult useWithoutItem(
            BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide) player.displayClientMessage(status(state), true);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    private static Component status(BlockState state) {
        return Component.translatable("infestusfrontier.status.filter_valve", reserve(state));
    }

    /** The valve's fluid handler, live: every call finds the store and the reserve again. */
    IFluidHandler handler(Level level, BlockPos pos) {
        return new Valve(level, pos.immutable());
    }

    /**
     * The store's handler, or null when the valve is gone, the store's chunk is unloaded, or the faced block is
     * another valve, a Septum Crossing or a Vascular Stitch: none of them is a store, and since all of them pass a
     * call on, a ring of them could call itself.
     */
    private static @Nullable IFluidHandler store(Level level, BlockPos pos) {
        if (!level.hasChunkAt(pos)) return null;
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof FilterValveBlock)) return null;
        Direction facing = state.getValue(FACING);
        BlockPos store = pos.relative(facing);
        if (!level.hasChunkAt(store)) return null;
        Block faced = level.getBlockState(store).getBlock();
        if (faced instanceof FilterValveBlock
                || faced instanceof SeptumCrossingBlock
                || faced instanceof VascularStitchBlock) return null;
        return level.getCapability(Capabilities.FluidHandler.BLOCK, store, facing.getOpposite());
    }

    private record Valve(Level level, BlockPos pos) implements IFluidHandler {
        private @Nullable IFluidHandler store() {
            return FilterValveBlock.store(level, pos);
        }

        /**
         * What of {@code fluid} may leave the store now: what it holds of that fluid above the reserve. Water and
         * biomass in one organ keep their reserves apart.
         */
        private int allowance(IFluidHandler store, FluidStack fluid) {
            long held = 0;
            for (int tank = 0; tank < store.getTanks(); tank++)
                if (FluidStack.isSameFluidSameComponents(store.getFluidInTank(tank), fluid))
                    held += store.getFluidInTank(tank).getAmount();
            return (int) Math.max(0, Math.min(Integer.MAX_VALUE, held - reserve(level.getBlockState(pos))));
        }

        @Override
        public int getTanks() {
            IFluidHandler store = store();
            return store == null ? 0 : store.getTanks();
        }

        @Override
        public FluidStack getFluidInTank(int tank) {
            IFluidHandler store = store();
            return store == null || tank >= store.getTanks() ? FluidStack.EMPTY : store.getFluidInTank(tank);
        }

        @Override
        public int getTankCapacity(int tank) {
            IFluidHandler store = store();
            return store == null || tank >= store.getTanks() ? 0 : store.getTankCapacity(tank);
        }

        @Override
        public boolean isFluidValid(int tank, FluidStack stack) {
            IFluidHandler store = store();
            return store != null && tank < store.getTanks() && store.isFluidValid(tank, stack);
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            IFluidHandler store = store();
            return store == null ? 0 : store.fill(resource, action);
        }

        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
            IFluidHandler store = store();
            if (store == null || resource.isEmpty()) return FluidStack.EMPTY;
            int allowed = Math.min(resource.getAmount(), allowance(store, resource));
            return allowed <= 0 ? FluidStack.EMPTY : store.drain(resource.copyWithAmount(allowed), action);
        }

        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            IFluidHandler store = store();
            if (store == null) return FluidStack.EMPTY;
            FluidStack first = store.drain(maxDrain, FluidAction.SIMULATE);
            if (first.isEmpty()) return FluidStack.EMPTY;
            int allowed = Math.min(maxDrain, allowance(store, first));
            return allowed <= 0 ? FluidStack.EMPTY : store.drain(first.copyWithAmount(allowed), action);
        }
    }
}
