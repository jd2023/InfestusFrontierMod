package org.jd.infestusfrontier.processing;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
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
 * Turns one hand-fed item at a time into biomass. Feed items start a batch, empty buckets take biomass out, anything
 * else opens the screen. Sneaking with an item builds against it.
 */
public final class DigestiveSacBlock extends BaseEntityBlock {
    /** True while a batch exists. Written by the block entity only; the client animates from it. */
    public static final BooleanProperty WORKING = BooleanProperty.create("working");

    private static final MapCodec<DigestiveSacBlock> CODEC = simpleCodec(DigestiveSacBlock::new);
    private static final VoxelShape SHAPE = Block.box(1, 0, 1, 15, 15, 15);

    DigestiveSacBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(WORKING, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(WORKING);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected MapCodec<DigestiveSacBlock> codec() {
        return CODEC;
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        return Organs.hasFoundation(context.getLevel(), context.getClickedPos()) ? defaultBlockState() : null;
    }

    /**
     * While it digests, bubbles pop in its mouth now and then and it gurgles, from the client's block ticks as
     * vanilla's furnace shows its fire. The mouth is the model's, 15 px up and 1.5 px off the middle (art/gen_digestive_sac.py).
     */
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!state.getValue(WORKING)) return;
        double x = pos.getX() + 0.5 + 1.5 / 16.0 + (random.nextDouble() - 0.5) * 0.15;
        double y = pos.getY() + 15.2 / 16.0;
        double z = pos.getZ() + 0.5 + (random.nextDouble() - 0.5) * 0.15;
        if (random.nextDouble() < 0.4) level.addParticle(ParticleTypes.BUBBLE_POP, x, y, z, 0.0, 0.02, 0.0);
        if (random.nextDouble() < 0.06)
            level.playLocalSound(
                    x,
                    y,
                    z,
                    SoundEvents.BUBBLE_COLUMN_UPWARDS_AMBIENT,
                    SoundSource.BLOCKS,
                    0.5F,
                    0.6F + random.nextFloat() * 0.2F,
                    false);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new DigestiveSacBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(
            Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide) return null;
        return createTickerHelper(
                type, ProcessingModule.DIGESTIVE_SAC_ENTITY.get(), DigestiveSacBlockEntity::serverTick);
    }

    /** Drawn by the block entity's GeckoLib renderer; the blockstate file only names the break particle. */
    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
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
        if (!(level.getBlockEntity(pos) instanceof DigestiveSacBlockEntity sac))
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        ItemInteractionResult fed = sac.feed(player, hand, stack);
        if (fed != ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION) return fed;
        return Organs.openScreen(stack, level, pos, player);
    }

    @Override
    protected InteractionResult useWithoutItem(
            BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        return Organs.openScreen(level, pos, player).result();
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof DigestiveSacBlockEntity sac)
            sac.dropFeed(level, pos);
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}
