package org.jd.infestusfrontier.logistics;

import com.mojang.serialization.MapCodec;
import java.util.EnumMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jd.infestusfrontier.organ.api.Organs;
import org.jd.infestusfrontier.storage.api.BiomassFluids;
import org.jetbrains.annotations.Nullable;

/**
 * The pump of the colony. Each face is a {@link Port} in the block state, so every client sees which mouths are
 * open; the probe cycles a face. Buckets move fluid, anything else opens the screen, sneaking builds against it.
 */
public final class VascularJunctionBlock extends BaseEntityBlock {
    /** The port on each face: {@code up}, {@code down}, {@code north}, {@code south}, {@code east}, {@code west}. */
    public static final Map<Direction, EnumProperty<Port>> PORT = ports();

    private static final MapCodec<VascularJunctionBlock> CODEC = simpleCodec(VascularJunctionBlock::new);
    private static final VoxelShape SHAPE = Block.box(2, 0, 2, 14, 14, 14);

    private static Map<Direction, EnumProperty<Port>> ports() {
        Map<Direction, EnumProperty<Port>> ports = new EnumMap<>(Direction.class);
        for (Direction side : Direction.values())
            ports.put(side, EnumProperty.create(side.getSerializedName(), Port.class));
        return Map.copyOf(ports);
    }

    VascularJunctionBlock(Properties properties) {
        super(properties);
        BlockState state = stateDefinition.any();
        for (EnumProperty<Port> port : PORT.values()) state = state.setValue(port, Port.CLOSED);
        registerDefaultState(state);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        for (Direction side : Direction.values()) builder.add(PORT.get(side));
    }

    @Override
    protected MapCodec<VascularJunctionBlock> codec() {
        return CODEC;
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
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        return Organs.hasFoundation(context.getLevel(), context.getClickedPos()) ? defaultBlockState() : null;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new VascularJunctionBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(
            Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide) return null;
        return createTickerHelper(
                type, LogisticsModule.VASCULAR_JUNCTION_ENTITY.get(), VascularJunctionBlockEntity::serverTick);
    }

    /** A Creative player who breaks a loaded junction still gets it back, as with a shulker box. */
    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide
                && player.isCreative()
                && level.getBlockEntity(pos) instanceof VascularJunctionBlockEntity junction
                && !junction.isEmpty()) {
            ItemStack stack = new ItemStack(this);
            junction.saveToItem(stack, level.registryAccess());
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
        ItemInteractionResult fluid = BiomassFluids.useOn(stack, level, pos, null, player, hand);
        if (fluid != ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION) return fluid;
        return Organs.openScreen(stack, level, pos, player);
    }

    @Override
    protected InteractionResult useWithoutItem(
            BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        return Organs.openScreen(level, pos, player).result();
    }
}
