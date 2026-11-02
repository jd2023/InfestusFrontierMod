package org.jd.infestusfrontier.processing;

import com.mojang.serialization.MapCodec;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
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
import org.jd.infestusfrontier.processing.api.PalmLink;
import org.jd.infestusfrontier.processing.api.PalmLinking;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * A Shaping Palm (BLOCK_CATALOG T1-41, stages 1 and 2): the nine stacks of a crafting grid that stay in the organ when
 * its screen closes, whether it has been given Reach, and its one supply link, a position. A craft is the crafting
 * table's own, taken at the screen by the player ({@link ShapingPalmMenu}), so it holds no result, no recipe and no
 * tray. It exposes no item handler, so no hopper, pipe or Lobe reaches it; the store is found at the linked position
 * only when a screen action asks ({@link #store()}), and its cycle is a later stage and is not here.
 */
final class ShapingPalmBlockEntity extends BlockEntity implements MenuProvider, GeoBlockEntity, PalmLink {
    /** The organ has no foundation under it, so it keeps its grid and crafts nothing. */
    static final Refusal NO_FOUNDATION = new Refusal("processing.palm_no_foundation");
    /** The organ already has Reach, so a second Contractile Fiber is not spent. */
    static final Refusal ALREADY_REACHES = new Refusal("processing.palm_has_reach");

    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle");
    private static final int BLEND_TICKS = 10;
    private static final MapCodec<PalmContents> SAVED = PalmContents.CODEC.fieldOf("contents");
    private static final String REACH = "reach";
    private static final String LINK = "link";

    private final AnimatableInstanceCache animatableCache = GeckoLibUtil.createInstanceCache(this);
    private final NonNullList<ItemStack> grid = NonNullList.withSize(PalmContents.CELLS, ItemStack.EMPTY);
    private Optional<CompoundTag> unreadable = Optional.empty();
    /** Counts every change to the grid, so a menu that is open knows when what it shows is out of date. */
    private int version;
    /** A Contractile Fiber was applied: the Palm may hold a supply link. Carried on its item. */
    private boolean reach;
    /** The one linked store position, in this organ's dimension and within range; never carried on the item. */
    private Optional<BlockPos> link = Optional.empty();

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

    @Override
    public boolean hasReach() {
        return reach;
    }

    /** The linked store position, if the Palm has one; it says nothing of whether anything is there now. */
    Optional<BlockPos> link() {
        return reach ? link : Optional.empty();
    }

    /** Applies Reach once. Refuses a Palm whose save could not be read, and a Palm that already has it. */
    Optional<Refusal> grantReach() {
        if (unreadable.isPresent()) return Optional.of(Persistence.CORRUPT);
        if (reach) return Optional.of(ALREADY_REACHES);
        reach = true;
        setChanged();
        return Optional.empty();
    }

    @Override
    public Optional<Refusal> link(BlockPos target) {
        Optional<Refusal> why = reachRefusal();
        if (why.isPresent()) return why;
        if (level == null) return Optional.of(PalmSupply.NO_STORE);
        PalmSupply.Reached reached = PalmSupply.reach(level, worldPosition, target);
        if (reached instanceof PalmSupply.Reached.Refused refused) return Optional.of(refused.why());
        link = Optional.of(target.immutable());
        setChanged();
        return Optional.empty();
    }

    /** Why the Palm cannot use a supply link at all now, or empty. */
    private Optional<Refusal> reachRefusal() {
        if (unreadable.isPresent()) return Optional.of(Persistence.CORRUPT);
        return reach ? Optional.empty() : Optional.of(PalmSupply.NO_REACH);
    }

    /**
     * The store the link reaches now, or the reason it does not: no Reach, no link, a position out of range, an
     * unloaded chunk, or a block without an item handler. Looks at the linked position only, loads nothing, and keeps
     * the link whatever it finds, so a store that comes back is used again.
     */
    PalmSupply.Reached store() {
        Optional<Refusal> why = reachRefusal();
        if (why.isPresent()) return new PalmSupply.Reached.Refused(why.get());
        if (link.isEmpty()) return new PalmSupply.Reached.Refused(PalmSupply.NOT_LINKED);
        if (level == null) return new PalmSupply.Reached.Refused(PalmSupply.UNLOADED);
        return PalmSupply.reach(level, worldPosition, link.get());
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
        reach = tag.getBoolean(REACH);
        link = NbtUtils.readBlockPos(tag, LINK)
                .filter(pos -> pos.distSqr(worldPosition) <= PalmLinking.RANGE * PalmLinking.RANGE);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (unreadable.isPresent()) tag.merge(unreadable.get());
        else Persistence.write(SAVED, contents(), registries, tag);
        tag.remove(REACH);
        tag.remove(LINK);
        if (reach) tag.putBoolean(REACH, true);
        link.ifPresent(pos -> tag.put(LINK, NbtUtils.writeBlockPos(pos)));
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        if (unreadable.isPresent()) {
            CompoundTag kept = unreadable.get().copy();
            kept.remove(LINK);
            components.set(OrganModule.UNREADABLE_SAVE.get(), kept);
            return;
        }
        if (!isEmpty()) components.set(ProcessingModule.PALM_CONTENTS.get(), contents());
        if (reach) components.set(ProcessingModule.PALM_REACH.get(), true);
    }

    @Override
    protected void applyImplicitComponents(DataComponentInput input) {
        super.applyImplicitComponents(input);
        CompoundTag kept = input.get(OrganModule.UNREADABLE_SAVE.get());
        if (kept != null) {
            unreadable = Optional.of(kept.copy());
            reach = kept.getBoolean(REACH);
            return;
        }
        if (Boolean.TRUE.equals(input.get(ProcessingModule.PALM_REACH.get()))) reach = true;
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
        tag.remove(REACH);
        tag.remove(LINK);
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
