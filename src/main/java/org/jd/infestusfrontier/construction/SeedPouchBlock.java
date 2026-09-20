package org.jd.infestusfrontier.construction;

import com.mojang.serialization.MapCodec;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

final class SeedPouchBlock extends BaseEntityBlock {
    private static final VoxelShape SHAPE = box(1, 0, 1, 15, 11, 15);
    private final Supplier<BlockEntityType<SeedPouchEntity>> entityType;

    SeedPouchBlock(BlockBehaviour.Properties properties, Supplier<BlockEntityType<SeedPouchEntity>> entityType) {
        super(properties);
        this.entityType = entityType;
    }

    @Override protected MapCodec<? extends BaseEntityBlock> codec() { return MapCodec.unit(this); }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SeedPouchEntity(pos, state, entityType);
    }
    @Override protected RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }
    @Override protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }
    @Override protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        if (!level.hasChunkAt(pos.below())) return false;
        var substrate = BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath(
                "infestusfrontier", "ecology/living_substrate"));
        return level.getBlockState(pos.below()).is(substrate);
    }

    @Override protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level,
            BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof SeedPouchEntity pouch)) return ItemInteractionResult.FAIL;
        if (stack.isEmpty()) return player.getOffhandItem().isEmpty()
                ? ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION
                : ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION;
        if (stack.is(net.minecraft.world.item.Items.STICK)) {
            if (!level.isClientSide && !refuseRejected(pouch, player)) {
                pouch.adjustReserve(player.isShiftKeyDown());
                player.displayClientMessage(net.minecraft.network.chat.Component.translatable(
                        "message.infestusfrontier.seed_pouch.reserve", pouch.reserve()), true);
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        if (!stack.is(ConstructionTags.SEED_STOCK) || !stack.getComponentsPatch().isEmpty()) {
            return ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION;
        }
        if (level.isClientSide) return ItemInteractionResult.SUCCESS;
        if (refuseRejected(pouch, player)) return ItemInteractionResult.FAIL;
        int inserted = pouch.insert(stack);
        if (inserted == 0) return ItemInteractionResult.FAIL;
        stack.consume(inserted, player);
        return ItemInteractionResult.CONSUME;
    }

    @Override protected net.minecraft.world.InteractionResult useWithoutItem(BlockState state, Level level,
            BlockPos pos, Player player, BlockHitResult hit) {
        if (!player.getOffhandItem().isEmpty()) return net.minecraft.world.InteractionResult.PASS;
        if (!(level.getBlockEntity(pos) instanceof SeedPouchEntity pouch)) return net.minecraft.world.InteractionResult.PASS;
        if (level.isClientSide) return net.minecraft.world.InteractionResult.SUCCESS;
        if (refuseRejected(pouch, player)) return net.minecraft.world.InteractionResult.FAIL;
        ItemStack taken = pouch.take(player.isShiftKeyDown());
        if (taken.isEmpty()) return net.minecraft.world.InteractionResult.PASS;
        if (!player.addItem(taken)) player.drop(taken, false);
        return net.minecraft.world.InteractionResult.CONSUME;
    }

    private static boolean refuseRejected(SeedPouchEntity pouch, Player player) {
        if (!pouch.rejected()) return false;
        player.displayClientMessage(net.minecraft.network.chat.Component.translatable(
                "message.infestusfrontier.seed_pouch.invalid_save"), true);
        return true;
    }

    @Override protected java.util.List<ItemStack> getDrops(BlockState state,
            net.minecraft.world.level.storage.loot.LootParams.Builder params) {
        if (params.getOptionalParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.BLOCK_ENTITY)
                instanceof SeedPouchEntity pouch && pouch.rejected()) {
            var stack = new ItemStack(this);
            stack.set(net.minecraft.core.component.DataComponents.BLOCK_ENTITY_DATA,
                    net.minecraft.world.item.component.CustomData.of(pouch.saveWithId(params.getLevel().registryAccess())));
            return java.util.List.of(stack);
        }
        return super.getDrops(state, params);
    }

    @Override protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState replacement, boolean moved) {
        if (!state.is(replacement.getBlock()) && level.getBlockEntity(pos) instanceof SeedPouchEntity pouch) {
            pouch.dropContents(level, pos);
        }
        super.onRemove(state, level, pos, replacement, moved);
    }
}
