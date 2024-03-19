package org.jd.infestusfrontier.block;

import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
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
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
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
import org.slf4j.Logger;

public class BioReservoir extends HorizontalDirectionalBlock {
    public static final String ID = "bio_reservoir";
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    private static final Logger LOGGER = LogUtils.getLogger();

    public BioReservoir() {
        super(Properties.of(Material.GLASS)
                .strength(1.5F, 6.0F) // Adjust hardness and resistance as needed
                .noOcclusion());  // Prevent visual occlusion for transparent blocks


    }
    private static final VoxelShape SHAPE =
            Block.box(2, 2, 2, 14, 16, 14);

    @Override
    public VoxelShape getShape(BlockState p_60555_, BlockGetter p_60556_, BlockPos p_60557_, CollisionContext p_60558_) {
        return SHAPE;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        LOGGER.info("Getting state for placement BioReservoir at {} by {}", context.getClickedPos(), context.getPlayer());
        if (!InfestUtils.isInfestusNetwork(context.getClickedPos().below(), context.getLevel())) {
            return null;
        }
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public BlockState rotate(BlockState pState, Rotation pRotation) {
        return pState.setValue(FACING, pRotation.rotate(pState.getValue(FACING)));
    }

    @Override
    public BlockState mirror(BlockState pState, Mirror pMirror) {
        return pState.rotate(pMirror.getRotation(pState.getValue(FACING)));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
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

    public InteractionResult use(BlockState state, Level level, BlockPos pos,
                                 Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide() && level.getBlockEntity(pos) instanceof BioReservoirBlockEntity entity) {
            LOGGER.info("Network {} has {} biomass capacity. Used: {}.",
                    entity.getNetworkId(),
                    NetworkManager.getNetwork(entity.getNetworkId()).getBiomassStorage().getTotalCapacity(),
                    NetworkManager.getNetwork(entity.getNetworkId()).getBiomassStorage().getUsedCapacity());
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }
}
