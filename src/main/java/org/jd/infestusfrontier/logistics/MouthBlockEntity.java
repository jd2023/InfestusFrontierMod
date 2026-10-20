package org.jd.infestusfrontier.logistics;

import com.mojang.serialization.MapCodec;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Containers;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import org.jd.infestusfrontier.foundation.Persistence;
import org.jd.infestusfrontier.organ.OrganModule;
import org.jetbrains.annotations.Nullable;

/** One bounded inventory transfer. A refused insertion retains its extracted packet until a later pulse. */
final class MouthBlockEntity extends BlockEntity {
    static final int PULSE_TICKS = 8;
    private static final int RETRY_TICKS = 20;
    private static final int PACKET = 8;
    private static final int WINDOW = 16;
    private static final MapCodec<ItemStack> SAVED =
            ItemStack.OPTIONAL_CODEC.optionalFieldOf("pending", ItemStack.EMPTY);
    private ItemStack pending = ItemStack.EMPTY;
    private Optional<CompoundTag> unreadable = Optional.empty();
    private int sourceCursor;
    private int targetCursor;

    MouthBlockEntity(BlockPos pos, BlockState state) {
        super(LogisticsModule.MOUTH_ENTITY.get(), pos, state);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        schedule(PULSE_TICKS);
    }

    private void schedule(int ticks) {
        if (level != null && !level.isClientSide)
            level.scheduleTick(worldPosition, getBlockState().getBlock(), ticks);
    }

    void pulse() {
        if (level == null || level.isClientSide || unreadable.isPresent()) return;
        if (!level.getData(LogisticsModule.MOUTH_QUOTA).take(level.getGameTime())) {
            schedule(RETRY_TICKS + 1);
            return;
        }
        schedule(move() ? PULSE_TICKS : RETRY_TICKS);
    }

    private boolean move() {
        Direction facing = getBlockState().getValue(BlockStateProperties.FACING);
        boolean intake = ((MouthBlock) getBlockState().getBlock()).intake();
        Direction sourceSide = intake ? facing : facing.getOpposite();
        Direction targetSide = sourceSide.getOpposite();
        BlockPos sourcePos = worldPosition.relative(sourceSide);
        BlockPos targetPos = worldPosition.relative(targetSide);
        if (!level.hasChunkAt(sourcePos) || !level.hasChunkAt(targetPos)) return false;
        IItemHandler target = inventory(targetPos, targetSide.getOpposite());
        if (target == null || target.getSlots() == 0) return false;
        targetCursor %= target.getSlots();
        int end = targetCursor + Math.min(WINDOW, target.getSlots() - targetCursor);
        IItemHandler window = new Window(target, targetCursor, end - targetCursor);
        int nextTarget = end == target.getSlots() ? 0 : end;
        if (!pending.isEmpty()) {
            targetCursor = nextTarget;
            return deliver(window);
        }
        IItemHandler source = inventory(sourcePos, sourceSide.getOpposite());
        if (source == null || source.getSlots() == 0) return false;
        sourceCursor %= source.getSlots();
        int visits = Math.min(WINDOW, source.getSlots() - sourceCursor);
        for (int checked = 0; checked < visits; checked++) {
            int slot = sourceCursor;
            sourceCursor = sourceCursor == source.getSlots() - 1 ? 0 : sourceCursor + 1;
            if (sourceCursor == 0) targetCursor = nextTarget;
            ItemStack offered = source.extractItem(slot, PACKET, true);
            if (offered.isEmpty()) continue;
            int accepted = offered.getCount()
                    - ItemHandlerHelper.insertItem(window, offered.copy(), true).getCount();
            if (accepted == 0) continue;
            sourceCursor = slot;
            targetCursor = ((Window) window).start();
            pending = source.extractItem(slot, accepted, false);
            setChanged();
            return deliver(window);
        }
        return false;
    }

    private @Nullable IItemHandler inventory(BlockPos pos, Direction face) {
        BlockState state = level.getBlockState(pos);
        // Vanilla's double-chest capability resolves both halves, even across a chunk boundary.
        if (state.getBlock() instanceof ChestBlock
                && state.getValue(ChestBlock.TYPE) != ChestType.SINGLE
                && !level.hasChunkAt(pos.relative(ChestBlock.getConnectedDirection(state)))) return null;
        return level.getCapability(Capabilities.ItemHandler.BLOCK, pos, face);
    }

    /** A bounded insertion view; inventory implementation and slot rules stay with the provider. */
    private record Window(IItemHandler delegate, int start, int size) implements IItemHandler {
        @Override
        public int getSlots() {
            return size;
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return delegate.getStackInSlot(start + slot);
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            return delegate.insertItem(start + slot, stack, simulate);
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return delegate.extractItem(start + slot, amount, simulate);
        }

        @Override
        public int getSlotLimit(int slot) {
            return delegate.getSlotLimit(start + slot);
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return delegate.isItemValid(start + slot, stack);
        }
    }

    private boolean deliver(IItemHandler target) {
        int before = pending.getCount();
        pending = ItemHandlerHelper.insertItem(target, pending.copy(), false);
        setChanged();
        return pending.getCount() < before;
    }

    boolean isUnreadable() {
        return unreadable.isPresent();
    }

    void dropPending() {
        if (level != null && !level.isClientSide && unreadable.isEmpty()) {
            Containers.dropContents(level, worldPosition, new SimpleContainer(pending));
            pending = ItemStack.EMPTY;
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        Optional<ItemStack> read = Persistence.read(
                SAVED,
                tag,
                registries,
                LogisticsModule.LOGGER,
                "Mouth",
                stack -> stack.getCount() > PACKET ? Optional.of("more than eight pending items") : Optional.empty());
        pending = read.map(ItemStack::copy).orElse(ItemStack.EMPTY);
        unreadable = read.isPresent() ? Optional.empty() : Optional.of(tag.copy());
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (unreadable.isPresent()) tag.merge(unreadable.get());
        else Persistence.write(SAVED, pending, registries, tag);
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        unreadable.ifPresent(tag -> components.set(OrganModule.UNREADABLE_SAVE.get(), tag.copy()));
    }

    @Override
    protected void applyImplicitComponents(DataComponentInput input) {
        super.applyImplicitComponents(input);
        CompoundTag kept = input.get(OrganModule.UNREADABLE_SAVE.get());
        if (kept != null) unreadable = Optional.of(kept.copy());
    }

    @Override
    public void removeComponentsFromTag(CompoundTag tag) {
        tag.remove("pending");
    }
}
