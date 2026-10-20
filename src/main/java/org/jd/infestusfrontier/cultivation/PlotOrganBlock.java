package org.jd.infestusfrontier.cultivation;

import com.mojang.serialization.MapCodec;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jd.infestusfrontier.organ.api.Organs;
import org.jd.infestusfrontier.storage.api.BiomassFluids;
import org.jetbrains.annotations.Nullable;

/**
 * An organ that works a {@link Plot}: the Planting Proboscis and the Harvest Corolla. Buckets move biomass,
 * planting stock goes into the planter, anything else opens the screen, sneaking builds against it. {@code working}
 * is true while the last pulse did something.
 */
public final class PlotOrganBlock extends BaseEntityBlock {
    public static final BooleanProperty WORKING = BooleanProperty.create("working");

    private final Supplier<BlockEntityType<? extends PlotOrganBlockEntity>> type;
    private final VoxelShape shape;
    private final MapCodec<PlotOrganBlock> codec;

    PlotOrganBlock(
            Properties properties, Supplier<BlockEntityType<? extends PlotOrganBlockEntity>> type, VoxelShape shape) {
        super(properties);
        this.type = type;
        this.shape = shape;
        this.codec = simpleCodec(p -> new PlotOrganBlock(p, type, shape));
        registerDefaultState(stateDefinition.any().setValue(WORKING, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(WORKING);
    }

    @Override
    protected MapCodec<PlotOrganBlock> codec() {
        return codec;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return shape;
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        return Organs.hasFoundation(context.getLevel(), context.getClickedPos()) ? defaultBlockState() : null;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return type.get().create(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(
            Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide) return null;
        return createTickerHelper(type, this.type.get(), PlotOrganBlockEntity::serverTick);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof PlotOrganBlockEntity organ)
            organ.dropItems(level, pos);
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    protected ItemInteractionResult useItemOn(
            ItemStack stack,
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            InteractionHand hand,
            BlockHitResult hit) {
        ItemInteractionResult fluid = BiomassFluids.useOn(stack, level, pos, hit.getDirection(), player, hand);
        if (fluid != ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION) return fluid;
        if (!(level.getBlockEntity(pos) instanceof PlotOrganBlockEntity organ) || !organ.takesByHand(stack))
            return Organs.openScreen(stack, level, pos, player);
        if (level.isClientSide) return ItemInteractionResult.SUCCESS;
        ItemStack offered = player.getAbilities().instabuild ? stack.copy() : stack;
        return organ.insertByHand(offered) ? ItemInteractionResult.CONSUME : ItemInteractionResult.FAIL;
    }

    @Override
    protected InteractionResult useWithoutItem(
            BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        return Organs.openScreen(level, pos, player).result();
    }
}
