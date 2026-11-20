package org.jd.infestusfrontier.mining;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jd.infestusfrontier.core.foundation.Biomass;
import org.jd.infestusfrontier.core.foundation.Outcome;
import org.jd.infestusfrontier.core.foundation.Refusal;
import org.jd.infestusfrontier.core.mining.Collecting;
import org.jd.infestusfrontier.core.mining.Counts;
import org.jd.infestusfrontier.core.storage.BiomassTank;
import org.jd.infestusfrontier.foundation.Persistence;
import org.jd.infestusfrontier.logistics.api.ItemAccess;
import org.jd.infestusfrontier.logistics.api.LogisticsCapabilities;
import org.jd.infestusfrontier.organ.OrganModule;
import org.jd.infestusfrontier.organ.api.Organs;
import org.jd.infestusfrontier.organ.api.Steward;
import org.jd.infestusfrontier.organ.api.Stewarded;
import org.jd.infestusfrontier.storage.api.BiomassHandler;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * The work of Collection Cilia. Every {@value Collecting#SWEEP_TICKS} loaded ticks they offer the first
 * {@value Collecting#SWEEP_ITEMS} items of the Work Bed in front of them to the Item Vein network through the Mouth
 * they stand on ({@link ItemAccess}): logistics decides which far Mouths take what, and really moves it before the
 * call returns. Exactly what was taken leaves the bed; the rest stays there. A sweep that moved something costs
 * {@value Collecting#SWEEP_BIOMASS_BU} BU, which it must have before it starts. The cilia hold no items, so nothing
 * they handle can spill, vanish or double. Biomass reaches them by bucket, pipe or vein. They load no chunk. While the
 * last sweep moved something their block state says so, and the client shows the fronds drawing fragments into the
 * intake and a pulse running down into the Mouth; nothing else is sent for it.
 */
final class CollectionCiliaBlockEntity extends BlockEntity
        implements BiomassHandler, Stewarded, MenuProvider, GeoBlockEntity {
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation DRAWING = RawAnimation.begin().thenLoop("drawing");
    /** Ticks the model blends from one clip into the next. */
    private static final int BLEND_TICKS = 4;

    private final AnimatableInstanceCache animatableCache = GeckoLibUtil.createInstanceCache(this);
    private BiomassTank tank = CollectionCiliaContents.EMPTY.tank();
    private int sent;
    private Optional<CompoundTag> unreadable = Optional.empty();
    private final Steward steward = new Steward();
    /** Why the last sweep moved nothing. Not saved: the next sweep tells again. */
    private Optional<Refusal> stop = Optional.empty();
    /** Loaded ticks before the next sweep. Not saved: a loaded organ waits a whole sweep, so there is no burst. */
    private int wait = Collecting.SWEEP_TICKS;

    CollectionCiliaBlockEntity(BlockPos pos, BlockState state) {
        super(MiningModule.COLLECTION_CILIA_ENTITY.get(), pos, state);
    }

    @Override
    public Steward steward() {
        return steward;
    }

    boolean unreadable() {
        return unreadable.isPresent();
    }

    int sent() {
        return sent;
    }

    Optional<Refusal> stop() {
        return stop;
    }

    /** Loaded ticks before the next sweep. */
    int waiting() {
        return wait;
    }

    // ---- biomass ----

    @Override
    public BiomassTank tank() {
        return tank;
    }

    @Override
    public Outcome<BiomassTank> insert(Biomass amount, boolean simulate) {
        return apply(tank.fill(amount), simulate);
    }

    @Override
    public Outcome<BiomassTank> extract(Biomass amount, boolean simulate) {
        return apply(tank.drain(amount), simulate);
    }

    private Outcome<BiomassTank> apply(Outcome<BiomassTank> change, boolean simulate) {
        if (unreadable.isPresent()) return Outcome.refused(Persistence.CORRUPT);
        if (!simulate && change instanceof Outcome.Done<BiomassTank> done) {
            tank = done.value();
            setChanged();
        }
        return change;
    }

    // ---- the work ----

    static void serverTick(Level level, BlockPos pos, BlockState state, CollectionCiliaBlockEntity cilia) {
        if (cilia.unreadable.isPresent() || !(level instanceof ServerLevel server)) return;
        if (--cilia.wait > 0) return;
        cilia.wait = Collecting.SWEEP_TICKS;
        int before = cilia.sent;
        cilia.stop = cilia.sweep(server, pos, state.getValue(FacingOrganBlock.FACING));
        boolean drew = cilia.sent != before;
        if (state.getValue(CollectionCiliaBlock.WORKING) != drew)
            level.setBlock(pos, state.setValue(CollectionCiliaBlock.WORKING, drew), Block.UPDATE_CLIENTS);
    }

    /** One sweep from the bed in front into the network. Returns why it moved nothing, or empty. */
    private Optional<Refusal> sweep(ServerLevel level, BlockPos pos, Direction facing) {
        if (!Organs.hasFoundation(level, pos)) return Optional.of(Collecting.UNROOTED);
        ItemAccess door = level.getCapability(LogisticsCapabilities.ITEM_ACCESS, pos.below(), null);
        if (door == null) return Optional.of(Collecting.NO_MOUTH);
        BlockPos front = pos.relative(facing);
        if (!level.hasChunkAt(front)
                || !(level.getBlockEntity(front) instanceof WorkBedBlockEntity bed)
                || bed.unreadable()) return Optional.of(Collecting.NO_BED);
        if (!door.gives()) return Optional.of(ItemAccess.NOT_GIVING);
        int[] held = new int[bed.slots().getSlots()];
        for (int slot = 0; slot < held.length; slot++)
            held[slot] = bed.slots().getStackInSlot(slot).getCount();
        int[] offered = Collecting.offered(held);
        List<Integer> slots = new ArrayList<>();
        List<ItemAccess.Offer> offers = new ArrayList<>();
        for (int slot = 0; slot < held.length; slot++) {
            if (offered[slot] == 0) continue;
            ItemStack stack = bed.slots().getStackInSlot(slot);
            slots.add(slot);
            offers.add(new ItemAccess.Offer(stack.copyWithCount(offered[slot]), kindHeld(bed, stack)));
        }
        if (offers.isEmpty()) return Optional.empty(); // nothing to collect: waiting is not a stop
        if (!tank.stored().atLeast(Biomass.of(Collecting.SWEEP_BIOMASS_BU))) return Optional.of(Collecting.NO_BIOMASS);
        ItemAccess.Sent result = door.push(offers, false);
        int moved = 0;
        for (int n = 0; n < offers.size(); n++) {
            int offer = offers.get(n).stack().getCount();
            int taken = Math.max(0, offer - result.rests().get(n).getCount());
            bed.remove(slots.get(n), taken);
            moved = Counts.more(moved, taken);
        }
        if (moved == 0) return Optional.of(result.why().orElse(Collecting.STORAGE_FULL));
        tank = ((Outcome.Done<BiomassTank>) tank.drain(Biomass.of(Collecting.SWEEP_BIOMASS_BU))).value();
        sent = Counts.more(sent, moved);
        setChanged();
        steward.worked(level, MiningModule.COLLECTION_CILIA.getId());
        return Optional.empty();
    }

    /** All the bed holds of {@code like}'s kind, for the Mouth's amount. */
    private static int kindHeld(WorkBedBlockEntity bed, ItemStack like) {
        int held = 0;
        for (int slot = 0; slot < bed.slots().getSlots(); slot++)
            if (ItemStack.isSameItemSameComponents(bed.slots().getStackInSlot(slot), like))
                held = Counts.more(held, bed.slots().getStackInSlot(slot).getCount());
        return held;
    }

    // ---- appearance ----

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(
                this,
                "sweep",
                BLEND_TICKS,
                state -> state.setAndContinue(
                        state.getAnimatable().getBlockState().getValue(CollectionCiliaBlock.WORKING)
                                ? DRAWING
                                : IDLE)));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return animatableCache;
    }

    // ---- saved state ----

    CollectionCiliaContents contents() {
        return new CollectionCiliaContents(tank, sent);
    }

    private void restore(CollectionCiliaContents contents) {
        tank = contents.tank();
        sent = contents.sent();
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        Optional<CollectionCiliaContents> read = Persistence.read(
                CollectionCiliaContents.CODEC,
                tag,
                registries,
                MiningModule.LOGGER,
                "Collection Cilia at " + worldPosition);
        unreadable = read.isPresent() ? Optional.empty() : Optional.of(tag.copy());
        restore(read.orElse(CollectionCiliaContents.EMPTY));
        stop = Optional.empty();
        wait = Collecting.SWEEP_TICKS;
        steward.load(tag);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (unreadable.isPresent()) tag.merge(unreadable.get());
        else Persistence.write(CollectionCiliaContents.CODEC, contents(), registries, tag);
        steward.save(tag);
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        if (unreadable.isPresent())
            components.set(OrganModule.UNREADABLE_SAVE.get(), unreadable.get().copy());
        else components.set(MiningModule.COLLECTION_CILIA_CONTENTS.get(), contents());
    }

    /** Carried cilia keep their biomass and what they have sent; carried bytes that fail their check stay unreadable. */
    @Override
    protected void applyImplicitComponents(DataComponentInput input) {
        super.applyImplicitComponents(input);
        CompoundTag kept = input.get(OrganModule.UNREADABLE_SAVE.get());
        if (kept != null) {
            unreadable = Optional.of(kept.copy());
            return;
        }
        CollectionCiliaContents carried = input.get(MiningModule.COLLECTION_CILIA_CONTENTS.get());
        if (carried == null) return;
        Optional<String> problem = carried.problem();
        if (problem.isPresent()) {
            MiningModule.LOGGER.warn("Collection Cilia keep unreadable carried state: {}", problem.get());
            unreadable = Optional.of(
                    level == null
                            ? new CompoundTag()
                            : Persistence.encode(CollectionCiliaContents.CODEC, carried, level.registryAccess()));
            return;
        }
        restore(carried);
    }

    @Override
    public void removeComponentsFromTag(CompoundTag tag) {
        for (String key : List.of("tank", "sent")) tag.remove(key);
    }

    @Override
    public Component getDisplayName() {
        return getBlockState().getBlock().getName();
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new WorkOrganMenu(id, inventory, this);
    }
}
