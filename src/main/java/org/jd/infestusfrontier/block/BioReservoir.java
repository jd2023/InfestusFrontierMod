package org.jd.infestusfrontier.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Material;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.network.NetworkHooks;
import net.minecraftforge.registries.DeferredRegister;
import org.checkerframework.checker.signature.qual.Identifier;
import org.jd.infestusfrontier.InfestusFrontier;
import org.jd.infestusfrontier.ZgBlockEntities;
import org.jd.infestusfrontier.block_entity.BioReservoirBlockEntity;

import org.jd.infestusfrontier.screen.BioReservoirScreen;
import org.jetbrains.annotations.Nullable;

public class BioReservoir extends BaseEntityBlock {
    public static final String ID = "bio_reservoir";

    public BioReservoir() {
        super(Properties.of(Material.GLASS)
                .strength(1.5F, 6.0F) // Adjust hardness and resistance as needed
                .noOcclusion());  // Prevent visual occlusion for transparent blocks


    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState state1, boolean b) {
        if (state.getBlock() != state1.getBlock()) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof BioReservoirBlockEntity) {
                ((BioReservoirBlockEntity) blockEntity).drops();
            }
        }
        super.onRemove(state, level, pos, state1, b);
    }

    @Override
    public InteractionResult use(BlockState pState, Level pLevel, BlockPos pPos,
                                 Player pPlayer, InteractionHand pHand, BlockHitResult pHit) {
        if (!pLevel.isClientSide()) {
            BlockEntity entity = pLevel.getBlockEntity(pPos);
            if(entity instanceof BioReservoirBlockEntity) {
                NetworkHooks.openScreen(((ServerPlayer)pPlayer), (BioReservoirBlockEntity)entity, pPos);
            } else {
                throw new IllegalStateException("Our Container provider is missing!");
            }
        }

        return InteractionResult.sidedSuccess(pLevel.isClientSide());
    }

    @Override
    public RenderShape getRenderShape(BlockState p_49232_) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BioReservoirBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, ZgBlockEntities.BIOMASS_RESERVOIR.get(), BioReservoirBlockEntity::tick);
    }
}
