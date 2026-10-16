package org.jd.infestusfrontier.integration.geckolib;

import java.util.Arrays;
import java.util.List;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * An organ as an item: an ordinary block item that GeckoLib draws with the block's own model, always at rest. No
 * animation id is ever assigned to a stack, so the item carries only the components its organ gives it.
 */
public class OrganItem extends BlockItem implements GeoItem {
    private final AnimatableInstanceCache animatableCache = GeckoLibUtil.createInstanceCache(this);
    private final List<RawAnimation> rest;

    /** Each rest animation loops in its own controller, so they may move different bones. */
    public OrganItem(Block block, Properties properties, String... restAnimations) {
        super(block, properties);
        this.rest = Arrays.stream(restAnimations)
                .map(name -> RawAnimation.begin().thenLoop(name))
                .toList();
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        for (int i = 0; i < rest.size(); i++) {
            RawAnimation animation = rest.get(i);
            controllers.add(new AnimationController<>(this, "rest_" + i, 0, state -> state.setAndContinue(animation)));
        }
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return animatableCache;
    }
}
