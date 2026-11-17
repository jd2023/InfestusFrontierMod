package org.jd.infestusfrontier.mining;

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
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jd.infestusfrontier.core.foundation.Biomass;
import org.jd.infestusfrontier.core.foundation.Outcome;
import org.jd.infestusfrontier.core.foundation.Refusal;
import org.jd.infestusfrontier.core.mining.Fracturing;
import org.jd.infestusfrontier.core.mining.JawProgress;
import org.jd.infestusfrontier.core.storage.BiomassTank;
import org.jd.infestusfrontier.foundation.Persistence;
import org.jd.infestusfrontier.organ.OrganModule;
import org.jd.infestusfrontier.organ.api.Organs;
import org.jd.infestusfrontier.organ.api.Steward;
import org.jd.infestusfrontier.organ.api.Stewarded;
import org.jd.infestusfrontier.storage.api.BiomassHandler;

/**
 * The work of a Fracture Jaw. Every {@value Fracturing#CUT_TICKS} loaded ticks it tries one cut: the block in front of
 * it, at a stone pickaxe's grade, rolled once in its ordinary loot into the Work Bed behind it, for
 * {@value Fracturing#CUT_BIOMASS_BU} BU. It never moves and never looks beyond the block in front. Everything a cut
 * reads and changes is judged first, so a cut is done whole in one tick or not at all; a stopped jaw keeps its cut
 * due, names the reason and looks again after {@value Fracturing#RETRY_TICKS} ticks, or at once when biomass arrives.
 * The jaws of a dimension share {@value Fracturing#CUTS_PER_TICK} cuts a tick. Biomass reaches it by bucket, pipe or
 * vein through its fluid capability. It loads no chunk.
 */
final class FractureJawBlockEntity extends BlockEntity implements BiomassHandler, Stewarded, MenuProvider {
    /** Blocks from the jaw whose chunks a cut needs: the bed, the cut, its neighbours and the blocks their updates reach. */
    private static final int REACH = 3;

    private BiomassTank tank = FractureJawContents.EMPTY.tank();
    private JawProgress progress = JawProgress.START;
    private Optional<CompoundTag> unreadable = Optional.empty();
    private final Steward steward = new Steward();
    /** Why the last try did not cut. Not saved: the next try tells again. */
    private Optional<Refusal> stop = Optional.empty();
    /** Loaded ticks before a stopped jaw looks again. */
    private int retry;

    FractureJawBlockEntity(BlockPos pos, BlockState state) {
        super(MiningModule.FRACTURE_JAW_ENTITY.get(), pos, state);
    }

    @Override
    public Steward steward() {
        return steward;
    }

    JawProgress progress() {
        return progress;
    }

