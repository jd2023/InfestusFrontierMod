package org.jd.infestusfrontier.processing;

import com.mojang.serialization.MapCodec;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;

final class BioFurnaceBlock extends BaseEntityBlock {
    private final Supplier<BlockEntityType<BioFurnaceEntity>> entityType;

    BioFurnaceBlock(Properties properties, Supplier<BlockEntityType<BioFurnaceEntity>> entityType) {
        super(properties);
        this.entityType = entityType;
    }

    @Override protected MapCodec<? extends BaseEntityBlock> codec() { return MapCodec.unit(this); }
    @Override protected RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return entityType.get().create(pos, state); }
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, entityType.get(),
                (world, pos, current, furnace) -> furnace.tick());
    }

    @Override protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
            Player player, InteractionHand hand, BlockHitResult hit) {
        if (hand == InteractionHand.OFF_HAND || stack.isEmpty()) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (level.isClientSide) return (stack.is(BioFurnaceResources.item("infestusfrontier:storage/biomass_bucket"))
                || stack.is(Items.CLOCK) || stack.is(Items.GLASS_BOTTLE))
                ? ItemInteractionResult.SUCCESS : ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (level.getBlockEntity(pos) instanceof BioFurnaceEntity furnace && furnace.interact(player, hand)) {
            return ItemInteractionResult.CONSUME;
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
            Player player, BlockHitResult hit) {
        if (!player.getMainHandItem().isEmpty() || !player.getOffhandItem().isEmpty()) return InteractionResult.PASS;
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof BioFurnaceEntity furnace) {
            furnace.interact(player, InteractionHand.MAIN_HAND);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        var stack = new ItemStack(this);
        if (params.getOptionalParameter(LootContextParams.BLOCK_ENTITY) instanceof BioFurnaceEntity furnace) {
            stack.set(DataComponents.BLOCK_ENTITY_DATA, CustomData.of(furnace.saveWithId(params.getLevel().registryAccess())));
        }
        return List.of(stack);
    }
}
