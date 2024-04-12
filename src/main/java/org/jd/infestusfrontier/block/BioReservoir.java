package org.jd.infestusfrontier.block;

import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.Material;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.network.NetworkHooks;
import org.jd.infestusfrontier.ZgBlocks;
import org.jd.infestusfrontier.block.entity.BioReservoirBlockEntity;
import org.jd.infestusfrontier.block.entity.CorruptionCoreBlockEntity;
import org.jd.infestusfrontier.network.NetworkManager;
import org.jd.infestusfrontier.screen.CorruptionCoreMenu;
import org.jd.infestusfrontier.utils.InfestUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

public class BioReservoir extends BaseEntityBlock {
    public static final String ID = "bio_reservoir";
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final IntegerProperty BIOMASS = IntegerProperty.create("biomass", 0, 10);




    public BioReservoir() {
        super(Properties.of(Material.GLASS)
                .strength(1.5F, 6.0F) // Adjust hardness and resistance as needed
                .noOcclusion());  // Prevent visual occlusion for transparent blocks
        this.registerDefaultState(this.stateDefinition.any().setValue(this.getBiomassProperty(), Integer.valueOf(0)));

    }
    private static final VoxelShape SHAPE =
            Block.box(2, 2, 2, 14, 16, 14);

    @Override
    public VoxelShape getShape(BlockState p_60555_, BlockGetter p_60556_, BlockPos p_60557_, CollisionContext p_60558_) {
        return SHAPE;
    }





    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(BIOMASS);
    }

    @Override
    public void setPlacedBy(Level world, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        var placerStringUUID = placer.getStringUUID();
        if (world.getBlockEntity(pos) instanceof BioReservoirBlockEntity blockEntity) {
            blockEntity.setNetworkId(placerStringUUID);
        }
        super.setPlacedBy(world, pos, state, placer, stack);

        LOGGER.info("Placed BioReservoir at {} by {}", pos, placerStringUUID);
        if (NetworkManager.getNetwork(placerStringUUID) == null) {
            LOGGER.error("Network {} doesn't exist", placerStringUUID);
        } else {
            NetworkManager.getNetwork(placerStringUUID).getBiomassStorage().addTotalCapacity(1000);
            LOGGER.info("Network {} has {} biomass capacity", placerStringUUID, NetworkManager.getNetwork(placerStringUUID).getBiomassStorage().getTotalCapacity());
        }
    }

//    public InteractionResult use(BlockState state, Level level, BlockPos pos,
//                                 Player player, InteractionHand hand, BlockHitResult hit) {
//        if (level.isClientSide() && level.getBlockEntity(pos) instanceof BioReservoirBlockEntity entity) {
//            LOGGER.info("Network {} has {} biomass capacity. Used: {}.",
//                    entity.getNetworkId(),
//                    NetworkManager.getNetwork(entity.getNetworkId()).getBiomassStorage().getTotalCapacity(),
//                    NetworkManager.getNetwork(entity.getNetworkId()).getBiomassStorage().getUsedCapacity());
//        }
//        return InteractionResult.sidedSuccess(level.isClientSide());
//    }
    public IntegerProperty getBiomassProperty() {
        return BIOMASS;
    }
    public int getMaxBiomass() {
        return 10;
    }


    protected int getBiomass(BlockState pState) {
        return pState.getValue(this.getBiomassProperty());
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pPos, BlockState pState) {
        return new BioReservoirBlockEntity(pPos, pState);
    }
    public BlockState getStateForBiomass(int biomass) {
        return this.defaultBlockState().setValue(this.getBiomassProperty(), Integer.valueOf(biomass));
    }
    public void randomTick(BlockState pState, ServerLevel pLevel, BlockPos pPos, RandomSource pRandom) {
        if (!pLevel.isAreaLoaded(pPos, 1)) return; // Forge: prevent loading unloaded chunks when checking neighbor's light

        int i = this.getBiomass(pState);
        if (i <= this.getMaxBiomass()) {

            pLevel.setBlock(pPos, this.getStateForBiomass(i + 1), 2);

        }


    }
}
