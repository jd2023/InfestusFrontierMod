package org.jd.infestusfrontier.mining;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jd.infestusfrontier.core.mining.Leaching;
import org.jd.infestusfrontier.core.mining.LeachingProgress;
import org.jd.infestusfrontier.foundation.Persistence;
import org.jd.infestusfrontier.organ.api.Steward;
import org.jd.infestusfrontier.organ.api.Stewarded;

/**
 * The work of a Leaching Pod. Every {@value Leaching#LAYER_TICKS} loaded ticks it converts one whole layer: the
 * eligible blocks of a 7-wide disc, one layer further below the pod each time. The block in the pod's own column becomes
 * its glowing root while the root above it is unbroken, and Leached Rock once the root has ended; the root never grows
 * into anything the pod would not convert. A layer is worked whole in one tick or not at all, so the saved
 * {@link LeachingProgress} always matches the ground: a reload neither repeats a layer nor pays for it twice. While the
 * chunks the layer touches or updates are not all loaded the pod waits and tries again after a second; it loads
 * nothing. Finishing its layers or charges stops it for good, the pod and its root left as they are.
 */
final class LeachingPodBlockEntity extends BlockEntity implements Stewarded {
    /** Blocks beyond the disc whose chunks must also be loaded: a conversion updates its neighbours. */
    private static final int UPDATE_REACH = 1;

    private static final MapCodec<LeachingProgress> SAVED = RecordCodecBuilder.mapCodec(instance -> instance.group(
                    Codec.INT.fieldOf("layers").forGetter(LeachingProgress::layers),
                    Codec.INT.fieldOf("charges").forGetter(LeachingProgress::charges),
                    Codec.INT.fieldOf("ticks").forGetter(LeachingProgress::ticks))
            .apply(instance, LeachingProgress::new));

    private LeachingProgress progress = LeachingProgress.START;
    private boolean waitingForGround;
    private Optional<CompoundTag> unreadable = Optional.empty();
    private final Steward steward = new Steward();

    LeachingPodBlockEntity(BlockPos pos, BlockState state) {
        super(MiningModule.LEACHING_POD_ENTITY.get(), pos, state);
    }

    @Override
    public Steward steward() {
        return steward;
    }

    /** True when saved data could not be read; the pod keeps it unchanged and does nothing. */
    boolean unreadable() {
        return unreadable.isPresent();
    }

    boolean spent() {
        return getBlockState().getValue(LeachingPodBlock.SPENT);
    }

    /** True while the last layer waited for ground that is not loaded. Not saved: the next try tells again. */
    boolean waitingForGround() {
        return waitingForGround;
    }

    int layers() {
        return progress.layers();
    }

    int charges() {
        return progress.charges();
    }

    static void serverTick(Level level, BlockPos pos, BlockState state, LeachingPodBlockEntity pod) {
        if (pod.unreadable.isPresent() || !(level instanceof ServerLevel server)) return;
        if (pod.progress.finished()) {
            pod.spend(server, pos, state);
            return;
        }
        if (!pod.progress.due()) {
            pod.progress = pod.progress.waited();
            pod.setChanged();
            return;
        }
        pod.workLayer(server, pos, state);
    }

    private void workLayer(ServerLevel level, BlockPos pos, BlockState state) {
        if (!groundLoaded(level, pos)) {
            waitingForGround = true;
            progress = progress.blocked();
            setChanged();
            return;
        }
        waitingForGround = false;
        int y = pos.getY() - 1 - progress.layers();
        BlockPos column = new BlockPos(pos.getX(), y, pos.getZ());
        BlockState above = level.getBlockState(column.above());
        boolean rooting = above.is(MiningModule.LEACHING_POD.get()) || above.is(MiningModule.LEACHING_ROOT.get());
        int converted = 0;
        for (Leaching.Cell cell : Leaching.DISC) {
            BlockPos target = new BlockPos(pos.getX() + cell.dx(), y, pos.getZ() + cell.dz());
            BlockState into = rooting && target.equals(column)
                    ? MiningModule.LEACHING_ROOT.get().defaultBlockState()
                    : MiningModule.LEACHED_ROCK.get().defaultBlockState();
            if (leachable(level, target) && level.setBlock(target, into, Block.UPDATE_ALL)) converted++;
        }
        progress = progress.worked(converted);
        setChanged();
        if (converted > 0) {
            level.playSound(null, pos, SoundEvents.SLIME_BLOCK_STEP, SoundSource.BLOCKS, 0.6F, 0.7F);
            steward.worked(level, MiningModule.LEACHING_POD.getId());
        }
        if (progress.finished()) spend(level, pos, state);
    }

    /** The finished pod stays as it is, its pore closed; it no longer ticks. */
    private static void spend(ServerLevel level, BlockPos pos, BlockState state) {
        level.setBlock(pos, state.setValue(LeachingPodBlock.SPENT, true), Block.UPDATE_ALL);
    }

    /** True when every chunk the next layer reads, replaces or updates is loaded. The pod forces none. */
    private boolean groundLoaded(ServerLevel level, BlockPos pos) {
        int reach = Leaching.RADIUS + UPDATE_REACH;
        ChunkPos low = new ChunkPos(pos.offset(-reach, 0, -reach));
        ChunkPos high = new ChunkPos(pos.offset(reach, 0, reach));
        for (int x = low.x; x <= high.x; x++)
            for (int z = low.z; z <= high.z; z++) if (!level.hasChunk(x, z)) return false;
        return true;
    }

    /** A block the pod may convert: tagged, soft enough, no fluid, no block entity. Read and replaced in one step. */
    private static boolean leachable(ServerLevel level, BlockPos target) {
        if (level.isOutsideBuildHeight(target)) return false;
        BlockState state = level.getBlockState(target);
        return state.is(MiningModule.LEACHABLE)
                && Leaching.softEnough(state.getDestroySpeed(level, target))
                && state.getFluidState().isEmpty()
                && !state.hasBlockEntity();
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        Optional<LeachingProgress> read =
                Persistence.read(SAVED, tag, registries, MiningModule.LOGGER, "Leaching Pod at " + worldPosition);
        progress = read.orElse(LeachingProgress.START);
        unreadable = read.isPresent() ? Optional.empty() : Optional.of(tag.copy());
        waitingForGround = false;
        steward.load(tag);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (unreadable.isPresent()) tag.merge(unreadable.get());
        else Persistence.write(SAVED, progress, registries, tag);
        steward.save(tag);
    }
}
