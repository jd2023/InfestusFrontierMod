package org.jd.infestusfrontier.ecology;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * The lower half of a Sphincter Door or a door hardened from it, which draws the whole iris. It saves nothing: the
 * door's {@code open} block state picks the animation, so a door that was already open when its chunk loaded starts
 * open instead of dilating.
 */
final class SphincterDoorBlockEntity extends BlockEntity implements GeoBlockEntity {
    private static final RawAnimation OPEN = RawAnimation.begin().thenLoop("open");
    private static final RawAnimation CLOSED = RawAnimation.begin().thenLoop("closed");
    private static final RawAnimation OPENING =
            RawAnimation.begin().thenPlay("opening").thenLoop("open");
    private static final RawAnimation CLOSING =
            RawAnimation.begin().thenPlay("closing").thenLoop("closed");

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    /** What the client last showed; empty until the first frame. Client-side only, never saved. */
    private Boolean shownOpen;

    private RawAnimation showing = CLOSED;

    SphincterDoorBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "iris", 0, state -> state.setAndContinue(next())));
    }

    /** The resting loop on the first frame; afterwards a change of the open state plays the iris moving. */
    private RawAnimation next() {
        boolean open = getBlockState().getValue(DoorBlock.OPEN);
        if (shownOpen == null) showing = open ? OPEN : CLOSED;
        else if (shownOpen != open) showing = open ? OPENING : CLOSING;
        shownOpen = open;
        return showing;
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