    Optional<Refusal> stop() {
        return stop;
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

    /** Biomass arriving makes a stopped jaw look again on its next tick. */
    private Outcome<BiomassTank> apply(Outcome<BiomassTank> change, boolean simulate) {
        if (unreadable.isPresent()) return Outcome.refused(Persistence.CORRUPT);
        if (!simulate && change instanceof Outcome.Done<BiomassTank> done) {
            tank = done.value();
            retry = 0;
            setChanged();
        }
        return change;
    }

    // ---- the work ----

    static void serverTick(Level level, BlockPos pos, BlockState state, FractureJawBlockEntity jaw) {
        if (jaw.unreadable.isPresent() || !(level instanceof ServerLevel server)) return;
        if (!jaw.progress.due()) {
            jaw.progress = jaw.progress.waited();
            jaw.setChanged();
            return;
        }
        if (jaw.retry > 0) {
            jaw.retry--;
            return;
        }
        jaw.tryCut(server, pos, state.getValue(FacingOrganBlock.FACING));
    }

    private void tryCut(ServerLevel level, BlockPos pos, Direction facing) {
        BlockPos front = pos.relative(facing);
        BlockPos back = pos.relative(facing.getOpposite());
        Optional<Refusal> blocked = blocked(level, pos, front, back);
        if (blocked.isPresent()) {
            stopped(blocked.get());
            return;
        }
        // admitted before the roll, so a cut over the shared limit rolls nothing
        if (!level.getData(MiningModule.JAW_CUTS).take(level.getGameTime())) return;
        WorkBedBlockEntity bed = (WorkBedBlockEntity) level.getBlockEntity(back);
        Optional<WorkBedContents> after = bed.with(Cutting.loot(level, front));
        if (after.isEmpty()) {
            stopped(Fracturing.BED_FULL);
            return;
        }
        level.destroyBlock(front, false);
        bed.restore(after.get());
        tank = ((Outcome.Done<BiomassTank>) tank.drain(Biomass.of(Fracturing.CUT_BIOMASS_BU))).value();
        progress = progress.cut();
        stop = Optional.empty();
        setChanged();
        steward.worked(level, MiningModule.FRACTURE_JAW.getId());
    }

    private void stopped(Refusal why) {
        stop = Optional.of(why);
        retry = Fracturing.RETRY_TICKS;
    }

    /**
     * Why the jaw at {@code pos} cannot cut {@code front} into the bed at {@code back} now, or empty when it can. Reads
     * loaded positions only and changes nothing.
     */
    private Optional<Refusal> blocked(ServerLevel level, BlockPos pos, BlockPos front, BlockPos back) {
        if (!Organs.hasFoundation(level, pos)) return Optional.of(Fracturing.UNROOTED);
        if (!Cutting.loaded(level, pos, REACH)) return Optional.of(Fracturing.UNLOADED);
        if (!(level.getBlockEntity(back) instanceof WorkBedBlockEntity bed) || bed.unreadable())
            return Optional.of(Fracturing.NO_BED);
        if (!tank.stored().atLeast(Biomass.of(Fracturing.CUT_BIOMASS_BU))) return Optional.of(Fracturing.NO_BIOMASS);
        Optional<Refusal> verdict = switch (Cutting.judge(level, front)) {
            case CUTTABLE -> Optional.empty();
            case CLEAR -> Optional.of(Fracturing.NOTHING);
            case FLUID -> Optional.of(Fracturing.FLUID);
            case UNSUPPORTED -> Optional.of(Fracturing.UNSUPPORTED);
            case TOO_HARD -> Optional.of(Fracturing.TOO_HARD);
        };
        if (verdict.isPresent()) return verdict;
        if (Cutting.touchesFragile(level, front)) return Optional.of(Fracturing.FRAGILE);
        return Optional.empty();
    }

    // ---- saved state ----

    FractureJawContents contents() {
        return new FractureJawContents(tank, progress);
    }

    private void restore(FractureJawContents contents) {
        tank = contents.tank();
        progress = contents.progress();
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        Optional<FractureJawContents> read = Persistence.read(
                FractureJawContents.CODEC, tag, registries, MiningModule.LOGGER, "Fracture Jaw at " + worldPosition);
        unreadable = read.isPresent() ? Optional.empty() : Optional.of(tag.copy());
        restore(read.orElse(FractureJawContents.EMPTY));
        stop = Optional.empty();
        retry = 0;
        steward.load(tag);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (unreadable.isPresent()) tag.merge(unreadable.get());
        else Persistence.write(FractureJawContents.CODEC, contents(), registries, tag);
        steward.save(tag);
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        if (unreadable.isPresent())
            components.set(OrganModule.UNREADABLE_SAVE.get(), unreadable.get().copy());
        else components.set(MiningModule.FRACTURE_JAW_CONTENTS.get(), contents());
    }

    /** A carried jaw keeps its biomass and its cuts; carried bytes that fail their check stay unreadable. */
    @Override
    protected void applyImplicitComponents(DataComponentInput input) {
        super.applyImplicitComponents(input);
        CompoundTag kept = input.get(OrganModule.UNREADABLE_SAVE.get());
        if (kept != null) {
            unreadable = Optional.of(kept.copy());
            return;
        }
        FractureJawContents carried = input.get(MiningModule.FRACTURE_JAW_CONTENTS.get());
        if (carried == null) return;
        Optional<String> problem = carried.problem();
        if (problem.isPresent()) {
            MiningModule.LOGGER.warn("Fracture Jaw keeps unreadable carried state: {}", problem.get());
            unreadable = Optional.of(
                    level == null
                            ? new CompoundTag()
                            : Persistence.encode(FractureJawContents.CODEC, carried, level.registryAccess()));
            return;
        }
        restore(carried);
    }

    @Override
    public void removeComponentsFromTag(CompoundTag tag) {
        for (String key : List.of("tank", "cuts", "ticks")) tag.remove(key);
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
