package org.jd.infestusfrontier.processing;

import com.mojang.serialization.MapCodec;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
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
import org.jd.infestusfrontier.core.foundation.Refusal;
import org.jd.infestusfrontier.foundation.Refusals;
import org.jd.infestusfrontier.organ.api.Organs;
import org.jd.infestusfrontier.storage.api.BiomassFluids;
import org.jetbrains.annotations.Nullable;

/**
 * The hand-loaded culturing bowl. Buckets move water and biomass, ingredients go in one at a time, an empty sneaking
 * hand takes them back, an empty hand starts a batch. Anything else passes through so the player can build against it.
 */
public final class CultureBowlBlock extends BaseEntityBlock {
    /** True while a batch runs; the client animates from it. */
    public static final BooleanProperty WORKING = BooleanProperty.create("working");

    private static final MapCodec<CultureBowlBlock> CODEC = simpleCodec(CultureBowlBlock::new);
    private static final VoxelShape SHAPE = Block.box(1, 0, 1, 15, 6, 15);

    CultureBowlBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(WORKING, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(WORKING);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected MapCodec<CultureBowlBlock> codec() {
        return CODEC;
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        return Organs.hasFoundation(context.getLevel(), context.getClickedPos()) ? defaultBlockState() : null;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CultureBowlBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(
            Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide) return null;
        return createTickerHelper(type, ProcessingModule.CULTURE_BOWL_ENTITY.get(), CultureBowlBlockEntity::serverTick);
    }

    /** A Creative player who breaks a loaded bowl still gets it back, as with a shulker box. */
    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide
                && player.isCreative()
                && level.getBlockEntity(pos) instanceof CultureBowlBlockEntity bowl
                && !bowl.isEmpty()) {
            ItemStack stack = new ItemStack(this);
            bowl.saveToItem(stack, level.registryAccess());
            ItemEntity drop = new ItemEntity(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, stack);
            drop.setDefaultPickUpDelay();
            level.addFreshEntity(drop);
        }
        return super.playerWillDestroy(level, pos, state, player);
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
        if (!(level.getBlockEntity(pos) instanceof CultureBowlBlockEntity bowl))
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (!bowl.accepts(stack)) return Organs.openScreen(stack, level, pos, player);
        if (level.isClientSide) return ItemInteractionResult.SUCCESS;
        ItemStack offered = player.getAbilities().instabuild ? stack.copy() : stack;
        var refusal = bowl.insert(offered);
        if (refusal.isPresent()) {
            Refusals.show(player, refusal.get());
            return ItemInteractionResult.FAIL;
        }
        return ItemInteractionResult.CONSUME;
    }

    @Override
    protected InteractionResult useWithoutItem(
            BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!player.isShiftKeyDown())
            return Organs.openScreen(level, pos, player).result();
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (!(level.getBlockEntity(pos) instanceof CultureBowlBlockEntity bowl)) return InteractionResult.PASS;
        Optional<Refusal> refusal = bowl.cancel(player);
        if (refusal.isPresent()) Refusals.show(player, refusal.get());
        else bowl.collect(player);
        return InteractionResult.CONSUME;
    }
}
