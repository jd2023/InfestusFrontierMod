package org.jd.infestusfrontier.processing.digestion;

import com.mojang.serialization.MapCodec;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
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
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

final class BiomassBladderBlock extends BaseEntityBlock {
    static final IntegerProperty FILL = IntegerProperty.create("fill", 0, 4);
    private static final VoxelShape SHAPE = Block.box(1, 0, 1, 15, 15, 15);
    private final Supplier<BlockEntityType<BiomassBladderEntity>> entityType;
    private final Supplier<Item> biomassBucket;

    BiomassBladderBlock(Properties properties, Supplier<BlockEntityType<BiomassBladderEntity>> entityType,
            Supplier<Item> biomassBucket) {
        super(properties);
        this.entityType = entityType;
        this.biomassBucket = biomassBucket;
        registerDefaultState(stateDefinition.any().setValue(FILL, 0));
    }

    @Override protected MapCodec<? extends BaseEntityBlock> codec() { return MapCodec.unit(this); }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) { builder.add(FILL); }
    @Override protected RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }
    @Override protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) { return SHAPE; }
    @Override protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return DigestionModule.isRooted(level, pos);
    }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return entityType.get().create(pos, state);
    }
    @Override protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
            Player player, InteractionHand hand, BlockHitResult hit) {
        if (stack.isEmpty()) return player.getOffhandItem().isEmpty()
                ? ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION
                : ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION;
        if (!BiomassInteractions.isBucket(stack, biomassBucket.get())) return ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION;
        if (level.isClientSide) return ItemInteractionResult.SUCCESS;
        if (level.getBlockEntity(pos) instanceof BiomassBladderEntity bladder
                && bladder.interact(player, hand, biomassBucket.get())) return ItemInteractionResult.CONSUME;
        return ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION;
    }
    @Override protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
            Player player, BlockHitResult hit) {
        if (!player.getOffhandItem().isEmpty()) return InteractionResult.PASS;
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof BiomassBladderEntity bladder) {
            var snapshot = bladder.snapshot();
            BiomassInteractions.status(player, snapshot.fluidAmount("biomass"), BiomassBladderEntity.CAPACITY);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
    @Override protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        var stack = new ItemStack(this);
        if (params.getOptionalParameter(LootContextParams.BLOCK_ENTITY) instanceof BiomassBladderEntity bladder) {
            stack.set(DataComponents.BLOCK_ENTITY_DATA, CustomData.of(bladder.saveWithId(params.getLevel().registryAccess())));
        }
        return List.of(stack);
    }
}
