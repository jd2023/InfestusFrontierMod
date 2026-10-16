package org.jd.infestusfrontier.storage;

import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import org.jd.infestusfrontier.storage.api.BiomassBuckets;

/** Exactly one bucket of biomass. Used on a biomass block it empties into it; it is never placed in the world. */
final class BiomassBucketItem extends Item {
    BiomassBucketItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (context.getPlayer() == null) return InteractionResult.PASS;
        return BiomassBuckets.useOn(
                        context.getItemInHand(),
                        context.getLevel(),
                        context.getClickedPos(),
                        context.getClickedFace(),
                        context.getPlayer(),
                        context.getHand())
                .result();
    }
}
