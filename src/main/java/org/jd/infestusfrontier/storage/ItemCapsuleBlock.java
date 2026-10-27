package org.jd.infestusfrontier.storage;

import com.mojang.serialization.MapCodec;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.LevelEvent;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jd.infestusfrontier.ecology.EcologyModule;
import org.jd.infestusfrontier.organ.api.Organs;
import org.jd.infestusfrontier.storage.api.CapsuleStock;
import org.jetbrains.annotations.Nullable;

/**
 * An Item Capsule (BLOCK_CATALOG T1-09): the pod of a Capsule Stem, one kind of item in bulk, its outer face toward
 * {@link #FACING} showing what it holds. A stem grows pods on its trunk cells; a pod pressed by hand on a trunk cell
 * joins that tree, anywhere else it stands alone. A hand with items puts them in, a sneaking empty hand takes a stack
 * out, any other hand opens the tree's screen. A Skeletal Graft grows the pod a grade, doubling what it holds.
 */
public final class ItemCapsuleBlock extends BaseEntityBlock {
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    public static final IntegerProperty GRADE = IntegerProperty.create("grade", 0, CapsuleStock.LAST_GRADE);

    private static final MapCodec<ItemCapsuleBlock> CODEC = simpleCodec(ItemCapsuleBlock::new);
    /**
     * The pod's body at each grade with its neck to the stem, by grade then {@link Direction#get2DDataValue}: the
     * model's hull, crown and keel scaled about the hull's back (art/gen_item_capsule.py GRADES, BACK).
     */
    private static final VoxelShape[][] SHAPES = {
        facings(3.5, 3, 5.5, 12.5, 13, 16), facings(3, 2, 4, 13, 14, 16), facings(2, 1, 2.5, 14, 15, 16)
    };

    private static final int GRAFT_PARTICLES = 8;

    /** A box given for a pod facing north, turned for each horizontal facing, by {@link Direction#get2DDataValue}. */
    private static VoxelShape[] facings(double x0, double y0, double z0, double x1, double y1, double z1) {
        VoxelShape[] shapes = new VoxelShape[4];
        shapes[Direction.NORTH.get2DDataValue()] = Block.box(x0, y0, z0, x1, y1, z1);
        shapes[Direction.SOUTH.get2DDataValue()] = Block.box(16 - x1, y0, 16 - z1, 16 - x0, y1, 16 - z0);
        shapes[Direction.EAST.get2DDataValue()] = Block.box(16 - z1, y0, x0, 16 - z0, y1, x1);
        shapes[Direction.WEST.get2DDataValue()] = Block.box(z0, y0, 16 - x1, z1, y1, 16 - x0);
        return shapes;
    }

    ItemCapsuleBlock(Properties properties) {
        super(properties);
        registerDefaultState(
                stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(GRADE, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, GRADE);
    }

    @Override
    protected MapCodec<ItemCapsuleBlock> codec() {
        return CODEC;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES[state.getValue(GRADE)][state.getValue(FACING).get2DDataValue()];
    }

    /** True when {@code pos} is a trunk cell of a stem a pod may hang on, through its face {@code face}. */
    private static boolean hangs(Level level, BlockPos pos, Direction face) {
        if (face.getAxis().isVertical() || !level.hasChunkAt(pos)) return false;
        BlockState state = level.getBlockState(pos);
        return state.is(StorageModule.CAPSULE_STEM.get()) && state.getValue(CapsuleStemBlock.PART) != StemPart.ROOT;
    }

    /** A pod pressed on a trunk cell faces away from it; anywhere else it faces its placer. */
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction face = context.getClickedFace();
        BlockPos cell = context.getClickedPos().relative(face.getOpposite());
        if (hangs(context.getLevel(), cell, face)) return defaultBlockState().setValue(FACING, face);
        return defaultBlockState()
                .setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    /** A pod placed against a trunk cell of a standing tree joins its plan. */
    @Override
    public void setPlacedBy(
            Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (level.isClientSide || !(level.getBlockEntity(pos) instanceof ItemCapsuleBlockEntity pod)) return;
        Direction facing = state.getValue(FACING);
        BlockPos cellPos = pos.relative(facing.getOpposite());
        if (!hangs(level, cellPos, facing)) return;
        Stems.cellAt(level, cellPos).flatMap(CapsuleStemBlockEntity::root).ifPresent(root -> {
            if (root.adopt(level, cellPos, facing)) pod.hangOn(Optional.of(root.getBlockPos()));
        });
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
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ItemCapsuleBlockEntity(pos, state);
    }

    /** A pod that leaves takes its own compartment; its tree reads its store again without it. */
    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock())
                && !level.isClientSide
                && level.getBlockEntity(pos) instanceof ItemCapsuleBlockEntity pod)
            pod.root().flatMap(rootPos -> Stems.rootAt(level, rootPos)).ifPresent(CapsuleStemBlockEntity::touch);
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    /** A Creative player who breaks a pod holding items or grown still gets them back, as with a shulker box. */
    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide
                && player.isCreative()
                && level.getBlockEntity(pos) instanceof ItemCapsuleBlockEntity pod
                && (!pod.isEmpty() || pod.grade() > 0)) {
            ItemStack stack = new ItemStack(this);
            stack.applyComponents(pod.collectComponents());
            ItemEntity drop = new ItemEntity(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, stack);
            drop.setDefaultPickUpDelay();
            level.addFreshEntity(drop);
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    /**
     * A Skeletal Graft grows the pod a grade. Items the pod takes go in, as many as fit; a hand with items the pod
     * refuses opens the screen, as any other item does.
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
        if (stack.isEmpty()
                || stack.is(Organs.READERS)
                || !(level.getBlockEntity(pos) instanceof ItemCapsuleBlockEntity pod))
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (stack.is(EcologyModule.SKELETAL_GRAFT.get())) {
            if (pod.grade() >= CapsuleStock.LAST_GRADE || !player.mayBuild())
                return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
            if (level.isClientSide) return ItemInteractionResult.SUCCESS;
            pod.grow();
            level.levelEvent(LevelEvent.PARTICLES_AND_SOUND_PLANT_GROWTH, pos, GRAFT_PARTICLES);
            if (!player.getAbilities().instabuild) stack.shrink(1);
            return ItemInteractionResult.CONSUME;
        }
        Optional<CapsuleTree> tree = Stems.of(pod);
        if (tree.isEmpty()) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        int slot = tree.get().slotOf(pod);
        ItemStack rest = tree.get().insert(slot, stack, level.isClientSide);
        if (rest.getCount() == stack.getCount()) return Organs.openScreen(stack, level, pos, player);
        if (!level.isClientSide) player.setItemInHand(hand, rest);
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    /** A sneaking empty hand takes a stack out of this pod; any other empty hand opens the tree's screen. */
    @Override
    protected InteractionResult useWithoutItem(
            BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!player.isSecondaryUseActive() || !(level.getBlockEntity(pos) instanceof ItemCapsuleBlockEntity pod))
            return Organs.openScreen(level, pos, player).result();
        Optional<CapsuleTree> tree = Stems.of(pod);
        if (tree.isEmpty()) return InteractionResult.PASS;
        int slot = tree.get().slotOf(pod);
        ItemStack taken = tree.get().extract(slot, Integer.MAX_VALUE, level.isClientSide);
        if (taken.isEmpty()) return InteractionResult.PASS;
        if (!level.isClientSide) player.getInventory().placeItemBackInInventory(taken);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
