package org.jd.infestusfrontier.mining;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jd.infestusfrontier.core.mining.Counts;
import org.jd.infestusfrontier.core.mining.Fracturing;
import org.jd.infestusfrontier.foundation.Persistence;
import org.jd.infestusfrontier.organ.OrganModule;

/**
 * A Work Bed's store. Only the Fracture Jaw behind which it stands puts cut material in; Collection Cilia facing it and
 * the player's hand at its screen take it out. It has no item capability, so no hopper, pipe or vein reaches it. It
 * does no work and never ticks.
 */
final class WorkBedBlockEntity extends BlockEntity implements MenuProvider {
    private final ItemStackHandler slots = new ItemStackHandler(Fracturing.BED_SLOTS) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return false; // nothing goes in through the handler: a jaw's cut is added whole by restore
        }

        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };

    private int received;
    private Optional<CompoundTag> unreadable = Optional.empty();

    WorkBedBlockEntity(BlockPos pos, BlockState state) {
        super(MiningModule.WORK_BED_ENTITY.get(), pos, state);
    }

    boolean unreadable() {
        return unreadable.isPresent();
    }

    /** The slots the screen and the cilia take from. */
    ItemStackHandler slots() {
        return slots;
    }

    WorkBedContents contents() {
        List<ItemStack> held = new ArrayList<>(Fracturing.BED_SLOTS);
        for (int slot = 0; slot < Fracturing.BED_SLOTS; slot++) held.add(slots.getStackInSlot(slot));
        return new WorkBedContents(held, received);
    }

    /** The bed with {@code drops} added, or empty when they do not fit or the bed is unreadable. Changes nothing. */
    Optional<WorkBedContents> with(List<ItemStack> drops) {
        return unreadable.isPresent() ? Optional.empty() : contents().with(drops);
    }

    /** Takes {@code taken} items out of {@code slot} for the cilia; they are counted as gone. */
    void remove(int slot, int taken) {
        if (unreadable.isPresent() || taken <= 0) return;
        ItemStack held = slots.getStackInSlot(slot).copy();
        held.shrink(taken);
        slots.setStackInSlot(slot, held);
    }

    void restore(WorkBedContents contents) {
        for (int slot = 0; slot < Fracturing.BED_SLOTS; slot++)
            slots.setStackInSlot(slot, contents.slots().get(slot).copy());
        received = contents.received();
        setChanged();
    }

    /** Items the bed holds now. */
    int held() {
        int items = 0;
        for (int slot = 0; slot < Fracturing.BED_SLOTS; slot++)
            items = Counts.more(items, slots.getStackInSlot(slot).getCount());
        return items;
    }

    int received() {
        return received;
    }

    // ---- saved state ----

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        Optional<WorkBedContents> read = Persistence.read(
                WorkBedContents.CODEC, tag, registries, MiningModule.LOGGER, "Work Bed at " + worldPosition);
        unreadable = read.isPresent() ? Optional.empty() : Optional.of(tag.copy());
        restore(read.orElse(WorkBedContents.EMPTY));
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (unreadable.isPresent()) tag.merge(unreadable.get());
        else Persistence.write(WorkBedContents.CODEC, contents(), registries, tag);
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        if (unreadable.isPresent())
            components.set(OrganModule.UNREADABLE_SAVE.get(), unreadable.get().copy());
        else components.set(MiningModule.WORK_BED_CONTENTS.get(), contents());
    }

    /** A carried bed keeps what it held and what it has received; carried bytes that fail their check stay unreadable. */
    @Override
    protected void applyImplicitComponents(DataComponentInput input) {
        super.applyImplicitComponents(input);
        CompoundTag kept = input.get(OrganModule.UNREADABLE_SAVE.get());
        if (kept != null) {
            unreadable = Optional.of(kept.copy());
            return;
        }
        WorkBedContents carried = input.get(MiningModule.WORK_BED_CONTENTS.get());
        if (carried == null) return;
        Optional<String> problem = carried.problem();
        if (problem.isPresent()) {
            MiningModule.LOGGER.warn("Work Bed keeps unreadable carried state: {}", problem.get());
            unreadable = Optional.of(
                    level == null
                            ? new CompoundTag()
                            : Persistence.encode(WorkBedContents.CODEC, carried, level.registryAccess()));
            return;
        }
        restore(carried);
    }

    @Override
    public void removeComponentsFromTag(CompoundTag tag) {
        for (String key : List.of("slots", "received")) tag.remove(key);
    }

    @Override
    public Component getDisplayName() {
        return getBlockState().getBlock().getName();
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new WorkBedMenu(id, inventory, this);
    }
}
