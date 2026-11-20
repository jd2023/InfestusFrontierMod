package org.jd.infestusfrontier.vigil;

import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** A tethered living keeper, not a mobile loader. The nest alone owns all tickets. */
public final class VigilKeeper extends PathfinderMob {
    private Optional<BlockPos> nest = Optional.empty();
    private static final int ORPHAN_GRACE_TICKS = 100;

    public VigilKeeper(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        setPersistenceRequired();
        setNoGravity(true);
        setNoAi(true);
    }

    public static AttributeSupplier.Builder attributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 30)
                .add(Attributes.MOVEMENT_SPEED, 0);
    }

    void bind(BlockPos pos) {
        nest = Optional.of(pos.immutable());
    }

    boolean belongsTo(BlockPos pos) {
        return nest.filter(pos::equals).isPresent();
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) return;
        if (nest.isEmpty()) {
            discard();
            return;
        }
        BlockPos home = nest.get();
        if (!level().hasChunkAt(home)) {
            if (tickCount > ORPHAN_GRACE_TICKS) discard();
            return;
        }
        if (!(level().getBlockEntity(home) instanceof VigilNestBlockEntity owner) || !owner.owns(getUUID())) {
            if (tickCount > ORPHAN_GRACE_TICKS) discard();
            return;
        }
        Vec3 anchor = Vec3.atBottomCenterOf(home).add(0, 2.5, 0);
        if (position().distanceToSqr(anchor) > 16) setPos(anchor);
        setDeltaMovement(anchor.subtract(position()).scale(0.06));
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        nest.ifPresent(pos -> tag.putLong("VigilNest", pos.asLong()));
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        nest = tag.contains("VigilNest", net.minecraft.nbt.Tag.TAG_LONG)
                ? Optional.of(BlockPos.of(tag.getLong("VigilNest")))
                : Optional.empty();
    }
}
