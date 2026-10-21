package org.jd.infestusfrontier.equipment;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jd.infestusfrontier.storage.api.BiomassFluids;
import org.jetbrains.annotations.Nullable;

/**
 * A refuelling cell, flush with the ground: a substrate cell with a pale nipple and a green pore. Veins and buckets
 * fill its tank; whoever stands on it has their worn awakened pieces filled from it. Buckets act on it; anything
 * else builds against it, it has no screen.
 */
public final class FuelPapillaBlock extends BaseEntityBlock {
    private static final MapCodec<FuelPapillaBlock> CODEC = simpleCodec(FuelPapillaBlock::new);

    FuelPapillaBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<FuelPapillaBlock> codec() {
        return CODEC;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new FuelPapillaBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(
            Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide) return null;
        return createTickerHelper(type, EquipmentModule.FUEL_PAPILLA_ENTITY.get(), FuelPapillaBlockEntity::serverTick);
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
        return BiomassFluids.useOn(stack, level, pos, hit.getDirection(), player, hand);
    }
}
