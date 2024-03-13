package org.jd.infestusfrontier.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Containers;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import org.jd.infestusfrontier.ZgBlockEntities;
import org.jd.infestusfrontier.ZgItems;
import org.jd.infestusfrontier.screen.sync.CorruptionCoreData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class BioReservoirBlockEntity extends BlockEntity {

    public BioReservoirBlockEntity(BlockPos pos, BlockState state) {
        super(ZgBlockEntities.BIOMASS_RESERVOIR.get(), pos, state);


    }


}
