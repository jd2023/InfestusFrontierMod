package org.jd.infestusfrontier.genetics;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.LivingEntity;
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
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jd.infestusfrontier.genetics.api.DnaSampleRecipe;
import org.jd.infestusfrontier.organ.api.Organs;
import org.jd.infestusfrontier.storage.api.BiomassFluids;
import org.jetbrains.annotations.Nullable;

/**
 * A DNA Bank: an organ on a foundation that extracts DNA samples and keeps the genome research they add. A DNA sample
 * loads it, a bucket moves biomass, any other hand opens its screen and sneaking builds against it. Broken, it comes
 * out whole with its samples, its biomass, its research and the extraction under way.
 */
public final class DnaBankBlock extends BaseEntityBlock {
    private static final MapCodec<DnaBankBlock> CODEC = simpleCodec(DnaBankBlock::new);

    /** The model's boxes as {x1, y1, z1, x2, y2, z2}: a bone foot, the fleshy body and the cluster of vials on top. */
    static final double[][] BOXES = {{1, 0, 1, 15, 2, 15}, {2, 2, 2, 14, 10, 14}, {4, 10, 4, 12, 15, 12}};

    private static final VoxelShape SHAPE = shape();

    DnaBankBlock(Properties properties) {
        super(properties);
    }

    private static VoxelShape shape() {
        VoxelShape shape = Shapes.empty();
        for (double[] box : BOXES) shape = Shapes.or(shape, Block.box(box[0], box[1], box[2], box[3], box[4], box[5]));
        return shape.optimize();
    }

    @Override
    protected MapCodec<DnaBankBlock> codec() {
        return CODEC;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    /** Placed by hand on a foundation. */
    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        return Organs.hasFoundation(context.getLevel(), context.getClickedPos()) ? defaultBlockState() : null;
    }

    /** The placer becomes the steward. */
    @Override
    public void setPlacedBy(
            Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        Organs.placedBy(level, pos, placer);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new DnaBankBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(
            Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide) return null;
        return createTickerHelper(type, GeneticsModule.DNA_BANK_ENTITY.get(), DnaBankBlockEntity::serverTick);
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
        if (level.getBlockEntity(pos) instanceof DnaBankBlockEntity bank
                && DnaSampleRecipe.forSample(level, stack).isPresent()) {
            if (!level.isClientSide) bank.load(player, hand);
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        ItemInteractionResult fluid = BiomassFluids.useOn(stack, level, pos, hit.getDirection(), player, hand);
        if (fluid != ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION) return fluid;
        return Organs.openScreen(stack, level, pos, player);
    }

    @Override
    protected InteractionResult useWithoutItem(
            BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        return Organs.openScreen(level, pos, player).result();
    }
}
