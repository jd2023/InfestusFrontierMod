package org.jd.infestusfrontier.defense;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** One non-recoverable pellet. Vanilla owns motion and impacts; only eligible flying hostiles take damage. */
final class SporePellet extends ThrowableItemProjectile {
    private static final TagKey<EntityType<?>> TARGETS = TagKey.create(
            Registries.ENTITY_TYPE, ResourceLocation.fromNamespaceAndPath("infestusfrontier", "aerial_hostiles"));
    private BlockPos origin = BlockPos.ZERO;
    private int life;

    SporePellet(EntityType<? extends ThrowableItemProjectile> type, Level level) {
        super(type, level);
        setNoGravity(true);
    }

    static boolean target(Entity entity) {
        return entity instanceof LivingEntity living
                && entity instanceof Enemy
                && !(entity instanceof Player)
                && !(entity instanceof OwnableEntity owned && owned.getOwnerUUID() != null)
                && entity.getType().is(TARGETS)
                && living.isAlive()
                && !living.isInvulnerable();
    }

    void launch(BlockPos source, Vec3 from, Vec3 target) {
        origin = source.immutable();
        setPos(from);
        Vec3 aim = target.subtract(from);
        shoot(aim.x, aim.y, aim.z, 1.5F, 0);
    }

    @Override
    protected Item getDefaultItem() {
        return Items.SLIME_BALL;
    }

    @Override
    public void tick() {
        if (!level().isClientSide
                && (++life > 40
                        || !DefenseSight.loaded(level(), position(), position().add(getDeltaMovement())))) {
            discard();
            return;
        }
        super.tick();
    }

    @Override
    protected void onHitEntity(EntityHitResult hit) {
        super.onHitEntity(hit);
        if (!level().isClientSide && target(hit.getEntity())) {
            DamageSource source = new DamageSource(
                    level().damageSources().source(DefenseModule.SPORE).typeHolder(), Vec3.atCenterOf(origin));
            hit.getEntity().hurt(source, 4);
        }
    }

    @Override
    protected void onHit(HitResult hit) {
        super.onHit(hit);
        if (!level().isClientSide) discard();
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putLong("organ", origin.asLong());
        tag.putInt("life", life);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        origin = BlockPos.of(tag.getLong("organ"));
        life = Math.max(0, Math.min(40, tag.getInt("life")));
        setNoGravity(true);
    }
}
