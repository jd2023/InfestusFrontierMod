package org.jd.infestusfrontier.block.entity;

import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jd.infestusfrontier.block.InfestusBlockEntities;
import org.jd.infestusfrontier.block.entity.spread.Infester;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.awt.*;
import java.util.Optional;

public class CorruptionCoreBlockEntity extends BlockEntity implements MenuProvider {
    private static final Logger LOGGER = LogUtils.getLogger();
    private Infester infester;


    public CorruptionCoreBlockEntity(BlockPos pos, BlockState state) {
        super(InfestusBlockEntities.CORRUPTION_CORE_ENTITY.get(), pos, state);
        this.infester = new Infester(2, 8);
        LOGGER.warn("Creating InfestusPodBlockEntity at {}", pos);
    }

    @Override
    public Component getDisplayName() {
        return null;
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int p_39954_, Inventory p_39955_, Player p_39956_) {
        return null;
    }

    public static <T extends BlockEntity> void tick(Level level, BlockPos pos, BlockState state, T be) {
        if (level.isClientSide()) return;
        if (level instanceof ServerLevel serverLevel) {
            if (be instanceof CorruptionCoreBlockEntity entity) {
                entity.infester.infestNext(serverLevel, pos);
            }
        }
    }

    public void remove(BlockState state, ServerLevel level, BlockPos pos) {
        infester.downgradeNetwork(state, level, pos);
    }

    protected void saveAdditional(CompoundTag nbt) {
        nbt.put(Infester.TAG, infester.serializeNBT());
        super.saveAdditional(nbt);
    }

    @Override
    public void load(CompoundTag nbt) {
        super.load(nbt);
        infester.deserializeNBT(nbt.getCompound(Infester.TAG));
    }
}
