package org.jd.infestusfrontier.processing;

import com.mojang.serialization.MapCodec;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.LivingEntity;
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
 * The Shaping Palm (BLOCK_CATALOG T1-41): a crafting table that keeps its grid. It stands on an organ foundation, and
 * any hand opens its screen, like a chest. A Contractile Fiber used on it gives it Reach, the use of the Mouth under it; a Synaptic
 * Gel used on a Palm with Reach gives it Rote, which repeats one recorded craft by itself. Its block state mirrors
 * whether it has Rote and whether a cycle is under way, for the client; everything else is in its block entity.
 */
public final class ShapingPalmBlock extends BaseEntityBlock {
    /** The Palm has Rote: its screen has a biomass tank, a tray and Select. */
    public static final BooleanProperty ROTE = BooleanProperty.create("rote");
    /** A cycle is under way. */
    public static final BooleanProperty WORKING = BooleanProperty.create("working");

    private static final MapCodec<ShapingPalmBlock> CODEC = simpleCodec(ShapingPalmBlock::new);
    private static final VoxelShape SHAPE = Block.box(1, 0, 1, 15, 10, 15);

    ShapingPalmBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(ROTE, false).setValue(WORKING, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(ROTE, WORKING);
    }

    @Override
    protected MapCodec<ShapingPalmBlock> codec() {
        return CODEC;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        return Organs.hasFoundation(context.getLevel(), context.getClickedPos()) ? defaultBlockState() : null;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ShapingPalmBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(
            Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide) return null;
        return createTickerHelper(type, ProcessingModule.SHAPING_PALM_ENTITY.get(), ShapingPalmBlockEntity::serverTick);
    }

    /** A Creative player who breaks a loaded Palm still gets it back with its grid, as with a shulker box. */
    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide
                && player.isCreative()
                && level.getBlockEntity(pos) instanceof ShapingPalmBlockEntity palm
                && !palm.isEmpty()) {
            ItemStack stack = new ItemStack(this);
            palm.saveToItem(stack, level.registryAccess());
            ItemEntity drop = new ItemEntity(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, stack);
            drop.setDefaultPickUpDelay();
            level.addFreshEntity(drop);
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    public void setPlacedBy(
            Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        Organs.placedBy(level, pos, placer);
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
        if (stack.is(ProcessingModule.PALM_GRAFTS)) return graft(stack, level, pos, player, false);
        if (stack.is(ProcessingModule.PALM_ROTE_GELS)) return graft(stack, level, pos, player, true);
        return Organs.openScreen(stack, level, pos, player);
    }

    /** One Reach or Rote graft on a Palm: spent only when the Palm takes it, kept by a Creative player. */
    private static ItemInteractionResult graft(
            ItemStack stack, Level level, BlockPos pos, Player player, boolean rote) {
        if (!(level.getBlockEntity(pos) instanceof ShapingPalmBlockEntity palm))
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (level.isClientSide) return ItemInteractionResult.SUCCESS;
        Optional<Refusal> why = rote ? palm.grantRote() : palm.grantReach();
        if (why.isPresent()) {
            Refusals.show(player, why.get());
            return ItemInteractionResult.CONSUME;
        }
        if (!player.getAbilities().instabuild) stack.shrink(1);
        player.displayClientMessage(
                Component.translatable("infestusfrontier.status.palm." + (rote ? "rote" : "reach")), true);
        return ItemInteractionResult.CONSUME;
    }

    @Override
    protected InteractionResult useWithoutItem(
            BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        return Organs.openScreen(level, pos, player).result();
    }
}
