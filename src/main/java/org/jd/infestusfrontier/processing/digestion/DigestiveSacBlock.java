package org.jd.infestusfrontier.processing.digestion;

import com.mojang.serialization.MapCodec;
import java.util.List;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

final class DigestiveSacBlock extends BaseEntityBlock {
    static final BooleanProperty ACTIVE = BooleanProperty.create("active");
    private static final VoxelShape SHAPE = Block.box(2, 0, 2, 14, 13, 14);
    private final Supplier<BlockEntityType<DigestiveSacEntity>> entityType;
    private final Supplier<Item> biomassBucket;

    DigestiveSacBlock(Properties properties, Supplier<BlockEntityType<DigestiveSacEntity>> entityType,
            Supplier<Item> biomassBucket) {
        super(properties);
        this.entityType = entityType;
        this.biomassBucket = biomassBucket;
        registerDefaultState(stateDefinition.any().setValue(ACTIVE, false));
    }

    @Override protected MapCodec<? extends BaseEntityBlock> codec() { return MapCodec.unit(this); }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) { builder.add(ACTIVE); }
    @Override protected RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }
    @Override protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) { return SHAPE; }
    @Override protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return DigestionModule.isRooted(level, pos);
    }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new DigestiveSacEntity(pos, state, entityType);
    }
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, entityType.get(),
                (world, pos, current, sac) -> sac.tick());
    }
    @Override protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
            Player player, InteractionHand hand, BlockHitResult hit) {
        if (stack.isEmpty()) return player.getOffhandItem().isEmpty()
                ? ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION
                : ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION;
        if (!BiomassInteractions.isBucket(stack, biomassBucket.get()) && BiomassInteractions.feed(stack).isEmpty()) {
            return ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION;
        }
        if (level.isClientSide) return ItemInteractionResult.SUCCESS;
        if (level.getBlockEntity(pos) instanceof DigestiveSacEntity sac && sac.interact(player, hand, biomassBucket.get())) {
            return ItemInteractionResult.CONSUME;
        }
        return ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION;
    }
    @Override protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
            Player player, BlockHitResult hit) {
        if (!player.getOffhandItem().isEmpty()) return InteractionResult.PASS;
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof DigestiveSacEntity sac) sac.status(player);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
    @Override protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        var stack = new ItemStack(this);
        if (params.getOptionalParameter(LootContextParams.BLOCK_ENTITY) instanceof DigestiveSacEntity sac) {
            stack.set(DataComponents.BLOCK_ENTITY_DATA, CustomData.of(sac.saveWithId(params.getLevel().registryAccess())));
        }
        return List.of(stack);
    }
}
