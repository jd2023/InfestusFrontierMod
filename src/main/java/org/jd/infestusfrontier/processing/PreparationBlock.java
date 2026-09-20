package org.jd.infestusfrontier.processing;

import com.mojang.serialization.MapCodec;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
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

final class PreparationBlock extends BaseEntityBlock {
    static final BooleanProperty ACTIVE = BooleanProperty.create("active");
    private static final VoxelShape RACK_SHAPE = Block.box(1, 0, 3, 15, 14, 13);
    private static final VoxelShape LOOM_SHAPE = Block.box(2, 0, 2, 14, 13, 14);
    private final Supplier<BlockEntityType<PreparationBlockEntity>> entityType;
    private final PreparationOrgan organ;

    PreparationBlock(Properties properties, Supplier<BlockEntityType<PreparationBlockEntity>> entityType,
            PreparationOrgan organ) {
        super(properties);
        this.entityType = entityType;
        this.organ = organ;
        registerDefaultState(stateDefinition.any().setValue(ACTIVE, false));
    }

    @Override protected MapCodec<? extends BaseEntityBlock> codec() { return MapCodec.unit(this); }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) { builder.add(ACTIVE); }
    @Override protected RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }
    @Override protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return organ == PreparationOrgan.MEMBRANE_RACK ? RACK_SHAPE : LOOM_SHAPE;
    }
    @Override protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return level.hasChunkAt(pos.below()) && level.getBlockState(pos.below()).is(BuiltInRegistries.BLOCK.get(
                ResourceLocation.parse("infestusfrontier:ecology/living_substrate")));
    }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return entityType.get().create(pos, state); }
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, entityType.get(),
                (world, pos, current, preparation) -> preparation.tick());
    }
    @Override protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
            Player player, InteractionHand hand, BlockHitResult hit) {
        if (stack.isEmpty()) return player.getOffhandItem().isEmpty()
                ? ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION
                : ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION;
        if (!directControl(stack)) return ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION;
        if (level.isClientSide) return ItemInteractionResult.SUCCESS;
        if (level.getBlockEntity(pos) instanceof PreparationBlockEntity preparation
                && preparation.interact(player, hand)) return ItemInteractionResult.CONSUME;
        return ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION;
    }
    private boolean directControl(ItemStack stack) {
        String key = PreparationResources.key(stack.getItem());
        return !key.isEmpty() && (key.equals("biomass_bucket")
                || organ.recipePriority.stream()
                        .map(org.jd.infestusfrontier.processing.api.PreparationRecipes::recipe)
                        .flatMap(recipe -> recipe.routes().stream())
                        .anyMatch(route -> route.itemInputs().containsKey(key)))
                || stack.is(net.minecraft.world.item.Items.WATER_BUCKET);
    }
    @Override protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
            Player player, BlockHitResult hit) {
        if (!player.getOffhandItem().isEmpty()) return InteractionResult.PASS;
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof PreparationBlockEntity preparation) {
            preparation.interact(player, InteractionHand.MAIN_HAND);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
    @Override protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        var stack = new ItemStack(this);
        if (params.getOptionalParameter(LootContextParams.BLOCK_ENTITY) instanceof PreparationBlockEntity preparation) {
            stack.set(DataComponents.BLOCK_ENTITY_DATA,
                    CustomData.of(preparation.saveWithId(params.getLevel().registryAccess())));
        }
        return List.of(stack);
    }
}
