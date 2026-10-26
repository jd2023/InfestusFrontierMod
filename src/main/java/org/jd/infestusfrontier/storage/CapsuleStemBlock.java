package org.jd.infestusfrontier.storage;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jd.infestusfrontier.organ.api.Organs;
import org.jetbrains.annotations.Nullable;

/**
 * One cell of a Capsule Stem (BLOCK_CATALOG T1-09): the planted root, a trunk cell, a bud or a branch. Only the root
 * is placed; the rest grow from bone meal on any cell, pods with them. Any cell opens the tree's screen. A cell is
 * tissue: broken, it leaves Dead Tissue and is regrown from the root's plan; a root broken kills its tree, cell by
 * cell from the bottom, and its pods stand on alone with their contents.
 */
public final class CapsuleStemBlock extends BaseEntityBlock implements BonemealableBlock {
    public static final EnumProperty<StemPart> PART = EnumProperty.create("part", StemPart.class);
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    /** A bone collar round the cell: it carries pods. */
    public static final BooleanProperty COLLAR = BooleanProperty.create("collar");

    private static final MapCodec<CapsuleStemBlock> CODEC = simpleCodec(CapsuleStemBlock::new);
    private static final VoxelShape COLUMN = Block.box(3, 0, 3, 13, 16, 13);
    private static final VoxelShape ROOT = Shapes.or(Block.box(0, 0, 0, 16, 4, 16), COLUMN);
    private static final VoxelShape BRANCH = Block.box(2, 2, 2, 14, 16, 14);

    CapsuleStemBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition
                .any()
                .setValue(PART, StemPart.ROOT)
                .setValue(FACING, Direction.NORTH)
                .setValue(COLLAR, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(PART, FACING, COLLAR);
    }

    @Override
    protected MapCodec<CapsuleStemBlock> codec() {
        return CODEC;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return switch (state.getValue(PART)) {
            case ROOT -> ROOT;
            case BRANCH -> BRANCH;
            case TRUNK, BUD -> COLUMN;
        };
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
        return new CapsuleStemBlockEntity(pos, state);
    }

    /** Plants a root at {@code pos} with a plan drawn from {@code seed}, as a placed root is from the level's. */
    public static void plant(ServerLevel level, BlockPos pos, long seed) {
        level.setBlock(pos, StorageModule.CAPSULE_STEM.get().defaultBlockState(), Block.UPDATE_ALL);
        if (level.getBlockEntity(pos) instanceof CapsuleStemBlockEntity cell) cell.plant(seed);
    }

    /** A root placed by hand plants a plan of its own. */
    @Override
    public void setPlacedBy(
            Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof CapsuleStemBlockEntity cell && !cell.isRoot())
            cell.plant(level.random.nextLong());
    }

    /** A root leaving kills its tree; any other cell leaving tells the root the tree changed. */
    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock())
                && !level.isClientSide
                && level.getBlockEntity(pos) instanceof CapsuleStemBlockEntity cell) {
            if (cell.isRoot()) cell.dies((ServerLevel) level);
            else cell.root().ifPresent(CapsuleStemBlockEntity::touch);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    /**
     * A root pays growth it owes; a cell whose root is gone, or replaced by a root whose plan does not hold it, dies,
     * leaving Dead Tissue.
     */
    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!(level.getBlockEntity(pos) instanceof CapsuleStemBlockEntity cell)) return;
        if (cell.isRoot()) cell.payPending(level);
        else if (cell.root().filter(root -> root.owns(pos)).isEmpty()) level.destroyBlock(pos, true);
    }

    /** The client has no plan, so it swings at every cell; the server decides. */
    @Override
    public boolean isValidBonemealTarget(LevelReader level, BlockPos pos, BlockState state) {
        if (level.isClientSide()) return true;
        return Stems.cellAt(level, pos)
                .flatMap(CapsuleStemBlockEntity::root)
                .flatMap(root -> root.nextGrowth(level))
                .isPresent();
    }

    @Override
    public boolean isBonemealSuccess(Level level, RandomSource random, BlockPos pos, BlockState state) {
        return true;
    }

    @Override
    public void performBonemeal(ServerLevel level, RandomSource random, BlockPos pos, BlockState state) {
        Stems.cellAt(level, pos).flatMap(CapsuleStemBlockEntity::root).ifPresent(root -> root.grow(level));
    }

    /** Bone meal goes to its own use, which grows the tree; any other item opens the tree's screen. */
    @Override
    protected ItemInteractionResult useItemOn(
            ItemStack stack,
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            InteractionHand hand,
            BlockHitResult hit) {
        if (stack.is(Items.BONE_MEAL)) return ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION;
        return Organs.openScreen(stack, level, pos, player);
    }

    @Override
    protected InteractionResult useWithoutItem(
            BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        return Organs.openScreen(level, pos, player).result();
    }
}
