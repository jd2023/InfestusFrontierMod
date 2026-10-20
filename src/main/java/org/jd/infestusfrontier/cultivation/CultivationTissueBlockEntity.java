package org.jd.infestusfrontier.cultivation;

import com.mojang.serialization.MapCodec;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.SimpleFluidContent;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import org.jd.infestusfrontier.foundation.Persistence;
import org.jd.infestusfrontier.logistics.LogisticsModule;
import org.jd.infestusfrontier.organ.OrganModule;

/** The bed's water: {@value #CAPACITY_MB} mB, water only, drunk one sip at a time. */
final class CultivationTissueBlockEntity extends BlockEntity {
    static final int CAPACITY_MB = 1000;
    static final int SIP_MB = 5;
    private static final MapCodec<SimpleFluidContent> SAVED = SimpleFluidContent.CODEC.fieldOf("water");

    private Optional<CompoundTag> unreadable = Optional.empty();
    private final FluidTank water =
            new FluidTank(CAPACITY_MB, stack -> unreadable.isEmpty() && stack.is(Fluids.WATER)) {
                @Override
                protected void onContentsChanged() {
                    setChanged();
                }
            };

    CultivationTissueBlockEntity(BlockPos pos, BlockState state) {
        super(CultivationModule.CULTIVATION_TISSUE_ENTITY.get(), pos, state);
    }

    FluidTank water() {
        return water;
    }

    boolean isWet() {
        return water.getFluidAmount() > 0;
    }

    void sip() {
        water.drain(SIP_MB, IFluidHandler.FluidAction.EXECUTE);
    }

    private static Optional<String> check(SimpleFluidContent saved) {
        if (saved.getAmount() > CAPACITY_MB) return Optional.of(saved.getAmount() + " mB in a 1000 mB bed");
        if (!saved.isEmpty() && !saved.is(Fluids.WATER)) return Optional.of("a fluid that is not water");
        return Optional.empty();
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        Optional<SimpleFluidContent> read = Persistence.read(
                SAVED,
                tag,
                registries,
                CultivationModule.LOGGER,
                "Cultivation Tissue",
                CultivationTissueBlockEntity::check);
        unreadable = read.isPresent() ? Optional.empty() : Optional.of(tag.copy());
        read.ifPresent(saved -> water.setFluid(saved.copy()));
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (unreadable.isPresent()) tag.merge(unreadable.get());
        else Persistence.write(SAVED, SimpleFluidContent.copyOf(water.getFluid()), registries, tag);
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        if (unreadable.isPresent())
            components.set(OrganModule.UNREADABLE_SAVE.get(), unreadable.get().copy());
        else if (!water.isEmpty())
            components.set(LogisticsModule.FLUID_CONTENT.get(), SimpleFluidContent.copyOf(water.getFluid()));
    }

    @Override
    protected void applyImplicitComponents(DataComponentInput input) {
        super.applyImplicitComponents(input);
        CompoundTag kept = input.get(OrganModule.UNREADABLE_SAVE.get());
        if (kept != null) {
            unreadable = Optional.of(kept.copy());
            return;
        }
        SimpleFluidContent carried = input.get(LogisticsModule.FLUID_CONTENT.get());
        if (carried == null || check(carried).isPresent()) return;
        water.setFluid(carried.copy());
    }

    @Override
    public void removeComponentsFromTag(CompoundTag tag) {
        tag.remove("water");
    }
}
