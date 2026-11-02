package org.jd.infestusfrontier.processing;

import com.mojang.serialization.MapCodec;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
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
import org.jd.infestusfrontier.core.foundation.Refusal;
import org.jd.infestusfrontier.foundation.Persistence;
import org.jd.infestusfrontier.organ.OrganModule;
import org.jd.infestusfrontier.organ.api.Organs;
import org.jd.infestusfrontier.processing.api.PalmContents;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * A Shaping Palm (BLOCK_CATALOG T1-41, stage 1): the nine stacks of a crafting grid that stay in the organ when its
 * screen closes. It owns the grid and nothing else: a craft is the crafting table's own, taken at the screen by the
 * player ({@link ShapingPalmMenu}), so it holds no result, no recipe and no tray. It exposes no item handler, so no
 * hopper, pipe or Lobe reaches it; its supply link and its cycle are later stages and are not here.
 */
final class ShapingPalmBlockEntity extends BlockEntity implements MenuProvider, GeoBlockEntity {
    /** The organ has no foundation under it, so it keeps its grid and crafts nothing. */
    static final Refusal NO_FOUNDATION = new Refusal("processing.palm_no_foundation");

    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle");
    private static final int BLEND_TICKS = 10;
    private static final MapCodec<PalmContents> SAVED = PalmContents.CODEC.fieldOf("contents");

    private final AnimatableInstanceCache animatableCache = GeckoLibUtil.createInstanceCache(this);
    private final NonNullList<ItemStack> grid = NonNullList.withSize(PalmContents.CELLS, ItemStack.EMPTY);
    private Optional<CompoundTag> unreadable = Optional.empty();
    /** Counts every change to the grid, so a menu that is open knows when what it shows is out of date. */
    private int version;

    ShapingPalmBlockEntity(BlockPos pos, BlockState state) {
        super(ProcessingModule.SHAPING_PALM_ENTITY.get(), pos, state);
    }

    /** The live cells, shared by every menu open on this organ. Only {@link ShapingPalmMenu}'s grid view writes them. */
    NonNullList<ItemStack> grid() {
        return grid;
    }

    int version() {
        return version;
    }

    /** Marks the grid changed: saves it and tells every open menu. */
    void touch() {
        version++;
        setChanged();
    }

    /** False while the saved bytes could not be read; the organ then keeps them and takes nothing in. */
    boolean readable() {
        return unreadable.isEmpty();
    }

    /** Why the organ cannot craft now, or empty when it can. */
    Optional<Refusal> refusal() {
        if (unreadable.isPresent()) return Optional.of(Persistence.CORRUPT);
        if (level == null || !Organs.hasFoundation(level, worldPosition)) return Optional.of(NO_FOUNDATION);
        return Optional.empty();
    }

    /** True when the grid is empty and no unreadable bytes are kept. */
    boolean isEmpty() {
        return unreadable.isEmpty() && grid.stream().allMatch(ItemStack::isEmpty);
    }

    PalmContents contents() {
        return new PalmContents(grid);
    }

    /** Why {@code saved} cannot be installed as a whole, or empty when every stack is one the game could hold. */
    private static Optional<String> check(PalmContents saved) {
        for (ItemStack stack : saved.grid())
            if (stack.getCount() > stack.getMaxStackSize())
                return Optional.of(stack.getCount() + " of " + stack.getItem() + " in one cell");
        return Optional.empty();
    }

    private void restore(PalmContents saved) {
        for (int cell = 0; cell < PalmContents.CELLS; cell++) grid.set(cell, saved.cell(cell));
        version++;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "state", BLEND_TICKS, state -> state.setAndContinue(IDLE)));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return animatableCache;
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        Optional<PalmContents> read = Persistence.read(
                SAVED, tag, registries, ProcessingModule.LOGGER, "shaping_palm", ShapingPalmBlockEntity::check);
        unreadable = read.isPresent() ? Optional.empty() : Optional.of(tag.copy());
        read.ifPresent(this::restore);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (unreadable.isPresent()) tag.merge(unreadable.get());
        else Persistence.write(SAVED, contents(), registries, tag);
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        if (unreadable.isPresent()) {
            components.set(OrganModule.UNREADABLE_SAVE.get(), unreadable.get().copy());
            return;
        }
        if (!isEmpty()) components.set(ProcessingModule.PALM_CONTENTS.get(), contents());
    }

    @Override
    protected void applyImplicitComponents(DataComponentInput input) {
        super.applyImplicitComponents(input);
        CompoundTag kept = input.get(OrganModule.UNREADABLE_SAVE.get());
        if (kept != null) {
            unreadable = Optional.of(kept.copy());
            return;
        }
        PalmContents carried = input.get(ProcessingModule.PALM_CONTENTS.get());
        if (carried == null) return;
        Optional<String> problem = check(carried);
        if (problem.isPresent()) {
            ProcessingModule.LOGGER.warn("shaping_palm keeps unreadable carried state: {}", problem.get());
            unreadable = Optional.of(
                    level == null ? new CompoundTag() : Persistence.encode(SAVED, carried, level.registryAccess()));
            return;
        }
        restore(carried);
    }

    @Override
    public void removeComponentsFromTag(CompoundTag tag) {
        tag.remove("contents");
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.infestusfrontier.shaping_palm");
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new ShapingPalmMenu(id, inventory, this);
    }
}
